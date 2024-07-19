package com.nageoffer.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.common.constant.RedisKeyConstant;
import com.nageoffer.shortlink.project.common.convention.exception.ClientException;
import com.nageoffer.shortlink.project.common.enums.VailDateTypeEnum;
import com.nageoffer.shortlink.project.config.GotoDomainWhiteListConfiguration;
import com.nageoffer.shortlink.project.dao.entity.*;
import com.nageoffer.shortlink.project.dao.mapper.*;
import com.nageoffer.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.project.dto.resp.*;
import com.nageoffer.shortlink.project.service.LinkStatsTodayService;
import com.nageoffer.shortlink.project.service.ShortLinkGotoService;
import com.nageoffer.shortlink.project.service.ShortLinkService;
import com.nageoffer.shortlink.project.util.HashUtil;
import com.nageoffer.shortlink.project.util.IpUtil;
import com.nageoffer.shortlink.project.util.LinkUtil;
import com.nageoffer.shortlink.project.util.UserAgentUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.rowset.serial.SerialException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter rBloomFilter;

    private final ShortLinkMapper shortLinkMapper;
    private final RedissonClient redissonClient;

    private final StringRedisTemplate stringRedisTemplate;

    private final ShortLinkGotoService shortLinkGotoService;

    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private static final  String AMAP_URL = "https://restapi.amap.com/v3/ip";

    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;

    private final LinkOsStatsMapper linkOsStatsMapper;

    private final LinkAccessLogMapper linkAccessLogMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkStatsTodayService linkStatsTodayService;

    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;

    private final GotoDomainWhiteListConfiguration gotoDomainWhiteListConfiguration;
    @Value("${short-link.stats.locale.amap-key}")
    private String key;
    @Value("${short-link.domain.default}")
    private String domainDefalueKey;
    @Override
    @SneakyThrows
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        verificationWhitelist(requestParam.getOriginUrl());
        String s = generateSuffix(requestParam);
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(domainDefalueKey)
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .delFlag(0)
                .totalUv(0)
                .totalPv(0)
                .totalUip(0)
                .shortUri(s)
                .favicon(getFavicon(requestParam.getOriginUrl()))
                .enableStatus(0)
                .fullShortUrl(requestParam.getDomain()+"/"+s)
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            ShortLinkGotoDO shortLinkGotoDO = ShortLinkGotoDO.builder()
                    .gid(shortLinkDO.getGid())
                    .fullShortUrl(shortLinkDO.getFullShortUrl()).build();
            shortLinkGotoService.save(shortLinkGotoDO);
        }catch (DuplicateKeyException e){
            rBloomFilter.add(shortLinkDO.getFullShortUrl());
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, shortLinkDO.getFullShortUrl());
            ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
            if (hasShortLinkDO != null){
                log.warn("短链接：{}重复入库",shortLinkDO.getFullShortUrl());
                throw new ClientException("短链接生成重复");
            }
        }
        // 缓存预热
        stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY,shortLinkDO.getFullShortUrl()),requestParam.getOriginUrl(), LinkUtil.getLinkCacheValidDate(requestParam.getValidDate()),TimeUnit.SECONDS);
        rBloomFilter.add(shortLinkDO.getFullShortUrl());
        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl(shortLinkDO.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
//        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
//                .eq(ShortLinkDO::getGid, requestParam.getGid())
//                .eq(ShortLinkDO::getDelFlag, 0)
//                .eq(ShortLinkDO::getEnableStatus, 0)
//                .orderByDesc(ShortLinkDO::getCreateTime); // DESC 降序
//        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
//        return resultPage.convert(each -> BeanUtil.toBean(each,ShortLinkPageRespDTO.class));
        IPage<ShortLinkDO> shortLinkDOIPage = baseMapper.pageLink(requestParam);
        IPage<ShortLinkPageRespDTO> convert = shortLinkDOIPage.convert(each -> BeanUtil.toBean(each, ShortLinkPageRespDTO.class));
        return convert;

    }

    @Override
    public List<ShortLinkCountQueryRespDTO> listGroupShortLinkCount( List<String> gids) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO()).select("gid,count(*) as shortLinkCount")
                .in("gid", gids)
                .eq("enable_status", 0)
                .groupBy("gid");
        List<Map<String, Object>> maps = baseMapper.selectMaps(queryWrapper);
        List<ShortLinkCountQueryRespDTO> collect = maps.stream().map(each -> BeanUtil.mapToBean(each, ShortLinkCountQueryRespDTO.class, true)).collect(Collectors.toList());
        return collect;
    }

    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {

        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO shortLink = baseMapper.selectOne(queryWrapper);
        if (shortLink == null){
            throw new ClientException("短链接记录不存在");
        }
        if (Objects.equals(shortLink.getGid(),requestParam.getGid())){
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .set(ObjectUtil.equal(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, "");
            ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                    .domain(shortLink.getDomain())
                    .shortUri(shortLink.getShortUri())
                    .favicon(shortLink.getFavicon())
                    .createdType(shortLink.getCreatedType())
                    .gid(requestParam.getGid())
                    .originUrl(requestParam.getOriginUrl())
                    .describe(requestParam.getDescribe())
                    .validDateType(requestParam.getValidDateType())
                    .validDate(requestParam.getValidDate())
                    .build();
            baseMapper.update(shortLinkDO,updateWrapper);
        }else {
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(RedisKeyConstant.LOCK_GID_UPDATE_KEY, requestParam.getFullShortUrl()));
            RLock rLock = readWriteLock.writeLock();
            rLock.lock();
            try {
                LambdaUpdateWrapper<ShortLinkDO> linkUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkDO::getGid, shortLink.getGid())
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getDelTime, 0L)
                        .eq(ShortLinkDO::getEnableStatus, 0);
                ShortLinkDO delShortLinkDO = ShortLinkDO.builder()
                        .delTime(System.currentTimeMillis())
                        .build();
                delShortLinkDO.setDelFlag(1);
                baseMapper.update(delShortLinkDO, linkUpdateWrapper);
                ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                        .domain(domainDefalueKey)
                        .originUrl(requestParam.getOriginUrl())
                        .gid(requestParam.getGid())
                        .createdType(shortLink.getCreatedType())
                        .validDateType(requestParam.getValidDateType())
                        .validDate(requestParam.getValidDate())
                        .describe(requestParam.getDescribe())
                        .shortUri(shortLink.getShortUri())
                        .enableStatus(shortLink.getEnableStatus())
                        .totalPv(shortLink.getTotalPv())
                        .totalUv(shortLink.getTotalUv())
                        .totalUip(shortLink.getTotalUip())
                        .fullShortUrl(shortLink.getFullShortUrl())
                        .favicon(getFavicon(requestParam.getOriginUrl()))
                        .delTime(0L)
                        .build();
                baseMapper.insert(shortLinkDO);
                LambdaQueryWrapper<LinkStatsTodayDO> statsTodayQueryWrapper = Wrappers.lambdaQuery(LinkStatsTodayDO.class)
                        .eq(LinkStatsTodayDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkStatsTodayDO::getGid, shortLink.getGid())
                        .eq(LinkStatsTodayDO::getDelFlag, 0);
                List<LinkStatsTodayDO> linkStatsTodayDOList = linkStatsTodayMapper.selectList(statsTodayQueryWrapper);
                if (CollUtil.isNotEmpty(linkStatsTodayDOList)) {
                    linkStatsTodayMapper.deleteBatchIds(linkStatsTodayDOList.stream()
                            .map(LinkStatsTodayDO::getId)
                            .toList()
                    );
                    linkStatsTodayDOList.forEach(each -> each.setGid(requestParam.getGid()));
                    linkStatsTodayService.saveBatch(linkStatsTodayDOList);
                }
                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkGotoDO::getGid, shortLink.getGid());
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoService.getOne(linkGotoQueryWrapper);
                shortLinkGotoService.removeById(shortLinkGotoDO.getId());
                shortLinkGotoDO.setGid(requestParam.getGid());
                shortLinkGotoService.save(shortLinkGotoDO);
                LambdaUpdateWrapper<LinkAccessStatsDO> linkAccessStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkAccessStatsDO.class)
                        .eq(LinkAccessStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkAccessStatsDO::getGid, shortLink.getGid())
                        .eq(LinkAccessStatsDO::getDelFlag, 0);
                LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkAccessStatsMapper.update(linkAccessStatsDO, linkAccessStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkLocaleStatsDO> linkLocaleStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkLocaleStatsDO.class)
                        .eq(LinkLocaleStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkLocaleStatsDO::getGid, shortLink.getGid())
                        .eq(LinkLocaleStatsDO::getDelFlag, 0);
                LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkLocaleStatsMapper.update(linkLocaleStatsDO, linkLocaleStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkOsStatsDO> linkOsStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkOsStatsDO.class)
                        .eq(LinkOsStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkOsStatsDO::getGid, shortLink.getGid())
                        .eq(LinkOsStatsDO::getDelFlag, 0);
                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkOsStatsMapper.update(linkOsStatsDO, linkOsStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkBrowserStatsDO> linkBrowserStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkBrowserStatsDO.class)
                        .eq(LinkBrowserStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkBrowserStatsDO::getGid, shortLink.getGid())
                        .eq(LinkBrowserStatsDO::getDelFlag, 0);
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkBrowserStatsMapper.update(linkBrowserStatsDO, linkBrowserStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkDeviceStatsDO> linkDeviceStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkDeviceStatsDO.class)
                        .eq(LinkDeviceStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkDeviceStatsDO::getGid, shortLink.getGid())
                        .eq(LinkDeviceStatsDO::getDelFlag, 0);
                LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkDeviceStatsMapper.update(linkDeviceStatsDO, linkDeviceStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkNetworkStatsDO> linkNetworkStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkNetworkStatsDO.class)
                        .eq(LinkNetworkStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkNetworkStatsDO::getGid, shortLink.getGid())
                        .eq(LinkNetworkStatsDO::getDelFlag, 0);
                LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkNetworkStatsMapper.update(linkNetworkStatsDO, linkNetworkStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkAccessLogDO> linkAccessLogsUpdateWrapper = Wrappers.lambdaUpdate(LinkAccessLogDO.class)
                        .eq(LinkAccessLogDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkAccessLogDO::getGid, shortLink.getGid())
                        .eq(LinkAccessLogDO::getDelFlag, 0);
                LinkAccessLogDO linkAccessLogsDO = LinkAccessLogDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkAccessLogMapper.update(linkAccessLogsDO, linkAccessLogsUpdateWrapper);
            } finally {
                rLock.unlock();
            }
        }
        if (!Objects.equals(requestParam.getValidDateType(),shortLink.getValidDateType()) ||
            !Objects.equals(requestParam.getValidDate(),shortLink.getValidDate())){
            stringRedisTemplate.delete(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY,requestParam.getFullShortUrl()));
        }
    }
    // 短链接跳转
    @Override
    @SneakyThrows
    public void restoreUrl(String shortUri, HttpServletRequest request, HttpServletResponse response) {
        String serverName = request.getServerName();
        String scheme = request.getScheme();
        String port = Optional.of(request.getServerPort())
                .filter(each -> !Objects.equals(each, 80))
                .map(String::valueOf)
                .map(each -> ":" + each)
                .orElse("");
        String fullShortUrl =scheme +"://"+ serverName + port + "/" + shortUri;
        String orginalLink = stringRedisTemplate.opsForValue().get(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(orginalLink)){
            if (!orginalLink.equals("/page/notfund")){
                shortLinkstats(fullShortUrl,null,request,response);
            }
            response.setStatus(302);
            response.sendRedirect(orginalLink);
            return;
        }
        RLock lock = redissonClient.getLock(String.format(RedisKeyConstant.LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();
        try{
            orginalLink = stringRedisTemplate.opsForValue().get(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY, fullShortUrl));
            if (StrUtil.isNotBlank(orginalLink)){
                if (!orginalLink.equals("/page/notfund")){
                    shortLinkstats(fullShortUrl,null,request,response);
                }
                response.setStatus(302);
                response.sendRedirect(orginalLink);
                return;
            }
            if (rBloomFilter.contains(fullShortUrl)){
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO linkGotoDO = shortLinkGotoService.getOne(queryWrapper);
                if (linkGotoDO == null){
                    stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY,fullShortUrl),"/page/notfund",5,TimeUnit.MINUTES);
                    // 过期  跳转到错误页面
                    response.sendRedirect("/page/notfund");
                    return;
                }
                LambdaQueryWrapper<ShortLinkDO> queryWrapper1 = Wrappers.lambdaQuery(ShortLinkDO.class)
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getEnableStatus, 0)
                        .eq(ShortLinkDO::getGid, linkGotoDO.getGid())
                        .eq(ShortLinkDO::getFullShortUrl, fullShortUrl);
                ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper1);
                if (shortLinkDO != null){
                    if (shortLinkDO.getValidDateType().equals(VailDateTypeEnum.CUSTOM) && shortLinkDO.getValidDate().before(new Date())){
                        stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY,fullShortUrl),"/page/notfund",5,TimeUnit.MINUTES);
                        // 过期  跳转到错误页面
                        response.sendRedirect("/page/notfund");
                        return;
                    }
                    shortLinkstats(shortLinkDO.getFullShortUrl(),shortLinkDO.getGid(),request,response);
                    stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY,fullShortUrl),shortLinkDO.getOriginUrl(),LinkUtil.getLinkCacheValidDate(shortLinkDO.getValidDate()),TimeUnit.SECONDS);
                    orginalLink = shortLinkDO.getOriginUrl();
                    response.setStatus(302);
                    response.sendRedirect(orginalLink);
                    return;
                }else {
                    stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY,fullShortUrl),"/page/notfund");
                    response.sendRedirect("/page/notfund");
                    return;
                }
            }else {
                // 布隆过滤器没有说明一定不存在
                stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY,fullShortUrl),"/page/notfund",5, TimeUnit.MINUTES);
                // 过期  跳转到错误页面
                response.sendRedirect("/page/notfund");
                return;
            }
        }finally {
            lock.unlock();
        }

    }

    @Override
    public ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
        List<String> originUrls = requestParam.getOriginUrls();
        List<String> describes = requestParam.getDescribes();
        List<ShortLinkBaseInfoRespDTO> result = new ArrayList<>();
        for (int i = 0;i<originUrls.size();i++){
            ShortLinkCreateReqDTO shortLinkCreateReqDTO = BeanUtil.toBean(requestParam, ShortLinkCreateReqDTO.class);
            shortLinkCreateReqDTO.setOriginUrl(originUrls.get(i));
            shortLinkCreateReqDTO.setDescribe(describes.get(i));
            try {
                ShortLinkCreateRespDTO shortLink = createShortLink(shortLinkCreateReqDTO);
                ShortLinkBaseInfoRespDTO build = ShortLinkBaseInfoRespDTO.builder()
                        .fullShortUrl(shortLink.getFullShortUrl())
                        .originUrl(shortLink.getOriginUrl())
                        .describe(describes.get(i))
                        .build();
                result.add(build);
            }catch (Throwable ex){
                log.error("批量创建短链接失败，原始参数：{}",originUrls.get(i));
            }
        }
        return ShortLinkBatchCreateRespDTO.builder()
                .total(result.size())
                .baseLinkInfos(result)
                .build();
    }

    private String generateSuffix(ShortLinkCreateReqDTO requestParam) throws SerialException {
        int customGenerateCount = 0;
        String shortUri;
        String originUrl = requestParam.getOriginUrl();
        while (true){
            if (customGenerateCount>10){
                throw new SerialException("短链接频繁生成,请稍后再试");
            }
            shortUri = HashUtil.hashToBase62(originUrl);
            if (!rBloomFilter.contains(requestParam.getDomain() + "/" + shortUri)){
                break;
            }
            originUrl += UUID.randomUUID().toString();
            customGenerateCount++;
        }
        return shortUri;
    }
    @SneakyThrows
    private String getFavicon(String url){
        URL urlC = new URL(url);
        HttpURLConnection urlConnection = (HttpURLConnection)urlC.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.connect();
        int responseCode = urlConnection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK){
            Document document = Jsoup.connect(url).get();
            Element iconElement = document.select("link[rel~=(?i)^(shortcut|icon|favicon)]").first();
            if (iconElement != null){
                String href = iconElement.attr("href");
                return href;
            }
        }
        return null;
    }
    private void shortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO statsRecord){
        Map<String,String> producerMap = new HashMap<>();
        producerMap.put("fullShortUrl",fullShortUrl);
        producerMap.put("gid",gid);
        producerMap.put("statsRecord", JSON.toJSONString(statsRecord));

    }
    private void shortLinkstats(String fullShortUrl,String gid,HttpServletRequest request,HttpServletResponse response){


        try {
            AtomicBoolean uvFlag = new AtomicBoolean(true);
            AtomicReference<String> uv = new AtomicReference<>();
            Cookie[] cookies = request.getCookies();
            if (ArrayUtil.isNotEmpty(cookies)){
                Arrays.stream(cookies)
                        .filter(each -> Objects.equals("uv",each.getName()))
                        .findFirst()
                        .map(Cookie::getValue)
                        .ifPresentOrElse(each -> {
                            Long add = stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, each);
                            uvFlag.set((add!=null && add >0));
                            uv.set(each);
                        },()->{
                            uv.set(UUID.fastUUID().toString());
                            Cookie uvCookie = new Cookie("uv", uv.get());
                            uvCookie.setMaxAge(60*60*24*30);
                            uvCookie.setPath(StrUtil.sub(fullShortUrl,fullShortUrl.lastIndexOf("/"),fullShortUrl.length()));
                            response.addCookie(uvCookie);
                        });
            }else {
                uv.set(UUID.fastUUID().toString());
                Cookie uvCookie = new Cookie("uv", uv.get());
                uvCookie.setMaxAge(60*60*24*30);
                uvCookie.setPath(StrUtil.sub(fullShortUrl,fullShortUrl.lastIndexOf("/"),fullShortUrl.length()));
                response.addCookie(uvCookie);
            }
            String ip = IpUtil.getClientIp(request);
            Long add = stringRedisTemplate.opsForSet().add("short-link:stats:uip:" + fullShortUrl, ip);
            boolean uipFlag = false;
            if (add!=null && add >0){
                uipFlag = true;
            }
            if (StrUtil.isBlank(gid)){
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO one = shortLinkGotoService.getOne(queryWrapper);
                gid = one.getGid();
            }
            int hour = DateUtil.hour(new Date(), true);
            Week week = DateUtil.dayOfWeekEnum(new Date());
            int value = week.getIso8601Value();
            LinkAccessStatsDO build = LinkAccessStatsDO.builder()
                    .pv(1)
                    .uv(uvFlag.get()?1:0)
                    .uip(uipFlag?1:0)
                    .hour(hour)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .delFlag(0)
                    .weekday(value)
                    .build();
            linkAccessStatsMapper.shortLinkStats(build);
            // 地区统计
            Map<String,Object> local = new HashMap<>();
            local.put("key",key);
            local.put("ip","121.43.132.118");
            String s = HttpUtil.get(AMAP_URL, local);
            JSONObject entries = JSONUtil.parseObj(s);
            String infocode = entries.get("infocode").toString();
            LinkLocaleStatsDO localeStatsDO=null;
            String province=null;
            String city=null;
            if (StrUtil.isNotBlank(infocode) && infocode.equals("10000")){
                province = entries.get("province").toString();
                Boolean unknowFlag = (StrUtil.isNotBlank(province) && province.indexOf("[")==0);
                province = unknowFlag ? "未知" : province;
                city = unknowFlag ? "未知" : entries.get("city").toString();
                localeStatsDO = LinkLocaleStatsDO.builder()
                        .fullShortUrl(fullShortUrl)
                        .gid(gid)
                        .province(province)
                        .city(city)
                        .adcode(unknowFlag ? "未知" : entries.get("adcode").toString())
                        .country("中国")
                        .date(new Date())
                        .cnt(1)
                        .build();
            }
            linkLocaleStatsMapper.shortLinkLocaleState(localeStatsDO);
            // 浏览器统计
            String browser = UserAgentUtil.getBrowser(request);
            LinkBrowserStatsDO browserStatsDO = LinkBrowserStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .cnt(1)
                    .browser(browser)
                    .date(new Date())
                    .build();
            linkBrowserStatsMapper.shortLinkBrowserState(browserStatsDO);
            // 操作系统统计
            String os = UserAgentUtil.getOperatingSystem(request);
            LinkOsStatsDO statsDO = LinkOsStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .cnt(1)
                    .os(os)
                    .date(new Date())
                    .delFlag(0)
                    .build();
            linkOsStatsMapper.shortLinkOsState(statsDO);
            // 访问日志
            LinkAccessLogDO accessLogDO = LinkAccessLogDO.builder()
                    .user(uv.get())
                    .ip(ip)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .os(os)
                    .locale("中国-"+province+"-"+city)
                    .network(UserAgentUtil.getNetwork(request))
                    .browser(browser)
                    .device(UserAgentUtil.getDevice(request))
                    .delFlag(0)
                    .build();
            linkAccessLogMapper.insert(accessLogDO);
            // 访问设备
            LinkDeviceStatsDO deviceStatsDO = LinkDeviceStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .device(UserAgentUtil.getDevice(request))
                    .cnt(1)
                    .date(new Date())
                    .delFlag(0)
                    .build();
            linkDeviceStatsMapper.shortLinkDeviceState(deviceStatsDO);
            // 访问网络
            LinkNetworkStatsDO networkStats = LinkNetworkStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .network(UserAgentUtil.getNetwork(request))
                    .cnt(1)
                    .date(new Date())
                    .delFlag(0)
                    .build();
            linkNetworkStatsMapper.shortLinkBrowserState(networkStats);
            baseMapper.incrementStats(gid,fullShortUrl,1,uvFlag.get()?1:0,uipFlag?1:0);
            LinkStatsTodayDO build1 = LinkStatsTodayDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .todayUv(1)
                    .todayPv(uvFlag.get()?1:0)
                    .todayUip(uipFlag?1:0)
                    .delFlag(0)
                    .build();
            linkStatsTodayMapper.shortLinkTodayState(build1);
        }catch (Exception e){
            log.error("短链接监控统计出错",e);
        }

    }


    private void verificationWhitelist(String originUrl) {
        Boolean enable = gotoDomainWhiteListConfiguration.getEnable();
        if (enable == null || !enable) {
            return;
        }
        String domain = LinkUtil.extractDomain(originUrl);
        if (StrUtil.isBlank(domain)) {
            throw new ClientException("跳转链接填写错误");
        }
        List<String> details =

                gotoDomainWhiteListConfiguration.getDetails();
        if (!details.contains(domain)) {
            throw new ClientException("演示环境为避免恶意攻击，请生成以下网站跳转链接：" + gotoDomainWhiteListConfiguration.getNames());
        }
    }
}
