package com.leyou.order.dto;

import com.leyou.common.dto.CartDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    @NonNull
    private Long addressId; //收货人地址
    @NonNull
    private Integer paymentType; //付款类型
    @NonNull
    private List<CartDTO> carts; //订单详情
}
