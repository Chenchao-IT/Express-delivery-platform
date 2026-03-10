package com.campus.express.dto;

import com.campus.express.entity.ExternalIdentity;
import com.campus.express.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 文档 2.2：多源身份 - 主账号 + 关联冲突账号/外部数据源
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiSourceIdentity {

    /** 主用户 ID（按用户名查到的用户） */
    private Long primaryId;
    private String username;
    private User primaryUser;

    /** 与主账号发生唯一键冲突的其他用户（同手机/同邮箱） */
    private List<User> linkedConflictUsers;

    /** 外部数据源记录（用于非关键冲突：同用户不同源字段不一致） */
    private List<ExternalIdentity> externalRecords;

    public static MultiSourceIdentity of(User primary) {
        return MultiSourceIdentity.builder()
            .primaryId(primary != null ? primary.getId() : null)
            .username(primary != null ? primary.getUsername() : null)
            .primaryUser(primary)
            .linkedConflictUsers(Collections.emptyList())
            .externalRecords(Collections.emptyList())
            .build();
    }
}
