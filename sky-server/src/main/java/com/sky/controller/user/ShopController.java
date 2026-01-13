package com.sky.controller.user;

import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Slf4j
public class ShopController {
    private static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取营业状态
     *
     * @return
     */
    @GetMapping("/status")
    public Result<Integer> getStatus() {
        log.info("获取营业状态");
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("获取到营业状态为：{}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
