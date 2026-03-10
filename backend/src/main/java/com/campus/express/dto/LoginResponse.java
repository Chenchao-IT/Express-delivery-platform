package com.campus.express.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应（文档 2.2：AuthResult 多种形态）
 * - 正常登录：token, username, role, realName
 * - 非关键冲突：+ hasConflicts=true, message="部分信息冲突，部分功能受限"
 * - 关键冲突需申诉：conflictResolutionRequired=true, tempToken, shadowId, minimalPermissions, message="检测到身份冲突，请上传学生证照片进行申诉"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String username;
    private String role;
    private String realName;

    /** 文档 2.2：非关键冲突时部分功能受限 */
    private Boolean hasConflicts;
    /** 文档 2.2：提示文案 */
    private String message;

    /** 文档 2.2：关键冲突需申诉 */
    private Boolean conflictResolutionRequired;
    private Long shadowId;
    /** 文档 2.2：最小权限集合 order:receive, order:query, profile:view, conflict:appeal */
    private List<String> minimalPermissions;
}
