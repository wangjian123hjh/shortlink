package com.nageoffer.shortlink.admin.dto.resp;

import lombok.Data;

@Data
public class ShortLinkGroupRespDTO {
    private String gid; //分组标识
    private String name;
    private String username;
    private Integer sortOrder;
}
