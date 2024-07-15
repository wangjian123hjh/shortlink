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
        return DateUtil.between(new Date(),valiDate, DateUnit.SECOND);
    }
}
