package com.leyou.order.service;

import com.leyou.common.dto.CartDTO;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.IdWorker;
import com.leyou.item.pojo.Sku;
import com.leyou.order.client.AddressClient;
import com.leyou.order.client.GoodsClient;
import com.leyou.order.dto.AddressDTO;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.interceptor.UserInterceptor;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.order.utils.PayHelper;
import com.leyou.pojo.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper detailMapper;

    @Autowired
    private OrderStatusMapper statusMapper;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private PayHelper payHelper;

    @Transactional
    public Long createOrder(OrderDTO orderDTO) {
        //1.新增订单
        Order order = new Order();
        //1.1订单编号,基本信息
        long orderId = idWorker.nextId();
        order.setOrderId(orderId);
        order.setCreateTime(new Date());
        order.setPaymentType(orderDTO.getPaymentType());

        //1.2用户信息
        UserInfo user = UserInterceptor.getUser();
        order.setUserId(user.getId());
        order.setBuyerNick(user.getUsername());
        order.setBuyerRate(false);

        //1.3收货人地址
        AddressDTO address = AddressClient.findById(orderDTO.getAddressId());
        order.setReceiver(address.getName());
        order.setReceiverAddress(address.getAddress());
        order.setReceiverCity(address.getCity());
        order.setReceiverDistrict(address.getDistrict());
        order.setReceiverMobile(address.getPhone());
        order.setReceiverState(address.getState());
        order.setReceiverZip(address.getZipCode());

        //1.4金额
        //付款金额相关，首先把orderDto转化成map，其中key为skuId,值为购物车中该sku的购买数量
        Map<Long, Integer> numMap = orderDTO.getCarts().stream().collect(
                Collectors.toMap(CartDTO::getSkuId, CartDTO::getNum));
        Set<Long> ids = numMap.keySet();
        List<Sku> skus = goodsClient.querySkuByByIds(new ArrayList<>(ids));
        //准备orderDetail集合
        List<OrderDetail> details = new ArrayList<>();

        long totalPay = 0L;
        for (Sku sku : skus) {
            //总价
            totalPay += sku.getPrice() * numMap.get(sku.getId());
            //封装orderDetail
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setOwnSpec(sku.getOwnSpec());
            orderDetail.setSkuId(sku.getId());
            orderDetail.setTitle(sku.getTitle());
            orderDetail.setNum(numMap.get(sku.getId()));
            orderDetail.setPrice(sku.getPrice());
            orderDetail.setImage(StringUtils.substringBefore(sku.getImages(), ","));
            details.add(orderDetail);

        }
        order.setTotalPay(totalPay);
        //总+邮费-优惠
        order.setActualPay(totalPay + order.getPostFee() - 0);
        //1.5写入数据库
        int count = orderMapper.insertSelective(order);
        if (count != 1){
            log.error("[创建订单] 创建订单失败,orderId:{}",orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }

        //2新增订单的详情
        count = detailMapper.insertList(details);
        if (count != 1){
            log.error("[创建订单] 创建订单失败,orderId:{}",orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }
        //3新增订单的状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setCreateTime(order.getCreateTime());
        orderStatus.setOrderId(orderId);
        orderStatus.setStatus(OrderStatusEnum.UN_PAY.value());
        count = statusMapper.insertSelective(orderStatus);
        if (count != 1){
            log.error("[创建订单] 创建订单失败,orderId:{}",orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }
        //4减库存
        List<CartDTO> cartDTS = orderDTO.getCarts();
        goodsClient.decreaseStock(cartDTS);
        return orderId;
    }

    public Order queryOrderById(Long orderId) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null){
            //不存在
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        //查询订单详情
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(orderId);
        List<OrderDetail> details = detailMapper.select(detail);
        if (CollectionUtils.isEmpty(details)){
            throw new LyException(ExceptionEnum.ORDER_DETAIL_NOT_FOUND);
        }
        //查询订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        if (orderStatus == null){
            //不存在
            throw new LyException(ExceptionEnum.STATUS_NOT_FOUNF);
        }
        return order;
    }

    public String createPayUrl(Long orderId) {
        //查询订单
        Order order = queryOrderById(orderId);
        //判断订单的状态
        Integer status = order.getOrderStatus().getStatus();
        if (status != OrderStatusEnum.UN_PAY.value()){
            //订单状态异常
            throw new LyException(ExceptionEnum.ORDER_STATUS_ERROR);
        }
        //支付金额
        Long actualPay = /*order.getActualPay()*/ 1L;
        //商品描述
        OrderDetail detail = order.getOrderDetails().get(0);
        String desc = detail.getTitle();
        return payHelper.createOrder(orderId,actualPay,desc);
    }

    public void handleNotify(Map<String, String> result) {
        //数据校验
        payHelper.isSuccess(result);
        //校验签名
        payHelper.isSignValid(result);
        //校验金额
        String totalFeeStr = result.get("total_fee");
        String tradeNo = result.get("out_trade_no");
        if (StringUtils.isEmpty(totalFeeStr) || StringUtils.isEmpty(tradeNo)){
            throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
        }
        //获取结果中的金额
        Long totalFee = Long.valueOf(totalFeeStr);
        //获取订单金额
        Long orderId = Long.valueOf(tradeNo);
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (totalFee != order.getActualPay()){
            //金额不符
            throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
        }
        //修改点订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setStatus(OrderStatusEnum.PAYED.value());
        orderStatus.setOrderId(orderId);
        orderStatus.setCreateTime(new Date());
        int count = statusMapper.updateByPrimaryKeySelective(orderStatus);
        if (count != 1){
            throw new LyException(ExceptionEnum.UPDATE_ORDER_STATUS_ERROR);
        }

        log.info("[支付回调] 订单支付成功,编号:{}",orderId);
    }

    public PayState queryOrderState(Long orderId) {
        //查询订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        Integer status = orderStatus.getStatus();
        if (status != OrderStatusEnum.UN_PAY.value()){
            //已支付
            return PayState.SUCCESS;
        }
        //未支付,有可能回调函数未执行
        return payHelper.queryPayState(orderId);
    }
}
