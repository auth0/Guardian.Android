package com.auth0.guardian.sample.consent.authorization.details.payments;

import com.auth0.android.guardian.sdk.annotations.AuthorizationDetailsType;

import java.util.List;

@AuthorizationDetailsType("payment_initiation")
public class PaymentInitiationDetails {
    private final String type;
    private final List<String> actions;
    private final List<String> locations;
    private final InstructedAmount instructedAmount;
    private final String creditorName;
    private final CreditorAccount creditorAccount;
    private final String remittanceInformation;

    public PaymentInitiationDetails(String type, List<String> actions, List<String> locations, InstructedAmount instructedAmount, String creditorName, CreditorAccount creditorAccount, String remittanceInformation) {
        this.type = type;
        this.actions = actions;
        this.locations = locations;
        this.instructedAmount = instructedAmount;
        this.creditorName = creditorName;
        this.creditorAccount = creditorAccount;
        this.remittanceInformation = remittanceInformation;
    }

    public String getType() {
        return type;
    }

    public List<String> getActions() {
        return actions;
    }

    public List<String> getLocations() {
        return locations;
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
