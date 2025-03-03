package com.auth0.android.guardian.sdk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.auth0.android.guardian.sdk.utils.NotAnnotatedPaymentIntentTestingAuthorizationDetailsType;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class GuardianRichConsentRequestedDetailsTest {
    GuardianRichConsentRequestedDetails subject;

    @Before
    public void beforeEach() {
        JsonObject authzDetailsType = new JsonObject();
        authzDetailsType.addProperty("type", "payment");
        authzDetailsType.addProperty("amount", "100.00");
        subject = new GuardianRichConsentRequestedDetails(
                "https://api.com",
                new String[]{"openid"},
                "binding_message",
                List.of(authzDetailsType)
        );
    }

    @Test
    public void shouldHaveCorrectData() {
        assertThat(subject.getAudience(), is(equalTo("https://api.com")));
        assertThat(subject.getScope()[0], is(equalTo("openid")));
        assertThat(subject.getBindingMessage(), is(equalTo("binding_message")));
        assertThat(subject.getAuthorizationDetails().isEmpty(), is(equalTo(false)));
        assertThat(subject.getAuthorizationDetails().get(0).get("type"), is(equalTo("payment")));
        assertThat(subject.getAuthorizationDetails().get(0).get("amount"), is(equalTo("100.00")));
    }

    @Test(expected = GuardianException.class)
    public void getAuthorizationDetailsShouldFailWhenUsedWithANonAnnotatedType() {
        subject.filterAuthorizationDetailsByType(NotAnnotatedPaymentIntentTestingAuthorizationDetailsType.class);
    }

}