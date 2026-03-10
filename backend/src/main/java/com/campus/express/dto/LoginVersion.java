package com.campus.express.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * 带版本号的登录信息（文档 1.2）
 * 登录版本号，每次密码修改、封号等递增；用于 JWT 无状态鉴权时的版本校验。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVersion implements Serializable {

    private Long userId;
    /** 登录版本号，每次密码修改、封号等递增 */
    private Integer version;
    /** 版本更新时间 */
    private Long timestamp;
    /** 版本更新原因 */
    private String reason;
    /** 已失效的 token 指纹（可选） */
    private Set<String> invalidatedTokens;

    /** 文档 1.2：校验 token 版本是否有效 */
    public boolean isValid(Integer tokenVersion) {
        return tokenVersion != null && tokenVersion >= this.version;
    }
}
