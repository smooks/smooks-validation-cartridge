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
import org.smooks.cartridges.rules.RuleEvalResult;
import org.smooks.tck.MockExecutionContext;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Test for {@link ValidationResult}.
 *
 * @author <a href="mailto:danielbevenius@gmail.com">Daniel Bevenius</a>
 */
public class ValidationResultsTest
{
    private MockExecutionContext context;
    private MockResult result;

    @BeforeEach
    public void beforeEach()
    {
        context = new MockExecutionContext();
        result = new MockResult("ruleName", "provider", true);
    }

    @Test
    public void addWarn()
    {
        ValidationResult validationResult = new ValidationResult();

        validationResult.addResult(new MockOnFailResult(result), OnFail.WARN);
        List<OnFailResult> warnings = validationResult.getWarnings();
        assertFalse(warnings.isEmpty());
        assertEquals(1, warnings.size());

        validationResult.addResult(new MockOnFailResult(result), OnFail.WARN);
        warnings = validationResult.getWarnings();
        assertEquals(2, warnings.size());
    }

    private class MockResult implements RuleEvalResult
    {
        private String ruleName;
        private String name;
        private boolean matched;

        public MockResult(final String ruleName, final String name, final boolean matched)
        {
            this.ruleName = ruleName;
            this.name = name;
            this.matched = matched;
        }

        public void setRuleName(final String ruleName)
        {
            this.ruleName = ruleName;
        }

        public String getRuleName()
        {
            return ruleName;
        }

        public void setRuleProviderName(final String name)
        {
            this.name = name;
        }
        public String getRuleProviderName()
        {
            return name;
        }

        public Throwable getEvalException() {
            return null;
        }

        public void setMatched(final boolean matched)
        {
            this.matched = matched;
        }

        public boolean matched()
        {
            return matched;
        }

        @Override
        public String toString()
        {
            return "MockResult";
        }
    }

    private class MockOnFailResult implements OnFailResult {
        private RuleEvalResult ruleResult;

        public MockOnFailResult(RuleEvalResult ruleResult) {
            this.ruleResult = ruleResult;
        }

        public String getFailFragmentPath() {
            return "x";
        }

        public RuleEvalResult getFailRuleResult() {
            return ruleResult;
        }

        public String getMessage() {
            return "x";
        }

        public String getMessage(Locale locale) {
            return "x";
        }
    }
}
