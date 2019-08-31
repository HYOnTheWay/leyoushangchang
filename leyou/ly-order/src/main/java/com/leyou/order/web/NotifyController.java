package com.leyou.order.web;

import com.leyou.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("notify")
public class NotifyController {

    @Autowired
    private OrderService orderService;

    //@ResponseBody消息转换器是序列化过程 只能转json
    // @RequestBody只能把json转成指定对象 逆序列话转换
    // 但是导入xml转换器(jackson-dataformat-xml)  就能实现接收和转换为xml
    /**
     * 微信支付的成功回调
     * @param result
     * @return
     */
    @PostMapping(value = "pay",produces = "application/xml")
    public Map<String,String> hello(@RequestBody Map<String,String> result){
        //处理回调
        orderService.handleNotify(result);

        log.info("[支付回调] 结果:{}",result);
        //返回结果,成功
        Map<String,String> msg = new HashMap<>();
        msg.put("return_code","SUCCESS");
        msg.put("return_msg","OK");
        return msg;
    }
}
