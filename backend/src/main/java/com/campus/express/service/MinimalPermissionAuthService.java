package com.campus.express.service;

import com.campus.express.dto.*;
import com.campus.express.entity.ConflictLog;
import com.campus.express.entity.ExternalIdentity;
import com.campus.express.entity.ShadowAccount;
import com.campus.express.entity.User;
import com.campus.express.exception.ValidationException;
import com.campus.express.repository.ConflictLogRepository;
import com.campus.express.repository.ExternalIdentityRepository;
import com.campus.express.repository.ShadowAccountRepository;
import com.campus.express.repository.UserRepository;
import com.campus.express.security.JwtTokenProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档 2.2：最小权限子集认证服务 - 处理多源身份冲突的登录
 */
@Service
@RequiredArgsConstructor
public class MinimalPermissionAuthService {

    private static final Set<String> MINIMAL_PERMISSIONS = Set.of(
        "order:receive",
        "order:query",
        "profile:view",
        "conflict:appeal"
    );

    private static final String MESSAGE_CRITICAL = "检测到身份冲突，请上传学生证照片进行申诉";
    private static final String MESSAGE_NON_CRITICAL = "部分信息冲突，部分功能受限";

    private final UserRepository userRepository;
    private final ShadowAccountRepository shadowAccountRepository;
    private final ConflictLogRepository conflictLogRepository;
    private final ExternalIdentityRepository externalIdentityRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 文档 2.2：处理多源身份冲突的登录
     */
    public LoginResponse loginWithConflictResolution(LoginRequest request) {
        // 1. 基础验证（格式）
        ValidationResult validation = validateLoginRequest(request);
        if (!validation.isValid()) {
            throw new ValidationException(validation.getErrors());
        }

        // 2. 多源身份查询
        MultiSourceIdentity identity = queryMultiSourceIdentity(request.getUsername());
        if (identity.getPrimaryUser() == null) {
            throw new ValidationException(List.of("用户不存在"));
        }

        // 3. 密码验证（统一先验证再分支）
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new ValidationException(List.of("用户名或密码错误"));
        }

        // 4. 检查冲突
        ConflictAnalysis analysis = analyzeConflicts(identity);

