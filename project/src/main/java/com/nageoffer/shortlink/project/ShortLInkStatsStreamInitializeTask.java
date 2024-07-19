package com.nageoffer.shortlink.project;

import com.nageoffer.shortlink.project.common.constant.RedisKeyConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShortLInkStatsStreamInitializeTask implements InitializingBean {
    private final StringRedisTemplate stringRedisTemplate;
    @Override
    public void afterPropertiesSet() throws Exception {
        Boolean hasKey = stringRedisTemplate.hasKey(RedisKeyConstant.SHORT_LINK_STATS_STREAM_TOPIC_KEY);
        if (hasKey == null || !hasKey){
            stringRedisTemplate.opsForStream().createGroup(RedisKeyConstant.SHORT_LINK_STATS_STREAM_TOPIC_KEY,RedisKeyConstant.SHORT_LINK_STATS_STREAM_GROUP_KEY);
        }
    }
}
