package com.campus.express.service;

import com.campus.express.entity.UserWallet;
import com.campus.express.repository.UserWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserWalletRepository walletRepository;

    @Transactional
    public UserWallet ensureWallet(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            UserWallet wallet = new UserWallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setFrozen(BigDecimal.ZERO);
            return walletRepository.save(wallet);
        });
    }

    @Transactional
    public UserWallet creditBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return ensureWallet(userId);
        }
        UserWallet wallet = ensureWallet(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        return walletRepository.save(wallet);
    }

    @Transactional
    public UserWallet ensureMinimumBalance(Long userId, BigDecimal minimumBalance) {
        if (minimumBalance == null || minimumBalance.signum() <= 0) {
            return ensureWallet(userId);
        }
        UserWallet wallet = ensureWallet(userId);
        if (wallet.getBalance().compareTo(minimumBalance) >= 0) {
            return wallet;
        }
        return creditBalance(userId, minimumBalance.subtract(wallet.getBalance()));
    }

    @Transactional
    public void freeze(Long userId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new RuntimeException("悬赏金额必须大于 0");
        }
        UserWallet wallet = ensureWallet(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setFrozen(wallet.getFrozen().add(amount));
        walletRepository.save(wallet);
    }

    @Transactional
    public void unfreezeToBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        UserWallet wallet = ensureWallet(userId);
        if (wallet.getFrozen().compareTo(amount) < 0) {
            throw new RuntimeException("冻结余额不足");
        }
        wallet.setFrozen(wallet.getFrozen().subtract(amount));
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }

    @Transactional
    public void transferFromFrozenToBalance(Long fromUserId, Long toUserId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        UserWallet fromWallet = ensureWallet(fromUserId);
        UserWallet toWallet = ensureWallet(toUserId);
        if (fromWallet.getFrozen().compareTo(amount) < 0) {
            throw new RuntimeException("冻结余额不足，无法结算");
        }
        fromWallet.setFrozen(fromWallet.getFrozen().subtract(amount));
        toWallet.setBalance(toWallet.getBalance().add(amount));
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
    }
}
