package com.payflow.payflow_service.payment.provider;

import java.math.BigDecimal;

public  interface  PaymentProviderClient {
 
    
    String createPayment(
        BigDecimal amount,
        String currency,
        String internalPaymentId
    );
}
