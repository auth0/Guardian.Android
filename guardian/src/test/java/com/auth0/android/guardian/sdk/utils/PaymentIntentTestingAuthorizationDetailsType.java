package com.auth0.android.guardian.sdk.utils;

import com.auth0.android.guardian.sdk.annotations.AuthorizationDetailsType;

@AuthorizationDetailsType("payment-intent")
public class PaymentIntentTestingAuthorizationDetailsType {
    private final String type;
    private final int amount;

    public PaymentIntentTestingAuthorizationDetailsType(String type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public String getType() {
       return type;
    }

    public int getAmount() {
        return amount;
    }
}
