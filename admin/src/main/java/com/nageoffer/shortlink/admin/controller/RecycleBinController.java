package com.nageoffer.shortlink.admin.controller;


import com.nageoffer.shortlink.admin.common.convention.Result;
import com.nageoffer.shortlink.admin.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.ShortLinkRemoteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class RecycleBinController {
    ShortLinkRemoteService service = new ShortLinkRemoteService() {
    };

    @PostMapping("/api/short-link/admin/v1/recycle-bin/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam){
        return service.saveRecycleBin(requestParam);
    }
}
