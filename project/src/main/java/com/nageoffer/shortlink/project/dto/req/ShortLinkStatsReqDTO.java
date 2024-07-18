package com.nageoffer.shortlink.project.dto.req;

import lombok.Data;

import java.util.List;

@Data
public class ShortLinkStatsReqDTO {
    private String fullShortUrl;
    private String gid;
    private String startDate;
    private String endDate;



}
