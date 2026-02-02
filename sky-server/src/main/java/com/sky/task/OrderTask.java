package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理支付超时订单
     */
    @Scheduled(cron = "0 * * * * *")
//    @Scheduled(cron = "0/5 * * * * ?")
    public void processTimeoutOrder() {
        log.info("处理支付超时订单：{}", LocalDateTime.now());

        List<Orders> ordersList = orderMapper.getTimeoutOrderByStatusAndOrderTimeLT(
                Orders.PENDING_PAYMENT,
                LocalDateTime.now().plusMinutes(-15));

        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单支付超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());

                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理派送超时订单
     */
    @Scheduled(cron = "0 0 4 * * ?")
//    @Scheduled(cron = "2/5 * * * * ?")
    public void processDeliveryTimeoutOrder() {
        log.info("处理派送超时订单：{}", LocalDateTime.now());
        List<Orders> ordersList = orderMapper.getTimeoutOrderByStatusAndOrderTimeLT(
                Orders.DELIVERY_IN_PROGRESS,
                LocalDateTime.now().plusHours(-4));
        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orders.setDeliveryTime(LocalDateTime.now());

                orderMapper.update(orders);
            }
        }
    }
}
