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

import org.smooks.cartridges.rules.RuleEvalResult;
import org.smooks.cartridges.rules.regex.RegexRuleEvalResult;
import org.smooks.cartridges.rules.regex.RegexProvider;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * {@link OnFail} result.
 * <p/>
 * Contains details of a validation rule failure.
 *
 * <h3 id="localized-messages">Localized Messages</h3>
 * Every rule source file (no mater what the provider type) can have associated
 * Validation message files ({@link ResourceBundle ResourceBundles}).
 * <p/>
 * The base bundle name is derived from the rules src.  The Validation message base bundle name for
 * a rules source "/a/b/c/rulesX.xml" would be "a/b/c/i18n/rulesX", which means that localized
 * {@link ResourceBundle} files would need to be supplied on the classpath/filesystem as "/a/b/c/i18n/rulesX.properties",
 * "/a/b/c/i18n/rulesX_en.properties", "/a/b/c/i18n/rulesX_de.properties" etc.  The message property
 * names are based on the rule property names e.g. the message bundle property name for rule "orderId"
 * would also need to be "orderId".
 * <p/>
 * The validation cartridge supports application of <b>FreeMarker templates</b> on the localized messages,
 * allowing the messages to contain contextual data from the bean context, as well as data about the actual
 * rule failure.  FreeMarker based messages must be prefixed with "ftl:" and the contextual data is references
 * using the normal FreeMarker notation.  The beans from the bean context can be referenced directly, while the
 * {@link RuleEvalResult} and rule failure path can be referenced through the "ruleResult" and "path"
 * beans.
 * <p/>
 * Example message using {@link RegexProvider} rules:<br/>
 * <pre>
 * customerId=ftl:Invalid customer number '${{@link RegexRuleEvalResult#getText() ruleResult.text}}' at '${path}'.  Customer number must match pattern '${{@link RegexRuleEvalResult#getPattern() ruleResult.pattern}}'.
 * </pre>
 *
 * @author <a href="mailto:tom.fennelly@jboss.com">tom.fennelly@jboss.com</a>
 */
public interface OnFailResult {

    /**
     * Maximum failures global param key.
     */
    public static final String MAX_FAILS = "validation.maxFails";

    /**
     * Get the path to the fragment on which the validation rule failure
     * occured.
     *
     * @return The path to the fragment on which the validation rule failure
     * occured.
     */
    String getFailFragmentPath();

    /**
     * Get the validation failure {@link RuleEvalResult}.
     *
     * @return The validation failure {@link RuleEvalResult}.
     */
    RuleEvalResult getFailRuleResult();

    /**
     * Get a localized message for the validation failure.
     * <p/>
     * Uses the default {@link Locale}.
     * <p/>
     * <a href="#localized-messages">See Localized Messages</a>.
     *
     * @return A localized message for the validation failure.
     */
    String getMessage();

    /**
     * Get a localized message for the validation failure.
     * <p/>
     * <a href="#localized-messages">See Localized Messages</a>.
     *
     * @return A localized message for the validation failure.
     */
    String getMessage(Locale locale);
}
