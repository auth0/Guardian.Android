package com.auth0.android.guardian.sdk;

import static com.auth0.android.guardian.sdk.oauth2.OAuth2AccessToken.getTokenHash;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

import com.auth0.android.guardian.sdk.networking.Callback;
import com.auth0.android.guardian.sdk.networking.RequestFactory;
import com.auth0.android.guardian.sdk.utils.MockWebService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAKey;

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
    @Mock
    Callback<RichConsent> fetchCallback;
    private MockWebService mockAPI;
    private KeyPair keyPair;
    private Gson gson;
    private RichConsentsAPIClient richConsentsAPIClient;

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();

        mockAPI = new MockWebService();
        final String domain = mockAPI.getDomain();

        Gson gson = new GsonBuilder()
                .create();

        RequestFactory requestFactory = new RequestFactory(gson, new OkHttpClient());

        richConsentsAPIClient = new RichConsentsAPIClient(requestFactory,
                HttpUrl.parse(domain),
                keyPair.getPrivate(),
                keyPair.getPublic());
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

        String dpopAssertion = request.getHeader("MFA-DPoP");
        assertThat(dpopAssertion, is(notNullValue()));
        verifyDPoPAssertion(dpopAssertion);

        ArgumentCaptor<RichConsent> onSuccessCaptor = ArgumentCaptor.forClass(GuardianRichConsent.class);
        verify(fetchCallback, timeout(100)).onSuccess(onSuccessCaptor.capture());

        RichConsent capturedRichConsent = onSuccessCaptor.getValue();
        assertThat(capturedRichConsent.getId(), is(equalTo(CONSENT_ID)));
        assertThat(capturedRichConsent.getRequestedDetails().getAudience(), is(equalTo(AUDIENCE)));
        assertThat(capturedRichConsent.getRequestedDetails().getScope()[0], is(equalTo(SCOPE)));
        assertThat(capturedRichConsent.getRequestedDetails().getBindingMessage(), is(equalTo(BINDING_MESSAGE)));

        verifyNoMoreInteractions(fetchCallback);
        assertThat(true, is(equalTo(true)));
    }

    private void verifyDPoPAssertion(String assertion) {
        Algorithm algorithm = Algorithm.RSA256((RSAKey) keyPair.getPublic());
        HttpUrl htu = HttpUrl.parse(mockAPI.getDomain())
                .newBuilder()
                .addPathSegment("rich-consents")
                .addPathSegments(CONSENT_ID)
                .build();

        JWT.require(algorithm)
                .withClaim("htu", htu.toString())
                .withClaim("htm", "GET")
                .withClaim("ath", getTokenHash(TRANSACTION_TOKEN))
                .build()
                .verify(assertion);
    }
}