package com.nageoffer.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nageoffer.shortlink.project.dao.entity.*;
import com.nageoffer.shortlink.project.dao.mapper.*;
import com.nageoffer.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import com.nageoffer.shortlink.project.dto.resp.*;
import com.nageoffer.shortlink.project.service.ShortLinkStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ShortLinkStatsServiceImpl implements ShortLinkStatsService {
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkAccessLogMapper linkAccessLogMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    public ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam){
        List<LinkAccessStatsDO> linkAccessStatsDOList = linkAccessStatsMapper.listStatsByShortLink(requestParam);
        if (CollUtil.isEmpty(linkAccessStatsDOList)){
            return null;
        }
        // 基础访问数据
        LinkAccessStatsDO uvUidStatsByShortLink = linkAccessLogMapper.findPvUvUidStatsByShortLink(requestParam);
        // 基础访问详情
        List<ShortLinkStatsAccessDailyRespDTO> daily = new ArrayList<>();
        List<String> rangeDates = DateUtil.rangeToList(DateUtil.parse(requestParam.getStartDate()), DateUtil.parse(requestParam.getEndDate()), DateField.DAY_OF_MONTH).stream()
                .map(DateUtil::formatDate)
                .toList();
        rangeDates.forEach(each  ->linkAccessStatsDOList.stream()
                .filter(item -> Objects.equals(each,DateUtil.formatDate(item.getDate())))
                .findFirst()
                .ifPresentOrElse(item -> {
                    ShortLinkStatsAccessDailyRespDTO build = ShortLinkStatsAccessDailyRespDTO.builder()
                            .date(each)
                            .pv(item.getPv())
                            .uv(item.getUv())
                            .uip(item.getUip())
                            .build();
                    daily.add(build);
                },()-> {
                    ShortLinkStatsAccessDailyRespDTO build = ShortLinkStatsAccessDailyRespDTO.builder()
                            .date(each)
                            .uv(0)
                            .pv(0)
                            .uip(0)
                            .build();
                    daily.add(build);
                })
        );
        // 地区访问详情
        List<ShortLinkStatsLocaleCNRespDTO> localeCNStats = new ArrayList<>();
        List<LinkLocaleStatsDO> linkLocaleStatsDOS = linkLocaleStatsMapper.listLocaleByShortLink(requestParam);
        int localeCnSum = linkLocaleStatsDOS.stream()
                .mapToInt(LinkLocaleStatsDO::getCnt)
                .sum();
        linkLocaleStatsDOS.forEach(each ->{
            double ratio = (double) each.getCnt() / localeCnSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsLocaleCNRespDTO localeCNRespDTO = ShortLinkStatsLocaleCNRespDTO.builder()
                    .cnt(each.getCnt())
                    .ratio(actualRatio)
                    .locale(each.getProvince())
                    .build();
            localeCNStats.add(localeCNRespDTO);
        });
        // 小时访问详情
        List<Integer> hourStats = new ArrayList<>();
        List<LinkAccessStatsDO> accessStatsDOS = linkAccessStatsMapper.listHourStatsByShortLink(requestParam);
        for (int i=0;i<24;i++){
            AtomicInteger hour = new AtomicInteger(i);
            Integer integer = accessStatsDOS.stream()
                    .filter(each -> Objects.equals(each.getHour(), hour.get()))
                    .findFirst()
                    .map(LinkAccessStatsDO::getPv)
                    .orElse(0);
            hourStats.add(integer);
        }
        // 高频访问IP详情
        List<ShortLinkStatsTopIpRespDTO> topIpRespDTOS = new ArrayList<>();
        List<HashMap<String, Object>> listTopIpByShortLink = linkAccessLogMapper.listTopIpByShortLink(requestParam);
        listTopIpByShortLink.forEach(each ->{
            ShortLinkStatsTopIpRespDTO build = ShortLinkStatsTopIpRespDTO.builder()
                    .ip(each.get("ip").toString())
                    .cnt(Integer.parseInt(each.get("count").toString()))
                    .build();
            topIpRespDTOS.add(build);
        });
        // 一周访问详情
        List<Integer> weekdayStats = new ArrayList<>();
        List<LinkAccessStatsDO> listWeekdayStatsByShortLink = linkAccessStatsMapper.listWeekdayStatsByShortLink(requestParam);
        for (int i=1;i<8;i++){
            AtomicInteger weekday = new AtomicInteger(i);
            Integer integer = listWeekdayStatsByShortLink.stream()
                    .filter(each -> Objects.equals(each.getWeekday(), weekday.get()))
                    .findFirst()
                    .map(LinkAccessStatsDO::getPv)
                    .orElse(0);
            weekdayStats.add(integer);
        }
        // 浏览器访问详情
        List<ShortLinkStatsBrowserRespDTO> browserRespDTOS = new ArrayList<>();
        List<HashMap<String, Object>> hashMaps = linkBrowserStatsMapper.listBrowserStatsByShortLink(requestParam);
        int sum = hashMaps.stream()
                .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
                .sum();
        hashMaps.forEach(each -> {
            double ratio = (double) Integer.parseInt(each.get("count").toString()) / sum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsBrowserRespDTO build = ShortLinkStatsBrowserRespDTO.builder()
                    .ratio(actualRatio)
                    .cnt(Integer.parseInt(each.get("count").toString()))
                    .browser(each.get("browser").toString())
                    .build();
            browserRespDTOS.add(build);
        });
        // 操作系统访问详情
        List<ShortLinkStatsOsRespDTO> osRespDTOS = new ArrayList<>();
        List<HashMap<String, Object>> maps = linkOsStatsMapper.listOsStatsByShortLink(requestParam);
        int sumOs = maps.stream()
                .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
                .sum();
        maps.forEach(each -> {
            double ratio = (double) Integer.parseInt(each.get("count").toString()) / sum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsOsRespDTO build = ShortLinkStatsOsRespDTO.builder()
                    .ratio(actualRatio)
                    .cnt(Integer.parseInt(each.get("count").toString()))
                    .os(each.get("os").toString())
                    .build();
            osRespDTOS.add(build);
        });
        // 访客访问类型详情
        List<ShortLinkStatsUvRespDTO> uvTypeStats = new ArrayList<>();
        HashMap<String, Object> findUvTypeByShortLink = linkAccessLogMapper.findUvTypeCntByShortLink(requestParam);
        int oldUserCnt = Integer.parseInt(
                Optional.ofNullable(findUvTypeByShortLink)
                        .map(each -> each.get("oldUserCnt"))
                        .map(Object::toString)
                        .orElse("0")
        );
        int newUserCnt = Integer.parseInt(
                Optional.ofNullable(findUvTypeByShortLink)
                        .map(each -> each.get("newUserCnt"))
                        .map(Object::toString)
                        .orElse("0")
        );
        int uvSum = oldUserCnt + newUserCnt;
        double oldRatio = (double) oldUserCnt / uvSum;
        double actualOldRatio = Math.round(oldRatio * 100.0) / 100.0;
        double newRatio = (double) newUserCnt / uvSum;
        double actualNewRatio = Math.round(newRatio * 100.0) / 100.0;
        ShortLinkStatsUvRespDTO newUvRespDTO = ShortLinkStatsUvRespDTO.builder()
                .uvType("newUser")
                .cnt(newUserCnt)
                .ratio(actualNewRatio)
                .build();
        uvTypeStats.add(newUvRespDTO);
        ShortLinkStatsUvRespDTO oldUvRespDTO = ShortLinkStatsUvRespDTO.builder()
                .uvType("oldUser")
                .cnt(oldUserCnt)
                .ratio(actualOldRatio)
                .build();
        uvTypeStats.add(oldUvRespDTO);
        // 访问设备类型详情
        List<ShortLinkStatsDeviceRespDTO> deviceRespDTOS = new ArrayList<>();
        List<LinkDeviceStatsDO> linkDeviceStatsDOS = linkDeviceStatsMapper.listDeviceStatsByShortLink(requestParam);
        int sum1 = linkDeviceStatsDOS.stream()
                .mapToInt(LinkDeviceStatsDO::getCnt)
                .sum();
        linkDeviceStatsDOS.forEach(each ->{
            double ratio = (double) each.getCnt() / sum1;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsDeviceRespDTO deviceRespDTO = ShortLinkStatsDeviceRespDTO.builder()
                    .cnt(each.getCnt())
                    .device(each.getDevice())
                    .ratio(actualRatio)
                    .build();
            deviceRespDTOS.add(deviceRespDTO);
        });
        // 访问网络类型详情
        List<ShortLinkStatsNetworkRespDTO> networkStats = new ArrayList<>();
        List<LinkNetworkStatsDO> listNetworkStatsByShortLink = linkNetworkStatsMapper.listNetworkStatsByShortLink(requestParam);
        int networkSum = listNetworkStatsByShortLink.stream()
                .mapToInt(LinkNetworkStatsDO::getCnt)
                .sum();
        listNetworkStatsByShortLink.forEach(each -> {
            double ratio = (double) each.getCnt() / networkSum;
            double actualRatio = Math.round(ratio * 100.0) / 100.0;
            ShortLinkStatsNetworkRespDTO networkRespDTO = ShortLinkStatsNetworkRespDTO.builder()
                    .cnt(each.getCnt())
                    .network(each.getNetwork())
                    .ratio(actualRatio)
                    .build();
            networkStats.add(networkRespDTO);
        });
        return ShortLinkStatsRespDTO.builder()
                .pv(uvUidStatsByShortLink.getPv())
                .uv(uvUidStatsByShortLink.getUv())
                .uip(uvUidStatsByShortLink.getUip())
                .daily(daily)
                .localeCnStats(localeCNStats)
                .hourStats(hourStats)
                .topIpStats(topIpRespDTOS)
                .weekdayStats(weekdayStats)
                .browserStats(browserRespDTOS)
                .osStats(osRespDTOS)
                .uvTypeStats(uvTypeStats)
                .deviceStats(deviceRespDTOS)
                .networkStats(networkStats)
                .build();
    }

    @Override
    public IPage<ShortLinkStatsAccessRecordRespDTO> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        LambdaQueryWrapper<LinkAccessLogDO> queryWrapper = Wrappers.lambdaQuery(LinkAccessLogDO.class)
                .eq(LinkAccessLogDO::getGid, requestParam.getGid())
                .between(LinkAccessLogDO::getCreateTime,requestParam.getStartDate(),requestParam.getEndDate())
                .eq(LinkAccessLogDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(LinkAccessLogDO::getDelFlag, 0)
                .orderByDesc(LinkAccessLogDO::getCreateTime);
        IPage<LinkAccessLogDO> shortLinkStatsAccessPage = linkAccessLogMapper.selectPage(requestParam, queryWrapper);
        List<String> userList = shortLinkStatsAccessPage.getRecords().stream().map(LinkAccessLogDO::getUser).toList();
        IPage<ShortLinkStatsAccessRecordRespDTO> convert = shortLinkStatsAccessPage.convert(each -> BeanUtil.toBean(each, ShortLinkStatsAccessRecordRespDTO.class)
        );
        List<Map<String, Object>> unTypeList = linkAccessLogMapper.selectUvTypeByUsers(requestParam.getGid(), requestParam.getFullShortUrl(), requestParam.getStartDate(), requestParam.getEndDate(), userList);

        convert.getRecords().stream().forEach(each -> {
            String s = unTypeList.stream()
                    .filter(item -> Objects.equals(each.getUser(), item.get("user")))
                    .findFirst()
                    .map(item -> item.get("uvType"))
                    .map(Object::toString)
                    .orElse("老访客");
            each.setUvType(s);
        });
        return convert;
    }
}
