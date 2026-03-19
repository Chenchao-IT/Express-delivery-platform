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
            UserWallet w = new UserWallet();
            w.setUserId(userId);
            w.setBalance(BigDecimal.ZERO);
            w.setFrozen(BigDecimal.ZERO);
            return walletRepository.save(w);
        });
    }

    @Transactional
    public void freeze(Long userId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new RuntimeException("悬赏金额必须大于0");
        UserWallet w = ensureWallet(userId);
        if (w.getBalance().compareTo(amount) < 0) throw new RuntimeException("余额不足");
        w.setBalance(w.getBalance().subtract(amount));
        w.setFrozen(w.getFrozen().add(amount));
        walletRepository.save(w);
    }

    @Transactional
    public void unfreezeToBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return;
        UserWallet w = ensureWallet(userId);
        if (w.getFrozen().compareTo(amount) < 0) throw new RuntimeException("冻结余额不足");
        w.setFrozen(w.getFrozen().subtract(amount));
        w.setBalance(w.getBalance().add(amount));
        walletRepository.save(w);
    }

    @Transactional
    public void transferFromFrozenToBalance(Long fromUserId, Long toUserId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return;
        UserWallet from = ensureWallet(fromUserId);
        UserWallet to = ensureWallet(toUserId);
        if (from.getFrozen().compareTo(amount) < 0) throw new RuntimeException("冻结余额不足，无法结算");
        from.setFrozen(from.getFrozen().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        walletRepository.save(from);
        walletRepository.save(to);
    }
}