        if (analysis.hasCriticalConflict()) {
            return handleCriticalConflict(request, identity, analysis);
        }
        if (analysis.hasNonCriticalConflict()) {
            return handleNonCriticalConflict(request, identity, analysis);
        }
        return handleNormalLogin(request, identity);
    }

    private ValidationResult validateLoginRequest(LoginRequest request) {
        List<String> errors = new ArrayList<>();
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            errors.add("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            errors.add("密码不能为空");
        }
        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.invalid(errors);
    }

    /**
     * 多源身份查询：主用户 + 同手机/同邮箱的其他用户 + 外部数据源
     */
    private MultiSourceIdentity queryMultiSourceIdentity(String username) {
        User primary = userRepository.findByUsername(username).orElse(null);
        if (primary == null) {
            return MultiSourceIdentity.builder()
                .primaryId(null)
                .username(username)
                .primaryUser(null)
                .linkedConflictUsers(Collections.emptyList())
                .externalRecords(Collections.emptyList())
                .build();
        }

        List<User> linkedConflictUsers = new ArrayList<>();
        if (primary.getPhone() != null && !primary.getPhone().isBlank()) {
            linkedConflictUsers.addAll(userRepository.findByPhoneAndIdNot(primary.getPhone(), primary.getId()));
        }
        if (primary.getEmail() != null && !primary.getEmail().isBlank()) {
            userRepository.findByEmailAndIdNot(primary.getEmail(), primary.getId()).stream()
                .filter(u -> linkedConflictUsers.stream().noneMatch(l -> l.getId().equals(u.getId())))
                .forEach(linkedConflictUsers::add);
        }

        List<ExternalIdentity> externalRecords = externalIdentityRepository.findByUserId(primary.getId());

        return MultiSourceIdentity.builder()
            .primaryId(primary.getId())
            .username(primary.getUsername())
            .primaryUser(primary)
            .linkedConflictUsers(linkedConflictUsers)
            .externalRecords(externalRecords)
            .build();
    }

    /**
     * 冲突分析：关键冲突 = 唯一键冲突（同手机/邮箱多账号）；非关键 = 外部源与主用户字段不一致
     */
    private ConflictAnalysis analyzeConflicts(MultiSourceIdentity identity) {
        if (identity.getPrimaryUser() == null) {
            return ConflictAnalysis.noConflict();
        }

        User primary = identity.getPrimaryUser();
        List<User> linked = identity.getLinkedConflictUsers() != null ? identity.getLinkedConflictUsers() : Collections.emptyList();
        List<ExternalIdentity> external = identity.getExternalRecords() != null ? identity.getExternalRecords() : Collections.emptyList();

        // 关键冲突：存在其他账号使用同一手机或邮箱
        if (!linked.isEmpty()) {
            Set<String> sources = new HashSet<>();
            if (primary.getPhone() != null) {
                linked.stream().filter(u -> primary.getPhone().equals(u.getPhone())).findAny().ifPresent(u -> sources.add("USER_PHONE"));
            }
            if (primary.getEmail() != null) {
                linked.stream().filter(u -> primary.getEmail().equals(u.getEmail())).findAny().ifPresent(u -> sources.add("USER_EMAIL"));
            }
            Map<String, Object> conflictData = new HashMap<>();
            conflictData.put("primaryId", primary.getId());
            conflictData.put("conflictUserIds", linked.stream().map(User::getId).toList());
            return ConflictAnalysis.builder()
                .criticalConflict(true)
                .nonCriticalConflict(false)
                .conflictType("DUPLICATE_IDENTITY")
                .conflictingSources(sources)
                .conflictData(conflictData)
                .conflictingFields(Set.of("phone", "email"))
                .build();
        }

        // 非关键冲突：外部数据源与主用户字段不一致
        Set<String> conflictingFields = new HashSet<>();
        for (ExternalIdentity ext : external) {
            if (primary.getPhone() != null && ext.getPhone() != null && !primary.getPhone().equals(ext.getPhone())) {
                conflictingFields.add("phone");
            }
            if (primary.getEmail() != null && ext.getEmail() != null && !primary.getEmail().equals(ext.getEmail())) {
                conflictingFields.add("email");
            }
            if (primary.getRealName() != null && ext.getRealName() != null && !primary.getRealName().equals(ext.getRealName())) {
                conflictingFields.add("realName");
            }
        }
        if (!conflictingFields.isEmpty()) {
            return ConflictAnalysis.builder()
                .criticalConflict(false)
                .nonCriticalConflict(true)
                .conflictType("MULTI_SOURCE_FIELD_MISMATCH")
                .conflictingSources(external.stream().map(ExternalIdentity::getSource).collect(Collectors.toSet()))
                .conflictingFields(conflictingFields)
                .build();
        }

        return ConflictAnalysis.noConflict();
    }

    /**
     * 文档 2.2：关键冲突 - 创建影子账户，返回临时令牌与最小权限
     */
    private LoginResponse handleCriticalConflict(LoginRequest request, MultiSourceIdentity identity, ConflictAnalysis analysis) {
        String conflictingSourcesJson;
        String conflictDataJson;
        try {
            conflictingSourcesJson = objectMapper.writeValueAsString(analysis.getConflictingSources() != null ? analysis.getConflictingSources() : List.of());
            conflictDataJson = analysis.getConflictData() != null ? objectMapper.writeValueAsString(analysis.getConflictData()) : "{}";
        } catch (JsonProcessingException e) {
            conflictingSourcesJson = "[]";
            conflictDataJson = "{}";
        }

        ShadowAccount shadow = ShadowAccount.builder()
            .originalUsername(request.getUsername())
            .conflictType(analysis.getConflictType())
            .conflictingSources(conflictingSourcesJson)
            .conflictData(conflictDataJson)
            .status(ShadowAccount.ShadowStatus.PENDING)
            .createdAt(java.time.LocalDateTime.now())
            .build();
        shadow = shadowAccountRepository.save(shadow);

        User primary = identity.getPrimaryUser();
        int version = primary.getLoginVersion() != null ? primary.getLoginVersion() : 1;
        String tempToken = jwtTokenProvider.generateToken(request.getUsername(), primary.getId(), version);

        return LoginResponse.builder()
            .token(tempToken)
            .username(request.getUsername())
            .role(identity.getPrimaryUser().getRole().name())
            .realName(identity.getPrimaryUser().getRealName())
            .conflictResolutionRequired(true)
            .shadowId(shadow.getId())
            .minimalPermissions(new ArrayList<>(MINIMAL_PERMISSIONS))
            .message(MESSAGE_CRITICAL)
            .build();
    }

    /**
     * 文档 2.2：非关键冲突 - 最小权限子集，带冲突标记
     */
    private LoginResponse handleNonCriticalConflict(LoginRequest request, MultiSourceIdentity identity, ConflictAnalysis analysis) {
        User primary = identity.getPrimaryUser();
        Set<String> permissionSubset = calculateMinimalPermissionSubset(primary.getRole(), analysis.getConflictingFields());

        int version = primary.getLoginVersion() != null ? primary.getLoginVersion() : 1;
        String token = jwtTokenProvider.generateToken(primary.getUsername(), primary.getId(), version);

        ConflictLog log = ConflictLog.builder()
            .userId(primary.getId())
            .conflictType("NON_CRITICAL")
            .conflictingFields(analysis.getConflictingFields() != null ? String.join(",", analysis.getConflictingFields()) : null)
            .resolution("MINIMAL_PERMISSIONS")
            .loggedAt(java.time.LocalDateTime.now())
            .build();
        conflictLogRepository.save(log);

        return LoginResponse.builder()
            .token(token)
            .username(primary.getUsername())
            .role(primary.getRole().name())
            .realName(primary.getRealName())
            .hasConflicts(true)
            .message(MESSAGE_NON_CRITICAL)
            .build();
    }

    /**
     * 文档 2.2：计算最小权限子集（冲突字段相关权限可收紧，此处返回基础权限）
     */
    private Set<String> calculateMinimalPermissionSubset(User.UserRole role, Set<String> conflictingFields) {
        Set<String> base = new HashSet<>(MINIMAL_PERMISSIONS);
        base.add("order:create");
        base.add("order:list");
        if (role == User.UserRole.ADMIN || role == User.UserRole.COURIER) {
            base.add("admin:packages");
            base.add("admin:deliveries");
        }
        if (role == User.UserRole.ADMIN) {
            base.add("admin:users");
        }
        return base;
    }

    /**
     * 文档 2.2：无冲突 - 正常登录
     */
    private LoginResponse handleNormalLogin(LoginRequest request, MultiSourceIdentity identity) {
        User user = identity.getPrimaryUser();
        int version = user.getLoginVersion() != null ? user.getLoginVersion() : 1;
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId(), version);
        return LoginResponse.builder()
            .token(token)
            .username(user.getUsername())
            .role(user.getRole().name())
            .realName(user.getRealName())
            .build();
    }
}
