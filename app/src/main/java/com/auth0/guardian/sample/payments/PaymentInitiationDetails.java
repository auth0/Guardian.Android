package com.auth0.guardian.sample.payments;

import com.auth0.android.guardian.sdk.annotations.AuthorizationDetailsType;

@AuthorizationDetailsType("payment_initiation")
public class PaymentInitiationDetails {
    private final InstructedAmount instructedAmount;
    private final String creditorName;
    private final CreditorAccount creditorAccount;
    private final String remittanceInformation;

    public PaymentInitiationDetails(InstructedAmount instructedAmount, String creditorName, CreditorAccount creditorAccount, String remittanceInformation) {
        this.instructedAmount = instructedAmount;
        this.creditorName = creditorName;
        this.creditorAccount = creditorAccount;
        this.remittanceInformation = remittanceInformation;
    }

    public String getType() {
        return "payment_initiation";
    }

    public InstructedAmount getInstructedAmount() {
        return instructedAmount;
    }

    public String getCreditorName() {
        return creditorName;
    }

    public CreditorAccount getCreditorAccount() {
        return creditorAccount;
    }

    public String getRemittanceInformation() {
        return remittanceInformation;
    }
}
