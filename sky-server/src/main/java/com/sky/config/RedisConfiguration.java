package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建 RedisTemplate 对象...");
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置 Redis 的连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置 Redis key 的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        log.info("创建 RedisTemplate 对象成功！");
        return redisTemplate;
    }
}
