package com.auth0.android.guardian.sdk.utils;

public class NotAnnotatedPaymentIntentTestingAuthorizationDetailsType {
    private final String type;
    private final int amount;

    public NotAnnotatedPaymentIntentTestingAuthorizationDetailsType(String type, int amount) {
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
