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
package org.smooks.cartridges.validation.order;

import org.junit.jupiter.api.Test;
import org.smooks.Smooks;
import org.smooks.api.SmooksException;
import org.smooks.cartridges.validation.OnFailResult;
import org.smooks.cartridges.validation.ValidationResult;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:tom.fennelly@jboss.com">tom.fennelly@jboss.com</a>
 */
public class OrderTest {

    @Test
    public void test_01() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks-config.xml"));
        ValidationResult result = new ValidationResult();

        try {
            smooks.filterSource(new StreamSource(getClass().getResourceAsStream("order-message-01.xml")), result);

            assertEquals(4, result.getNumFailures());

            List<OnFailResult> errors = result.getErrors();
            List<OnFailResult> warnings = result.getWarnings();

            assertEquals(3, errors.size());
            assertEquals(1, warnings.size());

            assertEquals("Invalid customer number '123123' at '/order/header/customer/@number'.  Customer number must match pattern '[A-Z]-[0-9]{5}'.", errors.get(0).getMessage());
            assertEquals("Invalid product ID '222' at '/order/order-items/order-item/product'.  Product ID must match pattern '[0-9]{6}'.", errors.get(1).getMessage());
            assertEquals("Order 12129 (Customer 123123) contains an order item for product 222 which contains an invalid quantity of 7. This quantity exceeds the maximum permited quantity for this product (5).", errors.get(2).getMessage());
            assertEquals("Invalid customer name 'Joe' at '/order/header/customer'.  Customer name must match pattern '[A-Z][a-z]*, [A-Z][a-z]*'.", warnings.get(0).getMessage());
        } finally {
            smooks.close();
        }
    }

    @Test
    public void test_02() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks-config.xml"));
        ValidationResult result = new ValidationResult();

        try {
            smooks.filterSource(new StreamSource(getClass().getResourceAsStream("order-message-02.xml")), result);
            fail("Expected SmooksException");
        } catch(SmooksException e) {
            assertEquals("The maximum number of allowed validation failures (5) has been exceeded.", e.getCause().getMessage());
            assertEquals(6, result.getNumFailures());
        } finally {
            smooks.close();
        }
    }

    @Test
    public void test_03() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("smooks-config.xml"));
        ValidationResult result = new ValidationResult();

        try {
            smooks.filterSource(new StreamSource(getClass().getResourceAsStream("order-message-03.xml")), result);
            fail("Expected SmooksException");
        } catch(SmooksException e) {
            assertEquals("A FATAL validation failure has occured [/order/order-items/order-item/fail] RegexRuleEvalResult, matched=false, providerName=product, ruleName=failProduct, text=true, pattern=false", e.getCause().getMessage());
            assertEquals(5, result.getNumFailures());
            assertEquals("[/order/order-items/order-item/fail] RegexRuleEvalResult, matched=false, providerName=product, ruleName=failProduct, text=true, pattern=false", result.getFatal().toString());
        } finally {
            smooks.close();
        }
    }
}
