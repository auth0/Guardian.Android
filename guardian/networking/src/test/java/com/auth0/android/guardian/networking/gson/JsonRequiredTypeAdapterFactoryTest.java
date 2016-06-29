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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class JsonRequiredTypeAdapterFactoryTest {

    Gson gson;

    @Before
    public void setUp() throws Exception {
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new JsonRequiredTypeAdapterFactory())
                .create();
    }

    @Test
    public void shouldFailBecauseMissingRequiredField() throws Exception {
        ClassWithRequiredField c = null;
        JsonParseException t = null;
        try {
            c = gson.fromJson(
                    "{ \"field\": \"value\" }",
                    ClassWithRequiredField.class);
        } catch (JsonParseException e) {
            t = e;
        }

        assertNull("Should fail to parse JSON", c);
        assertNotNull("Should throw JsonParseException", t);
    }

    @Test
    public void shouldNotFailWhenRequiredFieldIsPresent() throws Exception {
        ClassWithRequiredField c = gson.fromJson(
                    "{ \"requiredField\": \"value\" }",
                    ClassWithRequiredField.class);

        assertNotNull("Should successfully parse JSON", c);
        assertEquals("Should have 'requiredField' = 'value'", c.requiredField, "value");
    }

    @Test
    public void shouldNotFailWhenMissingFieldIsNotRequired() throws Exception {
        ClassWithoutRequiredField bean = null;
        Exception error = null;
        try {
            bean = gson.fromJson("{}", ClassWithoutRequiredField.class);
        } catch (Exception e) {
            error = e;
        }
        assertNull("Should not throw exception", error);
        assertNotNull("Should deserialize object", bean);
        assertNull("Should have null optionalField", bean.optionalField);
    }

    static class ClassWithRequiredField {

        @JsonRequired
        private String requiredField;
    }

    static class ClassWithoutRequiredField {

        String optionalField;
    }
}