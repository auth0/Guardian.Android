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

package com.auth0.android.guardian.networking.gson;

import com.auth0.android.guardian.networking.Serializer;
import com.auth0.android.guardian.networking.ServerErrorException;
import com.auth0.android.guardian.networking.ServerErrorParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class GsonSerializerTest {

    @Mock
    Reader reader;

    @Mock
    ServerErrorParser serverErrorParser;

    @Mock
    ServerErrorException serverErrorException;

    Gson gson;

    GsonSerializer serializer;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        gson = new GsonBuilder().create();

        serializer = new GsonSerializer(gson, serverErrorParser);
    }

    @Test
    public void shouldCallServerErrorParser() throws Exception {
        when(serverErrorParser.parseServerError(any(Reader.class), anyInt()))
                .thenReturn(serverErrorException);

        ServerErrorException error = serializer.parseServerError(reader, 123);
        verify(serverErrorParser).parseServerError(reader, 123);

        assertThat(error, is(sameInstance(serverErrorException)));
    }

    @Test
    public void shouldSerializeObject() throws Exception {
        String serialized = serializer.serialize(new DummyObject());
        assertThat(serialized, is(equalTo("{\"someString\":\"theString\",\"someInteger\":456}")));
    }

    @Test
    public void shouldCreateAndParseFromClass() throws Exception {
        Serializer.Parser<DummyObject> parser = serializer.createParserFor(DummyObject.class);
        DummyObject parsed = parser.parse(new StringReader("{\"someString\":\"theStringValue\",\"someInteger\":123}"));
        assertThat(parsed.someString, is(equalTo("theStringValue")));
        assertThat(parsed.someInteger, is(equalTo(123)));
    }

    @Test
    public void shouldCreateAndParseFromType() throws Exception {
        Type type = new TypeToken<Map<String,Object>>() {
        }.getType();
        Serializer.Parser<Map<String,Object>> parser = serializer.createParserFor(type);
        Map<String,Object> parsed = parser.parse(new StringReader("{\"someString\":\"theStringValue\",\"someNumber\":123.3}"));
        assertThat(parsed, hasEntry("someString", (Object)"theStringValue"));
        assertThat(parsed, hasEntry("someNumber", (Object)123.3));
    }

    class DummyObject {
        String someString = "theString";
        Integer someInteger = 456;
    }
}