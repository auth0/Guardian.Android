/*
 * Copyright (c) 2016 Auth0 (http://auth0.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.auth0.android.guardian.networking.internal;

import com.auth0.android.guardian.networking.Callback;
import com.auth0.android.guardian.networking.ExecutableRequest;
import com.auth0.android.guardian.networking.ParseErrorException;
import com.auth0.android.guardian.networking.Serializer;
import com.auth0.android.guardian.networking.ServerErrorException;
import com.auth0.android.guardian.networking.ServerErrorParser;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.Executor;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 */
public class RequestTest {

    private static final String BASE_URL = "http://example.com";
    private static final Object BODY = new DummyBody();
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    Serializer serializer;

    @Mock
    ServerErrorParser serverErrorParser;

    @Mock
    Serializer.Parser<Object> parser;

    @Mock
    OkHttpClient client;

    @Mock
    Call call;

    @Mock
    Callback<Object> callback;

    @Captor
    ArgumentCaptor<okhttp3.Request> requestCaptor;

    @Captor
    ArgumentCaptor<Object> objectCaptor;

    @Captor
    ArgumentCaptor<Map<String, Object>> mapCaptor;

    @Captor
    ArgumentCaptor<Throwable> errorCaptor;

    @Captor
    ArgumentCaptor<okhttp3.Callback> callbackCaptor;

    Request<Object> request;

    Executor executor = new CurrentThreadExecutor();

    Response successResponse = new Response.Builder()
            .request(new okhttp3.Request.Builder()
                    .url("https://example.com/")
                    .build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .body(ResponseBody.create(MEDIA_TYPE, "{}"))
            .build();

    Response errorResponse = new Response.Builder()
            .request(new okhttp3.Request.Builder()
                    .url("https://example.com/")
                    .build())
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .body(ResponseBody.create(MEDIA_TYPE, "{}"))
            .build();

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(call.execute())
                .thenReturn(successResponse);

        when(serializer.serialize(any(Object.class)))
                .thenReturn("{}");

        when(client.newCall(any(okhttp3.Request.class)))
                .thenReturn(call);

        request = new Request<>(executor, serializer, parser, serverErrorParser, MEDIA_TYPE, client);
    }

