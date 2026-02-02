package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        // 处理各种业务异常（地址为空、购物车数据为空等）
        // 查询当前用户的地址数据
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());

        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 查询当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 向订单表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(userId);
        orders.setAddress(addressBook.getDetail());
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());

        orderMapper.insert(orders);

        // 向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());// 设置当前订单明细关联的订单id
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        // 清空当前用户的购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        // 封装VO返回数据

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );

        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        // 通过WebSocket向客户端推送订单状态
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1); // 1表示来单提醒，2表示客户催单
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号" + outTradeNo);

        webSocketServer.sendToAllClient(JSONObject.toJSONString(map));
    }

    /**
     * 历史订单查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<OrderVO> page = orderMapper.pageQueryWithOrderDetail(ordersPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO getDetailById(Long id) {
        return orderMapper.getById(id);
    }

    /**
     * 用户取消订单
     *
     * @param id
     */
    @Override
    public void userCancel(Long id) {
        // 只有待支付和待接单状态的订单才允许用户取消订单
        Orders ordersDB = orderMapper.getById(id);
        if (!ordersDB.getStatus().equals(Orders.PENDING_PAYMENT) && !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (Objects.equals(payStatus, Orders.PAID)) {
            try {
//                String refund = weChatPayUtil.refund(
//                        ordersDB.getNumber(),
//                        ordersDB.getNumber(),
//                        new BigDecimal(0.01),
//                        new BigDecimal(0.01)
//                )
                String refund = "退款成功";
                log.info("申请退款：{}", refund);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED)
                .cancelReason("用户取消订单")
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     *
     * @param id
     */
    @Override
    @Transactional
    public void repetition(Long id) {
        // 查询当前订单的详细信息
        OrderVO orderVO = orderMapper.getById(id);
        // 查询当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList != null && !shoppingCartList.isEmpty()) {
            // 删除当前用户购物车数据
            shoppingCartMapper.deleteByUserId(userId);
        }
        // 添加购物车数据
        for (OrderDetail orderDetail : orderVO.getOrderDetailList()) {
            ShoppingCart cart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, cart);
            cart.setUserId(userId);
            cart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(cart);
        }
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        return orderMapper.statistics();
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO 接单参数
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        // 只有订单存在且状态为待接单时才可以接单
        OrderVO orderVO = orderMapper.getById(ordersConfirmDTO.getId());
        if (orderVO == null || !orderVO.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO 拒单参数
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        // 只有订单存在且状态为待接单时才可以拒单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (Objects.equals(payStatus, Orders.PAID)) {
            try {
//                String refund = weChatPayUtil.refund(
//                        ordersDB.getNumber(),
//                        ordersDB.getNumber(),
//                        new BigDecimal(0.01),
//                        new BigDecimal(0.01)
//                )
                String refund = "退款成功";
                log.info("申请退款：{}", refund);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 管理员取消订单
     *
     * @param ordersCancelDTO
     */
    @Override
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) {
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        // 支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (Objects.equals(payStatus, Orders.PAID)) {
            try {
//                String refund = weChatPayUtil.refund(
//                        ordersDB.getNumber(),
//                        ordersDB.getNumber(),
//                        new BigDecimal(0.01),
//                        new BigDecimal(0.01)
//                )
                String refund = "退款成功";
                log.info("申请退款：{}", refund);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 订单派送
     *
     * @param id
     */
    @Override
    public void delivery(Long id) {
        // 只有订单存在且状态为待派送时才可以派送
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    @Override
    public void complete(Long id) {
        // 只有订单存在且状态为派送中才可以完成
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 客户催单
     *
     * @param id
     */
    @Override
    public void reminder(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 判断订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type", 2); // 1表示来单提醒，2表示客户催单
        map.put("orderId", id);
        map.put("content", "订单号：" + ordersDB.getNumber());
        webSocketServer.sendToAllClient(JSONObject.toJSONString(map));
    }
}
