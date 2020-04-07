package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

/**
 * @author zr
 * @create 2020-04-01 下午 16:44
 */
@Service
public class AlipayServiceImpl implements AlipayService {

    // 支付
    @Autowired
    private PaymentService paymentService;

    // 调用订单服务
    @Autowired
    private OrderFeignClient orderFeignClient;

    // 引入 AlipayClient
    @Autowired
    private AlipayClient alipayClient;

    @Override
    public String careteAliPay(Long orderId) throws AlipayApiException {
        // 通过orderId查询到当前的订单
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        // 在生成二维码的同时，将数据保存到paymentInfo中
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());

        // 生成二维码
        // AlipayClient alipayClient =  new DefaultAlipayClient( "https://openapi.alipay.com/gateway.do" , APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);  //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        // 设置同步回调的url
        // https://www.domain.com/CallBack/return_url?
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 设置异步回调的url
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url); // 在公共参数中设置回跳和通知地址
        // 将数据封装到setBizContent() 生成二维码 金额，标题等 传递的是Json 数据
        // 将业务参数存入map集合中，将map转化为json字符串
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");
        map.put("total_amount", orderInfo.getTotalAmount());
        map.put("subject", "test-AliPay");
        alipayRequest.setBizContent(JSON.toJSONString(map));

        return alipayClient.pageExecute(alipayRequest).getBody(); // 调用sdk生成表单
    }

    // 退款接口
    @Override
    public boolean refund(Long orderId) {
        // 通过订单Id查询当前订单信息
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        map.put("refund_amount", orderInfo.getTotalAmount());
        map.put("refund_reason", "不想要了！");
        // 支持部分退款
        // map.put("out_request_no","HZ01RF001");
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            String msg = response.getMsg();
            if ("success".equals(msg)) {
                // 已退款，交易记录关闭
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
                paymentService.updatePaymentInfo(orderInfo.getOutTradeNo(), paymentInfo);

                return true;
            }
        } else {
            System.out.println("调用失败");
            return false;
        }
        return false;
    }

    // 关闭交易
    @SneakyThrows
    @Override
    public boolean closePay(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        // PaymentInfo paymentInfo = paymentService.getPaymentInfo(orderInfo.getOutTradeNo(), PaymentType.ALIPAY.name());
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        HashMap<String, Object> map = new HashMap<>();
        // map.put("trade_no",paymentInfo.getTradeNo()); // 必须从paymentInfo中获取
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("operator_id","YX01");
        request.setBizContent(JSON.toJSONString(map));
//        request.setBizContent("{" +
//                "\"trade_no\":\"2013112611001004680073956707\"," +
//                "\"out_trade_no\":\"HZ0120131127001\"," +
//                "\"operator_id\":\"YX01\"" +
//                "  }");
        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    // 查看是否有交易记录
    @SneakyThrows
    @Override
    public boolean checkPayment(Long orderId) {
        // 根据订单Id查询订单信息
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        // 根据out_trade_no查询交易记录
        request.setBizContent(JSON.toJSONString(map));
//        request.setBizContent("{" +
//                "\"out_trade_no\":\"20150320010101001\"," +
//                "\"trade_no\":\"2014112611001004680 073956707\"," +
//                "\"org_pid\":\"2088101117952222\"," +
//                "      \"query_options\":[" +
//                "        \"TRADE_SETTLE_INFO\"" +
//                "      ]" +
//                "  }");
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }
}
