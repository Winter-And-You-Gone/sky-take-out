package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param orders 订单数据
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber 订单号
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders 订单对象
     */
    void update(Orders orders);

    /**
     * 订单分页查询，包含订单详情
     * @param ordersPageQueryDTO 订单分页查询条件
     * @return 订单分页数据
     */
    Page<OrderVO> pageQueryWithOrderDetail(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单信息
     * @param id 订单id
     * @return 订单详情
     */
    OrderVO getById(Long id);

    /**
     * 各个状态的订单数量统计
     * @return 订单数量统计结果
     */
    OrderStatisticsVO statistics();
}
