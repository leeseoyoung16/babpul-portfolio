package com.bappul.orderservice.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoApproveResponse {
    private String aid;
    private String tid;
    private String partner_order_id;
    private String partner_user_id;
    private Amount amount;
    private String approved_at;

    @Getter
    @Setter
    public static class Amount {
        private int total;
    }
}