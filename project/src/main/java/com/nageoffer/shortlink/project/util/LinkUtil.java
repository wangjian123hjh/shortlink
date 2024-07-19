package com.nageoffer.shortlink.project.util;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.nageoffer.shortlink.project.common.constant.ShortLinkConstant;

import java.net.URI;
import java.util.Date;

public class LinkUtil {
    public static long getLinkCacheValidDate(Date valiDate){
        if (valiDate == null){
            return ShortLinkConstant.DEFAULT_CACHE_VALID_TIME;
        }
        long l = DateUtil.between(new Date(), valiDate, DateUnit.SECOND);
        if (l>ShortLinkConstant.DEFAULT_CACHE_VALID_TIME)
            return ShortLinkConstant.DEFAULT_CACHE_VALID_TIME;
        return l;
    }

    /**
     * 获取原始链接中的域名
     * 如果原始链接包含 www 开头的话需要去掉
     *
     * @param url 创建或者修改短链接的原始链接
     * @return 原始链接中的域名
     */
    public static String extractDomain(String url) {
        String domain = null;
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (StrUtil.isNotBlank(host)) {
                domain = host;
                if (domain.startsWith("www.")) {
                    domain = host.substring(4);
                }
            }
        } catch (Exception ignored) {
        }
        return domain;
    }

}
