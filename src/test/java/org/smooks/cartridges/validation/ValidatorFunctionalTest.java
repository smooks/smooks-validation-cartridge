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

import org.junit.Test;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.io.payload.StringResult;
import org.smooks.io.payload.StringSource;
import org.smooks.support.StreamUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Function test for {@link Validator}
 *
 * @author <a href="mailto:danielbevenius@gmail.com">Daniel Bevenius</a>
 */
public class ValidatorFunctionalTest {
    @Test
    public void filter() throws IOException, SAXException {
        InputStream config = null;
        try {
            config = getSmooksConfig("smooks-validation-config.xml");
            final Smooks smooks = new Smooks(config);

            final String xml = readStringFromFile("validation-test.xml");

            final ExecutionContext context = smooks.createExecutionContext();
            final StringResult result = new StringResult();
            final ValidationResult validationResult = new ValidationResult();

            smooks.filterSource(context, new StringSource(xml), result, validationResult);

            final List<OnFailResult> warnings = validationResult.getWarnings();

            assertEquals(1, warnings.size());
            assertEquals(0, validationResult.getOKs().size());
            assertEquals(0, validationResult.getErrors().size());

        } finally {
            if (config != null)
                config.close();
        }
    }

    private InputStream getSmooksConfig(final String fileName) {
        return getClass().getResourceAsStream("/smooks-configs/extended/1.0/" + fileName);
    }

    private String readStringFromFile(final String fileName) throws IOException {
        return StreamUtils.readStreamAsString(getClass().getResourceAsStream("/test-input-files/" + fileName), "UTF-8");
    }
}
