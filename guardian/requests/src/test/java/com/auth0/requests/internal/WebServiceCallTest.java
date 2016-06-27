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

package com.auth0.requests.internal;

import com.auth0.requests.Callback;
import com.auth0.requests.ParseErrorException;
import com.auth0.requests.Serializer;
import com.auth0.requests.ServerErrorException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.Executor;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Nicolas Ulrich (nikolaseu@gmail.com)
 */
public class WebServiceCallTest {

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    Serializer serializer;

    @Mock
    Serializer.Parser<Object> parser;

    @Mock
    OkHttpClient client;

    @Mock
    okhttp3.Call okhttpCall;

    @Mock
    Callback<Object> callback;

    @Captor
    ArgumentCaptor<Object> objectCaptor;

    @Captor
    ArgumentCaptor<Throwable> errorCaptor;

    @Captor
    ArgumentCaptor<okhttp3.Callback> callbackCaptor;

    WebServiceCall<Object> call;

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

    Executor executor = new DirectExecutor();

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        call = new WebServiceCall<>(executor, serializer, parser, okhttpCall);
    }

    @Test
    public void shouldReturnNullIfNoParserSetUpAndSuccessfulResponse() throws Exception {
        WebServiceCall<Object> call = new WebServiceCall<>(executor, serializer, null, okhttpCall);

        when(okhttpCall.execute())
                .thenReturn(successResponse);

        Object response = call.execute();
        assertThat(response, is(nullValue()));
    }

    @Test
    public void shouldParseSuccessfulResponse() throws Exception {
        when(okhttpCall.execute())
                .thenReturn(successResponse);

        Object parsedResponse = new Object();

        when(parser.parse(any(Reader.class)))
                .thenReturn(parsedResponse);

        Object response = call.execute();
        assertThat(response, is(sameInstance(parsedResponse)));

        verify(parser).parse(any(Reader.class));
    }

    @Test
    public void shouldParseErrorResponse() throws Exception {
        when(okhttpCall.execute())
                .thenReturn(errorResponse);

        ServerErrorException parsedErrorResponse = new ServerErrorException("Error", errorResponse.code());

        when(serializer.parseServerError(any(Reader.class), anyInt()))
                .thenReturn(parsedErrorResponse);

        ServerErrorException thrownException = null;
        try {
            call.execute();
        } catch (ServerErrorException error) {
            thrownException = error;
        }

        verify(serializer).parseServerError(any(Reader.class), anyInt());

        assertThat(thrownException, is(notNullValue()));
        assertThat(thrownException, is(sameInstance(parsedErrorResponse)));
    }

    @Test
    public void shouldFailParseWithSuccessResponse() throws Exception {
        thrown.expect(ParseErrorException.class);

        when(okhttpCall.execute())
                .thenReturn(successResponse);

        when(parser.parse(any(Reader.class)))
                .thenThrow(new RuntimeException());

        call.execute();

        verify(parser).parse(any(Reader.class));
    }

    @Test
    public void shouldFailParseWithErrorResponse() throws Exception {
        thrown.expect(ParseErrorException.class);

        when(okhttpCall.execute())
                .thenReturn(errorResponse);

        when(serializer.parseServerError(any(Reader.class), anyInt()))
                .thenThrow(new RuntimeException());

        call.execute();

        verify(serializer).parseServerError(any(Reader.class), anyInt());
    }

    @Test
    public void shouldReturnNullIfNoParserSetUpAndSuccessfulResponseAsync() throws Exception {
        WebServiceCall<Object> call = new WebServiceCall<>(executor, serializer, null, okhttpCall);

        call.start(callback);
        verify(okhttpCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue()
                .onResponse(okhttpCall, successResponse);

        verify(callback).onSuccess(objectCaptor.capture());
        Object response = objectCaptor.getValue();
        assertThat(response, is(nullValue()));
    }

    @Test
    public void shouldParseSuccessfulResponseAsync() throws Exception {
        Object parsedResponse = new Object();

        when(parser.parse(any(Reader.class)))
                .thenReturn(parsedResponse);

        call.start(callback);
        verify(okhttpCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue()
                .onResponse(okhttpCall, successResponse);

        verify(parser).parse(any(Reader.class));
        verify(callback).onSuccess(objectCaptor.capture());
        Object response = objectCaptor.getValue();
        assertThat(response, is(sameInstance(parsedResponse)));
    }

    @Test
    public void shouldParseErrorResponseAsync() throws Exception {
        ServerErrorException parsedErrorResponse = new ServerErrorException("Error", errorResponse.code());

        when(serializer.parseServerError(any(Reader.class), anyInt()))
                .thenReturn(parsedErrorResponse);

        call.start(callback);
        verify(okhttpCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue()
                .onResponse(okhttpCall, errorResponse);

        verify(serializer).parseServerError(any(Reader.class), anyInt());
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

        call.start(callback);
        verify(okhttpCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue()
                .onResponse(okhttpCall, successResponse);

        verify(parser).parse(any(Reader.class));
        verify(callback).onFailure(errorCaptor.capture());
        Throwable thrownException = errorCaptor.getValue();
        assertThat(thrownException, is(notNullValue()));
        assertThat(thrownException, is(instanceOf(ParseErrorException.class)));
    }

    @Test
    public void shouldFailParseWithErrorResponseAsync() throws Exception {
        when(serializer.parseServerError(any(Reader.class), anyInt()))
                .thenThrow(new RuntimeException());

        call.start(callback);
        verify(okhttpCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue()
                .onResponse(okhttpCall, errorResponse);

        verify(serializer).parseServerError(any(Reader.class), anyInt());
        verify(callback).onFailure(errorCaptor.capture());
        Throwable thrownException = errorCaptor.getValue();
        assertThat(thrownException, is(notNullValue()));
        assertThat(thrownException, is(instanceOf(ParseErrorException.class)));
    }

    @Test
    public void shouldFailWhenFailure() throws Exception {
        call.start(callback);

        verify(okhttpCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue()
                .onFailure(okhttpCall, new IOException(""));

        verify(callback).onFailure(errorCaptor.capture());
        Throwable thrownException = errorCaptor.getValue();
        assertThat(thrownException, is(notNullValue()));
        assertThat(thrownException, is(instanceOf(IOException.class)));
    }
}