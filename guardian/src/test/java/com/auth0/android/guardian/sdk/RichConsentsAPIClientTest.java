package com.auth0.android.guardian.sdk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

import com.auth0.android.guardian.sdk.model.RichConsent;
import com.auth0.android.guardian.sdk.networking.Callback;
import com.auth0.android.guardian.sdk.networking.RequestFactory;
import com.auth0.android.guardian.sdk.utils.MockWebService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, manifest = Config.NONE)
public class RichConsentsAPIClientTest {
    private static final String CONSENT_ID = "cns_00000000001";
    private static final String SCOPE = "openid";
    private static final String AUDIENCE = "https://t3st.auth0.com/userinfo";
    private static final String BINDING_MESSAGE = "abc-123";
    // TODO: actual token
    private static final String TRANSACTION_TOKEN = "token";

    private MockWebService mockAPI;
    private RichConsentsAPIClient richConsentsAPIClient;

    @Mock
    Callback<RichConsent> fetchCallback;

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        mockAPI = new MockWebService();
        final String domain = mockAPI.getDomain();

        Gson gson = new GsonBuilder()
                .create();

        RequestFactory requestFactory = new RequestFactory(gson, new OkHttpClient());

        richConsentsAPIClient = new RichConsentsAPIClient(requestFactory, HttpUrl.parse(domain));
    }

    @Test
    public void shouldFetchRichConsent() throws Exception {
        mockAPI.willReturnRichConsent(CONSENT_ID, AUDIENCE, SCOPE, BINDING_MESSAGE);

        richConsentsAPIClient
                .fetch(CONSENT_ID, TRANSACTION_TOKEN)
                .start(fetchCallback);

        RecordedRequest request = mockAPI.takeRequest();

        assertThat(request.getPath(), is(equalTo(String.format("/rich-consents/%s", CONSENT_ID))));
        assertThat(request.getMethod(), is(equalTo("GET")));
        assertThat(request.getHeader("Authorization"), is(equalTo("MFA-DPoP " + TRANSACTION_TOKEN)));
//         TODO: calculate actual assertion
//        assertThat(request.getHeader("MFA-DPoP"), is(equalTo("MFA-DPoP " + "assertion")));

        ArgumentCaptor<RichConsent> onSuccessCaptor = ArgumentCaptor.forClass(RichConsent.class);
        verify(fetchCallback, timeout(100)).onSuccess(onSuccessCaptor.capture());

        RichConsent capturedRichConsent = onSuccessCaptor.getValue();
        assertThat(capturedRichConsent.id, is(equalTo(CONSENT_ID)));
        assertThat(capturedRichConsent.requested_details.audience, is(equalTo(AUDIENCE)));
        assertThat(capturedRichConsent.requested_details.scope, is(equalTo(SCOPE)));
        assertThat(capturedRichConsent.requested_details.binding_message, is(equalTo(BINDING_MESSAGE)));

        verifyNoMoreInteractions(fetchCallback);
        assertThat(true, is(equalTo(true)));
    }
}