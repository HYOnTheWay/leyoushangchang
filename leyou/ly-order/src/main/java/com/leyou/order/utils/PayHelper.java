package com.leyou.order.utils;

import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.order.config.PayConfig;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class PayHelper {

    @Autowired
    private WXPay wxPay;

    @Autowired
    private PayConfig payConfig;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderStatusMapper statusMapper;


    public String createOrder(Long orderId, Long totalPay, String description) {

//        //从缓存中取出支付连接
//        String key = "order:pay:url:" + orderId;
//        try {
//            String url = redisTemplate.opsForValue().get(key);
//            if (StringUtils.isNotBlank(url)) {
//                return url;
//            }
//        } catch (Exception e) {
//            log.error("查询缓存付款链接异常，订单号：{}", orderId, e);
//        }

        try {
            Map<String, String> data = new HashMap<>();
            //描述
            data.put("body", description);
            //订单号
            data.put("out_trade_no", orderId.toString());
            //货币（默认就是人民币）
            //data.put("fee_type", "CNY");
            //总金额
            data.put("total_fee", totalPay.toString());
            //调用微信支付的终端ip
            data.put("spbill_create_ip", "127.0.0.1");
            //回调地址
            data.put("notify_url",payConfig.getNotifyUrl() );
            //交易类型为扫码支付
            data.put("trade_type", "NATIVE");
            //利用wxpay工具,完成下单
            Map<String, String> result = wxPay.unifiedOrder(data);
            //判断标识
            isSuccess(result);
            //下单成功，获取支付连接
            return result.get("code_url");
        } catch (Exception e) {
            log.error("【微信下单】创建预交易订单异常", e);
            return null;
        }
    }

    public void isSuccess(Map<String, String> result) {
        //通信失败提示
        String returnCode = result.get("return_code");
        //通信失败
        if (WXPayConstants.FAIL.equals(returnCode)) {
            log.error("【微信下单】与微信通信失败，失败信息：{}", result.get("return_msg"));
            throw new LyException(ExceptionEnum.WX_PAY_ORDER_FAIL);
        }
        //错误业务提示
        String resultCode = result.get("result_code");
        //下单失败
        if (WXPayConstants.FAIL.equals(resultCode)) {
            log.error("【微信下单】创建预交易订单失败，错误码：{}，错误信息：{}",
                    result.get("err_code"), result.get("err_code_des"));
            throw new LyException(ExceptionEnum.WX_PAY_ORDER_FAIL);
        }
    }

    /**
     * 检验签名
     * @param data
     */
    public void isSignValid(Map<String, String> data) {
        try {
            //重新生成签名
            String sign1 = WXPayUtil.generateSignature(data, payConfig.getKey(), WXPayConstants.SignType.HMACSHA256);
            String sign2 = WXPayUtil.generateSignature(data, payConfig.getKey(), WXPayConstants.SignType.MD5);

            //和传过来的作比较
            String sign = data.get("sign");
            if (!StringUtils.equals(sign,sign1) || !StringUtils.equals(sign,sign2)) {
                //签名有误
                throw new LyException(ExceptionEnum.INVALID_SIGN_ERROR);
            }
        } catch (Exception e) {
            log.error("【微信支付】检验签名失败，数据：{}", data);
            throw new LyException(ExceptionEnum.INVALID_SIGN_ERROR);
        }
    }

    /**
     * 查询订单支付状态
     * @param orderId
     * @return
     */
    public PayState queryPayState(Long orderId) {
        try {
            //组织请求参数
            Map<String, String> data = new HashMap<>();
            //订单号
            data.put("out_trade_no", orderId.toString());
            //查询状态
            Map<String, String> result = wxPay.orderQuery(data);
            //校验通信状态
            isSuccess(result);
            //校验签名
            isSignValid(result);
            //校验金额
            //校验金额
            String totalFeeStr = result.get("total_fee");
            String tradeNo = result.get("out_trade_no");
            if (StringUtils.isEmpty(totalFeeStr) || StringUtils.isEmpty(tradeNo)){
                throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
            }
            //获取结果中的金额
            Long totalFee = Long.valueOf(totalFeeStr);
            //获取订单金额
            Order order = orderMapper.selectByPrimaryKey(orderId);
            if (totalFee != order.getActualPay()){
                //金额不符
                throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
            }
            //查询支付状态
            String state = result.get("trade_state");
            if (WXPayConstants.SUCCESS.equals(state)) {
                //支付成功,修改点订单状态
                OrderStatus orderStatus = new OrderStatus();
                orderStatus.setStatus(OrderStatusEnum.PAYED.value());
                orderStatus.setOrderId(orderId);
                orderStatus.setCreateTime(new Date());
                int count = statusMapper.updateByPrimaryKeySelective(orderStatus);
                if (count != 1){
                    throw new LyException(ExceptionEnum.UPDATE_ORDER_STATUS_ERROR);
                }
                return PayState.SUCCESS;
            }
            if ("USERPAYING".equals(state) || "NOTPAY".equals(state)) {
                //未支付成功
                return PayState.NOT_PAY;
            }
            //其他返回付款失败
            return PayState.FAIL;

        }catch (Exception e){
            return PayState.NOT_PAY;
        }
    }
}