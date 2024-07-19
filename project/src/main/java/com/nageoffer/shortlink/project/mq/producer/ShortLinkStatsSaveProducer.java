package com.nageoffer.shortlink.project.mq.producer;

import com.nageoffer.shortlink.project.common.constant.RedisKeyConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ShortLinkStatsSaveProducer {

    private final StringRedisTemplate stringRedisTemplate;

    public void send(Map<String,String> producerMap){
        stringRedisTemplate.opsForStream().add(RedisKeyConstant.SHORT_LINK_STATS_STREAM_TOPIC_KEY,producerMap);
    }
}
