package com.nageoffer.shortlink.admin.dto.req;

import lombok.Data;

@Data
public class RecycleBinDelReqDTO {
    private String gid;
    private String fullShortUrl;
}