    @Test
    public void shouldFailWithoutBaseUrl() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        request
                .get("http://example.com/something")
                .execute();
    }

    @Test
    public void shouldFailWithInvalidBaseUrl() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        request
                .baseUrl("invalidUrl")
                .get("something")
                .execute();
    }

    @Test
    public void testHttpBaseUrl() throws Exception {
        request
                .baseUrl("http://hello.example.com")
                .get("something")
                .execute();

        verify(client).newCall(requestCaptor.capture());
        okhttp3.Request request = requestCaptor.getValue();

        assertThat(request.url().toString(), is(equalTo("http://hello.example.com/something")));
    }

    @Test
    public void testHttpsBaseUrl() throws Exception {
        request
                .baseUrl("https://hello.example.com")
                .get("something")
                .execute();

        verify(client).newCall(requestCaptor.capture());
        okhttp3.Request request = requestCaptor.getValue();

        assertThat(request.url().toString(), is(equalTo("https://hello.example.com/something")));
    }

    @Test
    public void testGetPath() throws Exception {
        request
                .baseUrl(BASE_URL)
                .get("user/123")
                .execute();

        verify(client).newCall(requestCaptor.capture());
        okhttp3.Request request = requestCaptor.getValue();

        assertThat(request.method(), is(equalTo("GET")));
        assertThat(request.url().encodedPath(), is(equalTo("/user/123")));
    }

    @Test
    public void shouldFailGetWithBody() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        request
                .baseUrl(BASE_URL)
                .get("user/123")
                .addParameter("some", "parameter")
                .execute();
    }

    @Test
    public void testDeletePath() throws Exception {
        request
                .baseUrl(BASE_URL)
                .delete("user/123")
                .execute();

        verify(client).newCall(requestCaptor.capture());
        okhttp3.Request request = requestCaptor.getValue();

        assertThat(request.method(), is(equalTo("DELETE")));
        assertThat(request.url().encodedPath(), is(equalTo("/user/123")));
    }

    @Test
    public void shouldNotFailIfDeleteHasBody() throws Exception {
        request
                .baseUrl(BASE_URL)
                .delete("user/123")
                .addParameter("some", "parameter")
                .execute();

        verify(serializer).serialize(mapCaptor.capture());
        verifyNoMoreInteractions(serializer);
        Map<String, Object> body = mapCaptor.getValue();
        assertThat(body, hasEntry("some", (Object) "parameter"));

        verify(client).newCall(requestCaptor.capture());
        okhttp3.Request request = requestCaptor.getValue();

        assertThat(request.method(), is(equalTo("DELETE")));
        assertThat(request.url().encodedPath(), is(equalTo("/user/123")));
    }

    @Test
    public void testPatchPath() throws Exception {
        request
                .baseUrl(BASE_URL)
                .patch("user/123")
                .addParameter("parameter", "value")
                .execute();

        verify(client).newCall(requestCaptor.capture());
        okhttp3.Request request = requestCaptor.getValue();

        assertThat(request.method(), is(equalTo("PATCH")));
        assertThat(request.url().encodedPath(), is(equalTo("/user/123")));
    }

    @Test
    public void shouldFailPatchWithEmptyBody() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        request
                .baseUrl(BASE_URL)
                .patch("user/123")
                .execute();
    }

    @Test
    public void testPostPath() throws Exception {
        request
                .baseUrl(BASE_URL)
                .post("user/123")
                .addParameter("parameter", "value")
                .execute();

        verify(client).newCall(requestCaptor.capture());
        okhttp3.Request request = requestCaptor.getValue();

        assertThat(request.method(), is(equalTo("POST")));
        assertThat(request.url().encodedPath(), is(equalTo("/user/123")));
    }

    @Test
    public void shouldFailPostWithEmptyBody() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        request
                .baseUrl(BASE_URL)
                .post("user/123")
                .execute();
    }

    @Test
    public void testPutPath() throws Exception {
        request
                .baseUrl(BASE_URL)
                .put("user/123")
                .addParameter("parameter", "value")
                .execute();

        verify(client).newCall(requestCaptor.capture());
        okhttp3.Request request = requestCaptor.getValue();

        assertThat(request.method(), is(equalTo("PUT")));
        assertThat(request.url().encodedPath(), is(equalTo("/user/123")));
    }

    @Test
    public void shouldFailPutWithEmptyBody() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        request
                .baseUrl(BASE_URL)
                .put("user/123")
                .execute();
    }

    @Test
    public void shouldNotSerializeNothing() throws Exception {
        request
                .baseUrl(BASE_URL)
                .get("user/123")
                .execute();

        verifyNoMoreInteractions(serializer);

        verify(client).newCall(requestCaptor.capture());
        okhttp3.Request request = requestCaptor.getValue();

        assertThat(request.method(), is(equalTo("GET")));
        assertThat(request.url().encodedPath(), is(equalTo("/user/123")));
    }

    @Test
    public void shouldSerializeFromParameters() throws Exception {
        request
                .baseUrl(BASE_URL)
                .put("user/123")
                .addParameter("string", "value")
                .addParameter("number", 123)
                .addParameter("boolean", true)
                .execute();

        verify(serializer).serialize(mapCaptor.capture());
        verifyNoMoreInteractions(serializer);
        Map<String, Object> body = mapCaptor.getValue();
        assertThat(body, hasEntry("string", (Object) "value"));
        assertThat(body, hasEntry("number", (Object) 123));
        assertThat(body, hasEntry("boolean", (Object) true));

        verify(client).newCall(requestCaptor.capture());
        okhttp3.Request request = requestCaptor.getValue();

        assertThat(request.method(), is(equalTo("PUT")));
        assertThat(request.url().encodedPath(), is(equalTo("/user/123")));
    }

    @Test
    public void shouldSerializeFromBodyObject() throws Exception {
        request
                .baseUrl(BASE_URL)
                .put("user/123", BODY)
                .execute();

        verify(serializer).serialize(objectCaptor.capture());
        verifyNoMoreInteractions(serializer);
        Object body = objectCaptor.getValue();
        assertThat(body, is(instanceOf(DummyBody.class)));

        verify(client).newCall(requestCaptor.capture());
        okhttp3.Request request = requestCaptor.getValue();

        assertThat(request.method(), is(equalTo("PUT")));
        assertThat(request.url().encodedPath(), is(equalTo("/user/123")));
    }

    @Test
    public void shouldFailWhenAddingParametersAndAlreadyHadBody() throws Exception {
        thrown.expect(IllegalArgumentException.class);

        request
                .baseUrl(BASE_URL)
                .put("user/123", BODY)
                .addParameter("parameter", "value")
                .execute();
    }

    @Test
    public void shouldAddHeaders() throws Exception {
        request
                .baseUrl(BASE_URL)
                .get("user/123")
                .addHeader("header", "value")
                .addHeader("anotherHeader", "anotherValue")
                .execute();

        verify(client).newCall(requestCaptor.capture());
        okhttp3.Request request = requestCaptor.getValue();

        assertThat(request.header("header"), is(equalTo("value")));
        assertThat(request.header("anotherHeader"), is(equalTo("anotherValue")));
    }

    @Test
    public void shouldAddAuthorizationHeader() throws Exception {
        request
                .baseUrl(BASE_URL)
                .get("user/123")
                .setBearer("some_token")
                .execute();

        verify(client).newCall(requestCaptor.capture());
        okhttp3.Request request = requestCaptor.getValue();

        assertThat(request.header("Authorization"), is(equalTo("Bearer some_token")));
    }

    @Test
    public void shouldAddQueryParameter() throws Exception {
        request
                .baseUrl(BASE_URL)
                .get("user/123")
                .addQueryParameter("string", "value")
                .addQueryParameter("number", String.valueOf(123))
                .addQueryParameter("boolean", String.valueOf(true))
                .execute();

        verify(client).newCall(requestCaptor.capture());
        okhttp3.Request request = requestCaptor.getValue();

        assertThat(request.url().queryParameter("string"), is(equalTo("value")));
        assertThat(request.url().queryParameter("number"), is(equalTo("123")));
        assertThat(request.url().queryParameter("boolean"), is(equalTo("true")));
    }

    @Test
    public void shouldExecuteRealCall() throws Exception {
        request
                .baseUrl(BASE_URL)
                .get("user/123")
                .execute();

        verify(call).execute();
    }

    @Test
    public void shouldStartRealCall() throws Exception {
        request
                .baseUrl(BASE_URL)
                .get("user/123")
                .start(callback);

        verify(call).enqueue(any(okhttp3.Callback.class));
    }

    @Test
    public void shouldReturnNullIfNoParserSetUpAndSuccessfulResponse() throws Exception {
        when(call.execute())
                .thenReturn(successResponse);

        Object response = new Request<>(executor, serializer, null, serverErrorParser, MEDIA_TYPE, client)
                .baseUrl(BASE_URL)
                .get("something")
                .execute();

        assertThat(response, is(nullValue()));
    }

    @Test
    public void shouldParseSuccessfulResponse() throws Exception {
        when(call.execute())
                .thenReturn(successResponse);

        Object parsedResponse = new Object();

        when(parser.parse(any(Reader.class)))
                .thenReturn(parsedResponse);

        Object response = request
                .baseUrl(BASE_URL)
                .get("something")
                .execute();
        assertThat(response, is(sameInstance(parsedResponse)));

        verify(parser).parse(any(Reader.class));
    }

    @Test
    public void shouldParseErrorResponse() throws Exception {
        when(call.execute())
                .thenReturn(errorResponse);

        ServerErrorException parsedErrorResponse = new ServerErrorException("Error", errorResponse.code());

        when(serverErrorParser.parseServerError(any(Reader.class), anyInt()))
                .thenReturn(parsedErrorResponse);

        ServerErrorException thrownException = null;
        try {
            request
                    .baseUrl(BASE_URL)
                    .get("something")
                    .execute();
        } catch (ServerErrorException error) {
            thrownException = error;
        }

        verify(serverErrorParser).parseServerError(any(Reader.class), anyInt());

        assertThat(thrownException, is(notNullValue()));
        assertThat(thrownException, is(sameInstance(parsedErrorResponse)));
    }

    @Test
    public void shouldFailParseWithSuccessResponse() throws Exception {
        thrown.expect(ParseErrorException.class);

        when(call.execute())
                .thenReturn(successResponse);

        when(parser.parse(any(Reader.class)))
                .thenThrow(new RuntimeException());

        request
                .baseUrl(BASE_URL)
                .get("something")
                .execute();

        verify(parser).parse(any(Reader.class));
    }

    @Test
    public void shouldFailParseWithErrorResponse() throws Exception {
        thrown.expect(ParseErrorException.class);

        when(call.execute())
                .thenReturn(errorResponse);

        when(serverErrorParser.parseServerError(any(Reader.class), anyInt()))
                .thenThrow(new RuntimeException());

        request
                .baseUrl(BASE_URL)
                .get("something")
                .execute();

        verify(serverErrorParser).parseServerError(any(Reader.class), anyInt());
    }

    @Test
    public void shouldReturnNullIfNoParserSetUpAndSuccessfulResponseAsync() throws Exception {
        new Request<>(executor, serializer, null, serverErrorParser, MEDIA_TYPE, client)
                .baseUrl(BASE_URL)
                .get("something")
                .start(callback);

        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue()
                .onResponse(call, successResponse);

        verify(callback).onSuccess(objectCaptor.capture());
        Object response = objectCaptor.getValue();
        assertThat(response, is(nullValue()));
    }

    @Test
    public void shouldParseSuccessfulResponseAsync() throws Exception {
        Object parsedResponse = new Object();

        when(parser.parse(any(Reader.class)))
                .thenReturn(parsedResponse);

        request
                .baseUrl(BASE_URL)
                .get("something")
                .start(callback);

        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue()
                .onResponse(call, successResponse);

        verify(parser).parse(any(Reader.class));
        verify(callback).onSuccess(objectCaptor.capture());
        Object response = objectCaptor.getValue();
        assertThat(response, is(sameInstance(parsedResponse)));
    }

    @Test
    public void shouldParseErrorResponseAsync() throws Exception {
        ServerErrorException parsedErrorResponse = new ServerErrorException("Error", errorResponse.code());

        when(serverErrorParser.parseServerError(any(Reader.class), anyInt()))
                .thenReturn(parsedErrorResponse);

        request
                .baseUrl(BASE_URL)
                .get("something")
                .start(callback);
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue()
                .onResponse(call, errorResponse);

        verify(serverErrorParser).parseServerError(any(Reader.class), anyInt());
        verify(callback).onFailure(errorCaptor.capture());
        Throwable thrownException = errorCaptor.getValue();
        assertThat(thrownException, is(notNullValue()));
        assertThat(thrownException, is(instanceOf(ServerErrorException.class)));
        ServerErrorException errorResponse = (ServerErrorException) thrownException;
        assertThat(errorResponse, is(sameInstance(parsedErrorResponse)));
    }

    @Test
    public void shouldFailParseWithSuccessResponseAsync() throws Exception {
        when(parser.parse(any(Reader.class)))
                .thenThrow(new RuntimeException());

        request
                .baseUrl(BASE_URL)
                .get("something")
                .start(callback);
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue()
                .onResponse(call, successResponse);

        verify(parser).parse(any(Reader.class));
        verify(callback).onFailure(errorCaptor.capture());
        Throwable thrownException = errorCaptor.getValue();
        assertThat(thrownException, is(notNullValue()));
        assertThat(thrownException, is(instanceOf(ParseErrorException.class)));
    }

    @Test
    public void shouldFailParseWithErrorResponseAsync() throws Exception {
        when(serverErrorParser.parseServerError(any(Reader.class), anyInt()))
                .thenThrow(new RuntimeException());

        request
                .baseUrl(BASE_URL)
                .get("something")
                .start(callback);
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue()
                .onResponse(call, errorResponse);

        verify(serverErrorParser).parseServerError(any(Reader.class), anyInt());
        verify(callback).onFailure(errorCaptor.capture());
        Throwable thrownException = errorCaptor.getValue();
        assertThat(thrownException, is(notNullValue()));
        assertThat(thrownException, is(instanceOf(ParseErrorException.class)));
    }

    @Test
    public void shouldFailWhenFailure() throws Exception {
        IOException exception = new IOException("");

        request
                .baseUrl(BASE_URL)
                .get("something")
                .start(callback);

        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue()
                .onFailure(call, exception);

        verify(callback).onFailure(errorCaptor.capture());
        Throwable thrownException = errorCaptor.getValue();
        assertThat(thrownException, is(notNullValue()));
        assertThat(thrownException, is(instanceOf(IOException.class)));
        assertThat((IOException)thrownException, is(sameInstance(exception)));
    }

    static class DummyBody {
        String someString = "someString";
    }
}