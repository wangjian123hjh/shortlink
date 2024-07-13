package com.nageoffer.shortlink.admin.dto.resp;

import lombok.Data;

// 短链接分组查询返回参数
@Data
public class ShortLinkCountQueryRespDTO {
    private String gid;
    private Integer shortLinkCount;
}
