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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.api.ApplicationContext;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.SmooksException;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.api.resource.visitor.VisitAfterReport;
import org.smooks.api.resource.visitor.VisitBeforeReport;
import org.smooks.api.resource.visitor.sax.ng.AfterVisitor;
import org.smooks.api.resource.visitor.sax.ng.ChildrenVisitor;
import org.smooks.cartridges.rules.RuleEvalResult;
import org.smooks.cartridges.rules.RuleProvider;
import org.smooks.cartridges.rules.RuleProviderAccessor;
import org.smooks.engine.delivery.fragment.NodeFragment;
import org.smooks.engine.memento.TextAccumulatorMemento;
import org.smooks.engine.memento.TextAccumulatorVisitorMemento;
import org.smooks.engine.resource.config.xpath.IndexedSelectorPath;
import org.smooks.engine.resource.config.xpath.step.AttributeSelectorStep;
import org.smooks.io.payload.FilterResult;
import org.smooks.resource.URIResourceLocator;
import org.smooks.support.DomUtils;
import org.smooks.support.FreeMarkerTemplate;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;

import jakarta.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 *
 * </p>
 * A Validator uses a predefined Rule that performs the actual validator for a Validator. This way a Validator does not know
 * about the technology used for the validation and users can mix and max different rules as appropriate to the use case they
 * have. For example, one problem might be solve nicely with a regular expression but another might be easier to sovle using
 * a MVEL expression.
 *
 * Example configuration:
 * <pre>{@code
 * <rules:ruleBases>
 *    <rules:ruleBase name="addressing" src="usa_address.properties" provider="org.smooks.validation.RegexProvider" />
 * </rules:ruleBases>
 *
 * <validation:field on="order/header/email" rule="addressing.email" onFail="WARN" />
 *
 * }</pre>
 * Options:
 * <ul>
 *  <li><b><i>on</b></i>
 *  The fragement that the validation will be performed upon. </li>
 *
 *  <li><b><i>rule</b></i>
 *  Is the name of a previously defined in a rules element. The rule itself is identified by ruleProviderName.ruleName.
 *  So taking the above example addressing is the ruleProviderName and email is the rule name. In this case email
 *  identifies a regular expression but if you were to change the provider that might change and a differnet technology
 *  could be used to validate an email address.</li>
 *
 *  <li><b><i>onFail</b></i>
 *  The onFail attribute in the validation configuration specified what action should be taken when a rule matches.
 *  This is all about reporting back valdiation failures.
 *  </li>
 *
 * </ul>
 *
 * @author <a href="mailto:daniel.bevenius@gmail.com">Daniel Bevenius</a>
 *
 */
