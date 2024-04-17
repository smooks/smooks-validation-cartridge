/*-
 * ========================LICENSE_START=================================
 * smooks-validation-cartridge
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 *
 * ======================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ======================================================================
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.cartridges.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.smooks.Smooks;
import org.smooks.api.ApplicationContext;
import org.smooks.cartridges.rules.RuleProviderAccessor;
import org.smooks.cartridges.rules.regex.RegexProvider;
import org.smooks.engine.DefaultApplicationContextBuilder;
import org.smooks.io.payload.FilterResult;
import org.smooks.io.payload.StringSource;
import org.smooks.testkit.MockApplicationContext;
import org.smooks.testkit.MockExecutionContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for {@link Validator}
 *
 * @author <a href="mailto:danielbevenius@gmail.com">Daniel Bevenius</a>
 */
public class ValidatorTestCase {
    private MockApplicationContext appContext;
    private RegexProvider regexProvider;

    @BeforeEach
    public void beforeEach() {
        appContext = new MockApplicationContext();
        regexProvider = new RegexProvider("/smooks-regex.properties");
    }

    @Test
    public void configure() {
        final String ruleName = "addressing.email";
        final Validator validator = new Validator(ruleName, OnFail.WARN);

        assertEquals(ruleName, validator.getCompositRuleName());
        assertEquals(OnFail.WARN, validator.getOnFail());
    }

    @Test
    public void testValidateWarn() {
        regexProvider.setName("addressing");
        RuleProviderAccessor.add(appContext, regexProvider);

        final String ruleName = "addressing.email";
        final Validator validator = new Validator(ruleName, OnFail.WARN).setAppContext(appContext);
        final ValidationResult result = new ValidationResult();

        MockExecutionContext executionContext = new MockExecutionContext();
        FilterResult.setResults(executionContext, result);
        validator.validate("xyz", executionContext);
        validator.validate("xyz", executionContext);
        validator.validate("xyz", executionContext);

        assertEquals(0, result.getOKs().size());
        assertEquals(3, result.getWarnings().size());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void testValidateOks() {
        regexProvider.setName("addressing");
        RuleProviderAccessor.add(appContext, regexProvider);

        final String ruleName = "addressing.email";
        final Validator validator = new Validator(ruleName, OnFail.OK).setAppContext(appContext);
        final ValidationResult result = new ValidationResult();

        MockExecutionContext executionContext = new MockExecutionContext();
        FilterResult.setResults(executionContext, result);
        validator.validate("xyz", executionContext);
        validator.validate("xyz", executionContext);
        validator.validate("xyz", executionContext);

        assertEquals(3, result.getOKs().size());
        assertEquals(0, result.getWarnings().size());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void testValidateErrors() {
        regexProvider.setName("addressing");
        RuleProviderAccessor.add(appContext, regexProvider);

        final String ruleName = "addressing.email";
        final Validator validator = new Validator(ruleName, OnFail.ERROR).setAppContext(appContext);
        final ValidationResult result = new ValidationResult();

        MockExecutionContext executionContext = new MockExecutionContext();
        FilterResult.setResults(executionContext, result);
        validator.validate("xyz", executionContext);
        validator.validate("xyz", executionContext);
        validator.validate("xyz", executionContext);

        assertEquals(0, result.getOKs().size());
        assertEquals(0, result.getWarnings().size());
        assertEquals(3, result.getErrors().size());
    }

    @Test
    public void testValidateFatal() {
        regexProvider.setName("addressing");
        RuleProviderAccessor.add(appContext, regexProvider);

        final String ruleName = "addressing.email";
        final String data = "xyz";
        final Validator validator = new Validator(ruleName, OnFail.FATAL).setAppContext(appContext);

        MockExecutionContext executionContext = new MockExecutionContext();
        try {
            validator.validate(data, executionContext);
            fail("A ValidationException should have been thrown");
        } catch (final Exception e) {
            assertTrue(e instanceof ValidationException);

            OnFailResult onFailResult = ((ValidationException) e).getOnFailResult();
            assertNotNull(onFailResult);
            /*
             *  [null] is the failFragmentPath. This test method only exercises the validate method, hence the
             *  frailFramentPath, which is set in visitAfte, is never set.
             */
            String expected = "[null] RegexRuleEvalResult, matched=false, providerName=addressing, ruleName=email, text=xyz, pattern=\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*([,;]\\s*\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*)*";
            assertEquals(expected, onFailResult.getMessage());
            assertEquals("A FATAL validation failure has occured " + expected, e.getMessage());
        }
    }

    @Test
    public void testXmlConfig01() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("config-01.xml"));
        ValidationResult result = new ValidationResult();

        smooks.filterSource(new StringSource("<a><b x='Xx'>11</b><b x='C'>Aaa</b></a>"), result);

        List<OnFailResult> warnings = result.getWarnings();
        assertEquals(2, warnings.size());
        assertEquals("RegexRuleEvalResult, matched=false, providerName=regex, ruleName=custom, text=11, pattern=[A-Z]([a-z])+", warnings.get(0).getFailRuleResult().toString());
        assertEquals("RegexRuleEvalResult, matched=false, providerName=regex, ruleName=custom, text=C, pattern=[A-Z]([a-z])+", warnings.get(1).getFailRuleResult().toString());
    }

    @Test
    public void testValidateGivenAppContextWithClassLoader() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ApplicationContext applicationContext = new DefaultApplicationContextBuilder().withClassLoader(new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals("//i18n/smooks-regex")) {
                    countDownLatch.countDown();
                }
                return super.loadClass(name);
            }
        }).build();
        regexProvider.setName("addressing");
        RuleProviderAccessor.add(applicationContext, regexProvider);

        String ruleName = "addressing.email";
        OnFail onFail = OnFail.values()[new Random().nextInt(OnFail.values().length)];
        Validator validator = new Validator(ruleName, onFail).setAppContext(applicationContext);
        ValidationResult result = new ValidationResult();

        MockExecutionContext executionContext = new MockExecutionContext();
        FilterResult.setResults(executionContext, result);
        try {
            validator.validate("xyz", executionContext);
            switch (onFail) {
                case OK:
                    result.getOKs().get(0).getMessage();
                    break;
                case WARN:
                    result.getWarnings().get(0).getMessage();
                    break;
                case ERROR:
                    result.getErrors().get(0).getMessage();
                    break;
            }
        } catch (ValidationException e) {
            result.getFatal().getMessage();
        }
        assertEquals(0, countDownLatch.getCount());
    }

}
