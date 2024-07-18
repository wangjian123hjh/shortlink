package com.nageoffer.shortlink.admin.remote.dto;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.admin.common.convention.Result;
import com.nageoffer.shortlink.admin.dto.req.*;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkCountQueryRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkStatsRespDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.RecycleBinRecoverReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
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

    default void updateShortLink(ShortLinkUpdateReqDTO requestParam){
        String s = HttpUtil.post("http://127.0.0.1:8002/api/short-link/v1/update", JSON.toJSONString(requestParam));
        Result result = JSON.parseObject(s, new TypeReference<Result>() {
        });
    }

    default Result<String> getTitleByUrl(String url){
        String s = HttpUtil.get("http://127.0.0.1:8002/api/short-link/v1/title?url="+url);
        Result<String> result = JSON.parseObject(s, new TypeReference<Result<String>>() {
        });
        return result;
    }

    default Result saveRecycleBin(RecycleBinSaveReqDTO requestParam){
        Map<String,Object> map = new HashMap<>();
        map.put("gid",requestParam.getGid());
        map.put("fullShortUrl",requestParam.getFullShortUrl());
        String s = HttpUtil.post("http://127.0.0.1:8002/api/short-link/v1/recycle-bin/save", JSONUtil.toJsonStr(map));
        Result result = JSON.parseObject(s, new TypeReference<Result>() {
        });
        return result;
    }

    // 分页查询回收站短链接
    default Result<IPage<ShortLinkPageRespDTO>> pageRecycleShortLink(ShortLinkRecycleBinPageReqDTO requestParam){
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("gids",requestParam.getGids());
        requestMap.put("current",requestParam.getCurrent());
        requestMap.put("size",requestParam.getSize());
        String resultPage = HttpUtil.get("http://127.0.0.1:8002/api/short-link/v1/recycle-bin/page", requestMap);
        Result<IPage<ShortLinkPageRespDTO>> iPageResult = JSON.parseObject(resultPage, new TypeReference<Result<IPage<ShortLinkPageRespDTO>>>() {
        });
        return iPageResult;
    }

    default Result recoverRecycleBin(RecycleBinRecoverReqDTO requestParam) {
        Map<String,Object> map = new HashMap<>();
        map.put("gid",requestParam.getGid());
        map.put("fullShortUrl",requestParam.getFullShortUrl());
        String s = HttpUtil.post("http://127.0.0.1:8002/api/short-link/v1/recycle-bin/recover", JSONUtil.toJsonStr(map));
        Result result = JSON.parseObject(s, new TypeReference<Result>() {
        });
        return result;
    }

    default Result removeRecycleBin(RecycleBinDelReqDTO requestParam){
        Map<String,Object> map = new HashMap<>();
        map.put("gid",requestParam.getGid());
        map.put("fullShortUrl",requestParam.getFullShortUrl());
        String s = HttpUtil.post("http://127.0.0.1:8002/api/short-link/v1/recycle-bin/remove", JSONUtil.toJsonStr(map));
        Result result = JSON.parseObject(s, new TypeReference<Result>() {
        });
        return result;
    }

    default Result<ShortLinkStatsRespDTO> oneShortLinkStats(ShortLinkStatsReqDTO requestParam){
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("gid",requestParam.getGid());
        requestMap.put("fullShortUrl",requestParam.getFullShortUrl());
        requestMap.put("startDate",requestParam.getStartDate());
        requestMap.put("endDate",requestParam.getEndDate());
        String result = HttpUtil.get("http://127.0.0.1:8002/api/short-link/v1/stats", requestMap);
        Result<ShortLinkStatsRespDTO> Resultc = JSON.parseObject(result, new TypeReference<Result<ShortLinkStatsRespDTO>>() {
        });
        return Resultc;
    }

    default Result<IPage<ShortLinkStatsAccessRecordRespDTO>>  shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam){
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("gid",requestParam.getGid());
        requestMap.put("fullShortUrl",requestParam.getFullShortUrl());
        requestMap.put("startDate",requestParam.getStartDate());
        requestMap.put("endDate",requestParam.getEndDate());
        String result = HttpUtil.get("http://127.0.0.1:8002/api/short-link/v1/stats/access-record", requestMap);
        Result<IPage<ShortLinkStatsAccessRecordRespDTO>> Resultc = JSON.parseObject(result, new TypeReference<Result<IPage<ShortLinkStatsAccessRecordRespDTO>>>() {
        });
        return Resultc;
    }
}