@VisitBeforeReport(condition = "false")
@VisitAfterReport(summary = "Applied validation rule '${resource.parameters.name}'.")
public final class Validator implements ChildrenVisitor, AfterVisitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Validator.class);

    /**
     * The name of the rule that will be used by this validator.
     */
    private String compositRuleName;
    /**
     * Rule provider name.
     */
    private String ruleProviderName;
    /**
     * Rule name.
     */
    private String ruleName;
    /**
     * Rule provider for this validator.
     */
    private RuleProvider ruleProvider;
    /**
     * The validation failure level. Default is OnFail.ERROR.
     */
    private OnFail onFail = OnFail.ERROR;
    /**
     * The Smooks {@link ApplicationContext}.
     */
    @Inject
    private ApplicationContext appContext;
    /**
     * Config.
     */
    @Inject
    private ResourceConfig resourceConfig;
    /**
     * Attribute name if the validation target is an attribute, otherwise null.
     */
    private String targetAttribute;

    /**
     * Message bundle name for the ruleset.
     */
    private String messageBundleBaseName;
    /**
     * The maximum number of failures permitted per {@link ValidationResult} instance..
     */
    private int maxFails;

    /**
     * No-args constructor required by Smooks.
     */
    public Validator() {
    }

    /**
     * Initialize the visitor instance.
     */
    @PostConstruct
    public void postConstruct() {
        if (resourceConfig.getSelectorPath() instanceof IndexedSelectorPath &&
                ((IndexedSelectorPath) resourceConfig.getSelectorPath()).getTargetSelectorStep() instanceof AttributeSelectorStep) {
            targetAttribute = ((AttributeSelectorStep) ((IndexedSelectorPath) resourceConfig.getSelectorPath()).
                    getTargetSelectorStep()).getQName().getLocalPart();
        } else {
            targetAttribute = null;
        }
    }

    /**
     * Public constructor.
     *
     * @param compositeRuleName The name of the rule that will be used by this validator.
     * @param onFail           The failure level.
     */
    public Validator(final String compositeRuleName, final OnFail onFail) {
        setCompositRuleName(compositeRuleName);
        this.onFail = onFail;
    }
    
    @Override
    public void visitAfter(final Element element, final ExecutionContext executionContext) throws SmooksException {
        if (targetAttribute != null) {
            OnFailResultImpl result = _validate(element.getAttribute(targetAttribute), executionContext);
            if (result != null) {
                result.setFailFragmentPath(DomUtils.getXPath(element) + "/@" + targetAttribute);
                assertValidationException(result, executionContext);
            }
        } else {
            TextAccumulatorMemento textAccumulatorMemento = new TextAccumulatorVisitorMemento(new NodeFragment(element), this);
            executionContext.getMementoCaretaker().restore(textAccumulatorMemento);

            OnFailResultImpl result = _validate(textAccumulatorMemento.getText(), executionContext);
            if (result != null) {
                result.setFailFragmentPath(DomUtils.getXPath(element));
                assertValidationException(result, executionContext);
            }
        }
    }
    
    private void assertValidationException(OnFailResultImpl result, ExecutionContext executionContext) {
        if (onFail == OnFail.FATAL) {
            throw new ValidationException("A FATAL validation failure has occured " + result, result);
        }

        ValidationResult validationResult = getValidationResult(executionContext);
        if (validationResult.getNumFailures() > maxFails) {
            throw new ValidationException("The maximum number of allowed validation failures (" + maxFails + ") has been exceeded.", result);
        }
    }

    /**
     * Validate will lookup the configured RuleProvider and validate the text against the
     * rule specfied by the composite rule name.
     *
     * @param text             The selected data to perform the evaluation on.
     * @param executionContext The Smooks {@link org.smooks.api.ExecutionContext}.
     * @throws ValidationException A FATAL Validation failure has occured, or the maximum number of
     *                             allowed failures has been exceeded.
     */
    void validate(final String text, final ExecutionContext executionContext) throws ValidationException {
        OnFailResultImpl result = _validate(text, executionContext);
        if (result != null) {
            assertValidationException(result, executionContext);
        }
    }

    /**
     * Validate will lookup the configured RuleProvider and validate the text against the
     * rule specfied by the composite rule name.
     *
     * @param text             The selected data to perform the evaluation on.
     * @param executionContext The Smooks {@link org.smooks.api.ExecutionContext}.
     * @throws ValidationException A FATAL Validation failure has occured, or the maximum number of
     *                             allowed failures has been exceeded.
     */
    private OnFailResultImpl _validate(final String text, final ExecutionContext executionContext) throws ValidationException {
        if (ruleProvider == null) {
            setRuleProvider(executionContext);
        }

        final RuleEvalResult result = ruleProvider.evaluate(ruleName, text, executionContext);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(result.toString());
        }

        if (!result.matched()) {
            ValidationResult validationResult = getValidationResult(executionContext);
            OnFailResultImpl onFailResult = new OnFailResultImpl();
            onFailResult.setRuleResult(result);
            onFailResult.setBeanContext(executionContext.getBeanContext().getBeanMap());
            validationResult.addResult(onFailResult, onFail);

            return onFailResult;
        }

        return null;
    }

    private ValidationResult getValidationResult(ExecutionContext executionContext) {
        ValidationResult validationResult = (ValidationResult) FilterResult.getResult(executionContext, ValidationResult.class);
        // Create a new ValidationResult if one was not available in the execution context.
        // This would be the case for example if one as not specified to Smooks filter method.
        if (validationResult == null) {
            validationResult = new ValidationResult();
        }

        return validationResult;
    }

    private synchronized void setRuleProvider(ExecutionContext executionContext) {
        if (ruleProvider != null) {
            return;
        }

        ruleProvider = RuleProviderAccessor.get(appContext, ruleProviderName);
        if (ruleProvider == null) {
            throw new SmooksException("Unknown rule provider '" + ruleProviderName + "'.");
        }

        // Configure the base bundle name for validation failure messages...
        setMessageBundleBaseName();

        // Configure the maxFails per ValidationResult instance...
        String maxFailsConfig = executionContext.getConfigParameter(OnFailResult.MAX_FAILS);
        if (maxFailsConfig != null) {
            try {
                maxFails = Integer.parseInt(maxFailsConfig.trim());
            } catch (NumberFormatException e) {
                throw new SmooksConfigException("Invalid config value '" + maxFailsConfig.trim() + "' for global parameter '" + OnFailResult.MAX_FAILS + "'.  Must be a valid Integer value.");
            }
        } else {
            maxFails = Integer.MAX_VALUE;
        }
    }

    private void setMessageBundleBaseName() {
        String ruleSource = ruleProvider.getSrc();
        File srcFile = new File(ruleSource);
        String srcFileName = srcFile.getName();
        int indexOfExt = srcFileName.lastIndexOf('.');
        File parentFolder = srcFile.getParentFile();

        if (indexOfExt != -1) {
            messageBundleBaseName = srcFileName.substring(0, indexOfExt);
        } else {
            messageBundleBaseName = ruleSource;
        }

        if (parentFolder != null) {
            messageBundleBaseName = parentFolder.getPath() + "/i18n/" + messageBundleBaseName;
        } else {
            messageBundleBaseName = "i18n/" + messageBundleBaseName;
        }

        messageBundleBaseName = messageBundleBaseName.replace('\\', '/');
    }

    @Override
    public String toString() {
        return String.format("%s [rule=%s, onFail=%s]", getClass().getSimpleName(), compositRuleName, onFail);
    }

    @Inject
    public void setCompositRuleName(@Named("name") final String compositRuleName) {
        this.compositRuleName = compositRuleName;
        this.ruleProviderName = RuleProviderAccessor.parseRuleProviderName(compositRuleName);
        this.ruleName = RuleProviderAccessor.parseRuleName(compositRuleName);
    }

    public String getCompositRuleName() {
        return compositRuleName;
    }

    @Inject
    public void setOnFail(final Optional<OnFail> onFail) {
        this.onFail = onFail.orElse(OnFail.ERROR);
    }

    public OnFail getOnFail() {
        return onFail;
    }

    public Validator setAppContext(ApplicationContext appContext) {
        this.appContext = appContext;
        return this;
    }

    @Override
    public void visitChildText(CharacterData characterData, ExecutionContext executionContext) {
        if (targetAttribute == null) {
            // The selected text is not an attribute, which means it's the element text,
            // which means we need to turn on text accumulation for SAX...
            TextAccumulatorMemento textAccumulatorMemento = new TextAccumulatorVisitorMemento(new NodeFragment(characterData.getParentNode()), this);
            executionContext.getMementoCaretaker().restore(textAccumulatorMemento);
            textAccumulatorMemento.accumulateText(characterData.getTextContent());
            executionContext.getMementoCaretaker().capture(textAccumulatorMemento);
        }
    }

    @Override
    public void visitChildElement(Element childElement, ExecutionContext executionContext) {

    }

    private class OnFailResultImpl implements OnFailResult {

        private String failFragmentPath;
        private RuleEvalResult ruleResult;
        public Map<String, Object> beanContext;

        public void setFailFragmentPath(String failFragmentPath) {
            this.failFragmentPath = failFragmentPath;
        }

        public String getFailFragmentPath() {
            return failFragmentPath;
        }

        public void setRuleResult(RuleEvalResult ruleResult) {
            this.ruleResult = ruleResult;
        }

        public RuleEvalResult getFailRuleResult() {
            return ruleResult;
        }

        public void setBeanContext(Map<String, Object> beanContext) {
            // Need to create a shallow copy as the context data may change.
            // Even this is not foolproof, as internal bean data can also be
            // overwritten by the bean context!!
            this.beanContext = new HashMap<String, Object>();
            this.beanContext.putAll(beanContext);
        }

        public String getMessage() {
            return getMessage(Locale.getDefault());
        }

        public String getMessage(Locale locale) {
            if (ruleResult.getEvalException() != null) {
                return ruleResult.getEvalException().getMessage();
            }

            String message = getMessage(locale, ruleName);
            // If no ResouceBundle was configured then use this instances toString
            if (message == null) {
                return toString();
            }

            if (message.startsWith("ftl:")) {
                // TODO: Is there a way to optimize this e.g. attach the compiled template
                // to the bundle as an object and then get back using ResourceBundle.getObject??
                // I timed it and it was able to create and apply 10000 templates in about 2500 ms
                // on an "average" spec machine, so it's not toooooo bad, and it's only done on demand :)
                FreeMarkerTemplate template = new FreeMarkerTemplate(message.substring("ftl:".length()));
                beanContext.put("ruleResult", ruleResult);
                beanContext.put("path", failFragmentPath);
                message = template.apply(beanContext);
            }

            return message;
        }

        private String getMessage(final Locale locale, final String messageName) {
            final ResourceBundle bundle = getMessageBundle(locale);
            if (messageName == null || bundle == null)
                return null;

            return bundle.getString(messageName);
        }

        /**
         * @param locale The Locale to look up.
         * @return {@link ResourceBundle} for the Locale and message bundle base name. Or null if no bundle exists.
         */
        private ResourceBundle getMessageBundle(final Locale locale) {
            try {
                return ResourceBundle.getBundle(messageBundleBaseName, locale, new ResourceBundleClassLoader());
            } catch (final MissingResourceException e) {
                LOGGER.warn("Failed to load Validation rule message bundle '" + messageBundleBaseName + "'.  This resource must be on the classpath!", e);
            }

            return null;
        }

        @Override
        public String toString() {
            return "[" + failFragmentPath + "] " + ruleResult.toString();
        }
    }

    private static class ResourceBundleClassLoader extends ClassLoader {
        @Override
        public InputStream getResourceAsStream(String name) {
            try {
                return new URIResourceLocator().getResource(name);
            } catch (IOException e) {
                return null;
            }
        }
    }
}
