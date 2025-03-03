package com.auth0.guardian.sample.consent.authorization.details.payments;

public class InstructedAmount {
    private final String currency;
    private final String amount;

    public InstructedAmount(String currency, String amount) {
        this.currency = currency;
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getAmount() {
        return amount;
    }
}
