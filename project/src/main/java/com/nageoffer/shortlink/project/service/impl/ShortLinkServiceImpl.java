package com.nageoffer.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.common.constant.RedisKeyConstant;
import com.nageoffer.shortlink.project.common.convention.exception.ClientException;
import com.nageoffer.shortlink.project.common.enums.VailDateTypeEnum;
import com.nageoffer.shortlink.project.dao.entity.LinkAccessStatsDO;
import com.nageoffer.shortlink.project.dao.entity.LinkLocaleStatsDO;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.nageoffer.shortlink.project.dao.mapper.LinkAccessStatsMapper;
import com.nageoffer.shortlink.project.dao.mapper.LinkLocaleStatsMapper;
import com.nageoffer.shortlink.project.dao.mapper.ShortLinkMapper;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCountQueryRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.project.service.ShortLinkGotoService;
import com.nageoffer.shortlink.project.service.ShortLinkService;
import com.nageoffer.shortlink.project.util.HashUtil;
import com.nageoffer.shortlink.project.util.IpUtil;
import com.nageoffer.shortlink.project.util.LinkUtil;
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
    @Value("${short-link.stats.locale.amap-key}")
    private String key;
    @Override
    @SneakyThrows
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        String s = generateSuffix(requestParam);
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(requestParam.getDomain())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .delFlag(0)
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
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0)
                .orderByDesc(ShortLinkDO::getCreateTime); // DESC 降序
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each -> BeanUtil.toBean(each,ShortLinkPageRespDTO.class));
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
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO shortLink = baseMapper.selectOne(queryWrapper);
        if (shortLink == null){
            throw new ClientException("短链接记录不存在");
        }
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0)
                .set(ObjectUtil.equal(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .originUrl(requestParam.getOriginUrl())
                .describe(requestParam.getDescribe())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .build();
        baseMapper.update(shortLinkDO,updateWrapper);

    }
    // 短链接跳转
    @Override
    @SneakyThrows
    public void restoreUrl(String shortUri, HttpServletRequest request, HttpServletResponse response) {
        String serverName = request.getServerName();
        String scheme = request.getScheme();
        String fullShortUrl =scheme +"://"+ serverName + "/" + shortUri;
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
    private void shortLinkstats(String fullShortUrl,String gid,HttpServletRequest request,HttpServletResponse response){
        try {
            AtomicBoolean uvFlag = new AtomicBoolean(true);
            Cookie[] cookies = request.getCookies();
            if (ArrayUtil.isNotEmpty(cookies)){
                Arrays.stream(cookies)
                        .filter(each -> Objects.equals("uv",each.getName()))
                        .findFirst()
                        .map(Cookie::getValue)
                        .ifPresentOrElse(each -> {
                            Long add = stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, each);
                            uvFlag.set((add!=null && add >0));
                        },()->{
                            String uv = UUID.fastUUID().toString();
                            Cookie uvCookie = new Cookie("uv", uv);
                            uvCookie.setMaxAge(60*60*24*30);
                            uvCookie.setPath(StrUtil.sub(fullShortUrl,fullShortUrl.lastIndexOf("/"),fullShortUrl.length()));
                            response.addCookie(uvCookie);
                        });
            }else {
                String uv = UUID.fastUUID().toString();
                Cookie uvCookie = new Cookie("uv", uv);
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
            if (StrUtil.isNotBlank(infocode) && infocode.equals("10000")){
                String province = entries.get("province").toString();
                Boolean unknowFlag = (StrUtil.isNotBlank(province) && province.indexOf("[")==0);
                localeStatsDO = LinkLocaleStatsDO.builder()
                        .fullShortUrl(fullShortUrl)
                        .gid(gid)
                        .province(unknowFlag ? "未知" : province)
                        .city(unknowFlag ? "未知" : entries.get("city").toString())
                        .adcode(unknowFlag ? "未知" : entries.get("adcode").toString())
                        .country("中国")
                        .date(new Date())
                        .cnt(1)
                        .build();
            }
            linkLocaleStatsMapper.shortLinkLocaleState(localeStatsDO);
        }catch (Exception e){
            log.error("短链接监控统计出错",e);
        }

    }
}
