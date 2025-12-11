package com.settlement.job.dto;

import com.settlement.common.model.payment.PaymentResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SettlementSourceDto {
    private String userId;
    private PaymentResponse payment;
}
