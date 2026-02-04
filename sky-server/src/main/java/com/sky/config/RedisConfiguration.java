package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
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

    /**
     * 项目启动时清空Redis缓存（只清空缓存相关的key，保留业务数据）
     */
    @Bean
    public CommandLineRunner clearRedisCacheOnStartup(RedisTemplate<String, Object> redisTemplate) {
        return args -> {
            try {
                log.info("项目启动，开始清空Redis缓存数据...");
                // 只清空以特定前缀开头的缓存key，保留业务数据如SHOP_STATUS
                String[] cachePatterns = {"dish*", "setmeal*", "category*"};
                
                for (String pattern : cachePatterns) {
                    redisTemplate.delete(redisTemplate.keys(pattern));
                    log.info("已清空缓存模式: {}", pattern);
                }
                
                log.info("Redis缓存清空完成！业务数据已保留。");
            } catch (Exception e) {
                log.error("清空Redis缓存失败: {}", e.getMessage());
            }
        };
    }
}