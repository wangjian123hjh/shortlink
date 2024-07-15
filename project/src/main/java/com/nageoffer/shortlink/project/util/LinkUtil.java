package com.nageoffer.shortlink.project.util;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.nageoffer.shortlink.project.common.constant.ShortLinkConstant;

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
}
