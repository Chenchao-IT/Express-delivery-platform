package com.campus.express.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 文档 2.2：提交身份申诉请求
 */
@Data
public class AppealRequest {

    /** 由后端从当前登录用户填充，前端可不传 */
    private Long userId;

    @NotNull(message = "影子账户ID不能为空")
    private Long shadowId;

    /** 申诉类型，如 IDENTITY_CONFLICT */
    private String appealType;

    /** 证明材料 URL 列表（前端先上传文件获得 URL 后传入） */
    private List<String> proofUrls;

    private String description;
}
