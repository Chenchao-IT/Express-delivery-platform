package com.campus.express.service;

import com.campus.express.dto.AppealRequest;
import com.campus.express.dto.AppealResult;
import com.campus.express.dto.ReviewResult;
import com.campus.express.entity.IdentityAppeal;
import com.campus.express.entity.ShadowAccount;
import com.campus.express.entity.User;
import com.campus.express.repository.IdentityAppealRepository;
import com.campus.express.repository.ShadowAccountRepository;
import com.campus.express.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文档 2.2：申诉处理服务 - 提交申诉、SLA 监控、审核、解决冲突
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityAppealService {

    private static final int APPEAL_SLA_MINUTES = 30;
    private static final int SLA_URGENT_MINUTES = 5;
    private static final int SLA_AUTO_ASSIGN_MINUTES = 2;

    private final IdentityAppealRepository appealRepository;
    private final ShadowAccountRepository shadowAccountRepository;
    private final UserRepository userRepository;

    /**
     * 文档 2.2：提交身份申诉
     */
    @Transactional
    public AppealResult submitAppeal(AppealRequest request) {
        if (!canSubmitAppeal(request.getUserId(), request.getShadowId())) {
            return AppealResult.denied("无权申诉");
        }

        String proofUrlsStr = request.getProofUrls() != null && !request.getProofUrls().isEmpty()
            ? String.join(",", request.getProofUrls())
            : null;

        IdentityAppeal appeal = IdentityAppeal.builder()
            .userId(request.getUserId())
            .shadowId(request.getShadowId())
            .appealType(request.getAppealType() != null ? request.getAppealType() : "IDENTITY_CONFLICT")
            .proofUrls(proofUrlsStr)
            .description(request.getDescription())
            .status(IdentityAppeal.AppealStatus.PENDING)
            .priority("NORMAL")
            .submittedAt(LocalDateTime.now())
            .slaDeadline(LocalDateTime.now().plusMinutes(APPEAL_SLA_MINUTES))
            .build();

        appeal = appealRepository.save(appeal);
        return AppealResult.submitted(appeal.getId(), appeal.getSlaDeadline());
    }

    /**
     * 文档 2.2：验证申诉权限 - 当前用户即 shadow 对应的 originalUsername 对应用户
     */
    private boolean canSubmitAppeal(Long userId, Long shadowId) {
        if (userId == null || shadowId == null) {
            return false;
        }
        Optional<ShadowAccount> shadowOpt = shadowAccountRepository.findById(shadowId);
        if (shadowOpt.isEmpty()) {
            return false;
        }
        ShadowAccount shadow = shadowOpt.get();
        if (shadow.getStatus() != ShadowAccount.ShadowStatus.PENDING) {
            return false;
        }
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getUsername().equals(shadow.getOriginalUsername());
    }

    /**
     * 文档 2.2：快速审核通道（SLA: 30分钟），每分钟检查
     */
    @Scheduled(fixedDelay = 60000)
    public void processAppealsWithinSLA() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusMinutes(SLA_URGENT_MINUTES + 1);
        List<IdentityAppeal> pendingAppeals = appealRepository
            .findByStatusAndSlaDeadlineBetweenOrderBySlaDeadlineAsc(
                IdentityAppeal.AppealStatus.PENDING,
                now,
                end
            );

        for (IdentityAppeal appeal : pendingAppeals) {
            long minutesLeft = Duration.between(now, appeal.getSlaDeadline()).toMinutes();
            if (minutesLeft <= SLA_URGENT_MINUTES) {
                appeal.setPriority("URGENT");
                appealRepository.save(appeal);
                log.info("申诉临近SLA截止，升级优先级: appealId={}, minutesLeft={}", appeal.getId(), minutesLeft);
            }
            if (minutesLeft <= SLA_AUTO_ASSIGN_MINUTES) {
                log.info("申诉即将超时，可自动分配给管理员: appealId={}", appeal.getId());
            }
        }
    }

    /**
     * 文档 2.2：审核身份申诉
     */
    @Transactional
    public ReviewResult reviewAppeal(Long appealId, String reviewerId, boolean approved, String comments) {
        IdentityAppeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new RuntimeException("申诉不存在"));

        appeal.setStatus(approved ? IdentityAppeal.AppealStatus.APPROVED : IdentityAppeal.AppealStatus.REJECTED);
        appeal.setReviewedAt(LocalDateTime.now());
        appeal.setReviewerId(reviewerId);
        appeal.setReviewComments(comments);
        appealRepository.save(appeal);

        if (approved) {
            resolveIdentityConflict(appeal.getShadowId(), appeal.getUserId());
            return ReviewResult.approved(appealId);
        } else {
            return ReviewResult.rejected(appealId, comments != null ? comments : "审核未通过");
        }
    }

    /**
     * 文档 2.2：解决身份冲突
     */
    private void resolveIdentityConflict(Long shadowId, Long userId) {
        ShadowAccount shadow = shadowAccountRepository.findById(shadowId)
            .orElseThrow(() -> new RuntimeException("影子账户不存在"));

        shadow.setStatus(ShadowAccount.ShadowStatus.RESOLVED);
        shadow.setResolvedAt(LocalDateTime.now());
        shadow.setResolvedBy("APPEAL_SYSTEM");
        shadowAccountRepository.save(shadow);

        repairDataSourceConflicts(shadow, userId);
    }

    /**
     * 修复数据源冲突：当前实现仅标记影子已解决，可选合并/去重其他账号
     */
    private void repairDataSourceConflicts(ShadowAccount shadow, Long userId) {
        log.info("身份冲突已解决: shadowId={}, userId={}", shadow.getId(), userId);
    }

    /** 用户可用的待处理申诉列表（按 shadow 对应） */
    public List<IdentityAppeal> findMyAppeals(Long userId) {
        return appealRepository.findByUserIdOrderBySubmittedAtDesc(userId);
    }

    /** 管理员：待审核申诉列表（所有 PENDING） */
    public List<IdentityAppeal> findPendingAppeals() {
        return appealRepository.findByStatusOrderBySlaDeadlineAsc(IdentityAppeal.AppealStatus.PENDING);
    }
}
