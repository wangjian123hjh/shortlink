package com.nageoffer.shortlink.admin.remote.dto;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.admin.common.convention.Result;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkCountQueryRespDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ShortLinkRemoteService {
    default Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("gid",requestParam.getGid());
        requestMap.put("current",requestParam.getCurrent());
        requestMap.put("size",requestParam.getSize());
        String resultPage = HttpUtil.get("http://127.0.0.1:8002/api/short-link/v1/page", requestMap);
        Result<IPage<ShortLinkPageRespDTO>> iPageResult = JSON.parseObject(resultPage, new TypeReference<Result<IPage<ShortLinkPageRespDTO>>>() {
        });
        return iPageResult;
    }

    default Result<ShortLinkCreateRespDTO> createShortLink(ShortLinkCreateReqDTO requestParam){
        String s = HttpUtil.post("http://127.0.0.1:8002/api/short-link/v1/create", JSON.toJSONString(requestParam));
        Result<ShortLinkCreateRespDTO> result = JSON.parseObject(s, new TypeReference<Result<ShortLinkCreateRespDTO>>() {
        });
        return result;
    }

    // 查询分组短链接总量
    default Result<List<ShortLinkCountQueryRespDTO>> listGroupShortLinkCount(List<String> gids){
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("gids",gids);
        String resultPage = HttpUtil.get("http://127.0.0.1:8002/api/short-link/v1/count", requestMap);
        Result<List<ShortLinkCountQueryRespDTO>> iResult = JSON.parseObject(resultPage, new TypeReference<Result<List<ShortLinkCountQueryRespDTO>>>() {
        });
        return iResult;
    }
}
