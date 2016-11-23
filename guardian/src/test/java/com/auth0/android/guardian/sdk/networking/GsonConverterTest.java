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

package com.auth0.android.guardian.sdk.networking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertThat;

public class GsonConverterTest {

    GsonConverter converter;

    @Before
    public void setUp() throws Exception {
        Gson gson = new GsonBuilder().create();
        converter = new GsonConverter(gson);
    }

    @Test
    public void shouldSerializeObject() throws Exception {
        String serialized = converter.serialize(new DummyObject());
        assertThat(serialized, is(equalTo("{\"someString\":\"theString\",\"someInteger\":456}")));
    }

    @Test
    public void shouldSerializeMap() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("someString", "theString");
        map.put("someInteger", 456);
        String serialized = converter.serialize(map);
        assertThat(serialized, containsString("\"someString\":\"theString\""));
        assertThat(serialized, containsString("\"someInteger\":456"));
    }

    @Test
    public void shouldParseClass() throws Exception {
        Type type = new TypeToken<DummyObject>() {}.getType();
        DummyObject parsed = converter.parse(type, new StringReader("{\"someString\":\"theStringValue\",\"someInteger\":123}"));
        assertThat(parsed.someString, is(equalTo("theStringValue")));
        assertThat(parsed.someInteger, is(equalTo(123)));
    }

    @Test
    public void shouldParseMap() throws Exception {
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String,Object> parsed = converter.parse(type, new StringReader("{\"someString\":\"theStringValue\",\"someNumber\":123.3}"));
        assertThat(parsed, hasEntry("someString", (Object)"theStringValue"));
        assertThat(parsed, hasEntry("someNumber", (Object)123.3));
    }

    class DummyObject {
        String someString = "theString";
        Integer someInteger = 456;
    }
}