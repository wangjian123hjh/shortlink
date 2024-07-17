package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.shortlink.project.dao.entity.LinkLocaleStatsDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LinkLocaleStatsMapper extends BaseMapper<LinkLocaleStatsDO> {
    @Insert("INSERT into t_link_locale_stats(full_short_url,gid,date,cnt,country,province,city,adcode,create_time,update_time,del_flag)VALUES(#{linkLocaleStats.fullShortUrl},#{linkLocaleStats.gid},#{linkLocaleStats.date},#{linkLocaleStats.cnt},#{linkLocaleStats.country},#{linkLocaleStats.province},#{linkLocaleStats.city},#{linkLocaleStats.adcode},NOW(),NOW(),0) on DUPLICATE key UPDATE cnt = cnt+#{linkLocaleStats.cnt};")
    void shortLinkLocaleState(@Param("linkLocaleStats")LinkLocaleStatsDO linkLocaleStats);
}
