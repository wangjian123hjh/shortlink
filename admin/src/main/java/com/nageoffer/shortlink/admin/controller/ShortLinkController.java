package com.nageoffer.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.admin.common.convention.Result;
import com.nageoffer.shortlink.admin.common.convention.Results;
import com.nageoffer.shortlink.admin.dto.req.*;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkStatsRespDTO;
import com.nageoffer.shortlink.admin.remote.dto.ShortLinkRemoteService;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShortLinkController {
    @GetMapping("/api/short-link/admin/v1/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){
        ShortLinkRemoteService service = new ShortLinkRemoteService() {
        };
        return service.pageShortLink(requestParam);
    }

    @PostMapping("/api/short-link/admin/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam){
        ShortLinkRemoteService service = new ShortLinkRemoteService() {
        };
        return service.createShortLink(requestParam);
    }

    @PostMapping ("/api/short-link/admin/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam){
        ShortLinkRemoteService service = new ShortLinkRemoteService() {
        };
        service.updateShortLink(requestParam);
        return Results.success();
    }
    @GetMapping("/api/short-link/admin/v1/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam){
        ShortLinkRemoteService service = new ShortLinkRemoteService() {
        };
        return service.oneShortLinkStats(requestParam);
    }
    @GetMapping("/api/short-link/admin/v1/stats/access-record")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam){
        ShortLinkRemoteService service = new ShortLinkRemoteService() {
        };
        return service.shortLinkStatsAccessRecord(requestParam);
    }
    @GetMapping("/api/short-link/admin/v1/stats/group")
    public Result<ShortLinkStatsRespDTO> groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam){
        ShortLinkRemoteService service = new ShortLinkRemoteService() {
        };
        return service.groupShortLinkStats(requestParam);
    }
    @GetMapping("/api/short-link/admin/v1/stats/access-record/group")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> groupshortLinkStatsAccessRecordgroup(ShortLinkGroupStatsAccessRecordReqDTO requestParam){
        ShortLinkRemoteService service = new ShortLinkRemoteService() {
        };
        return service.groupshortLinkStatsAccessRecord(requestParam);
    }
}
