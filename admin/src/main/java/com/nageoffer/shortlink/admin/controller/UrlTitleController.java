package com.nageoffer.shortlink.admin.controller;

import com.nageoffer.shortlink.admin.common.convention.Result;
import com.nageoffer.shortlink.admin.remote.dto.ShortLinkRemoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UrlTitleController {
    ShortLinkRemoteService service = new ShortLinkRemoteService() {
    };
    @GetMapping("/api/short-link/admin/v1/title")
    public Result<String> getTitleByUrl(@RequestParam("url")String url){
        return service.getTitleByUrl(url);
    }
}
