/**
 * Copyright (c) 2024 Kentyou and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Kentyou - initial implementation
 */
package com.kentyou.featurelauncher.impl.decorator;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kentyou.featurelauncher.impl.util.FeatureDecorationUtil;

/**
 * Implementation of {@link com.kentyou.featurelauncher.impl.decorator.FrameworkLaunchingPropertiesFeatureExtensionHandler}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 19, 2024
 */
public class FrameworkLaunchingPropertiesFeatureExtensionHandlerImpl
		implements FrameworkLaunchingPropertiesFeatureExtensionHandler {
	private static final Logger LOG = LoggerFactory
			.getLogger(FrameworkLaunchingPropertiesFeatureExtensionHandlerImpl.class);

	private final Map<String, String> frameworkProperties;

	private final Map<String, String> customProperties; // properties starting with a single underscore

	public FrameworkLaunchingPropertiesFeatureExtensionHandlerImpl() {
		this.frameworkProperties = new HashMap<>();
		this.customProperties = new HashMap<>();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler#handle(org.osgi.service.feature.Feature, org.osgi.service.feature.FeatureExtension, org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler.FeatureExtensionHandlerBuilder, org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory)
	 */
	@Override
	public Feature handle(Feature feature, FeatureExtension extension,
			FeatureExtensionHandlerBuilder decoratedFeatureBuilder, DecoratorBuilderFactory factory)
			throws AbandonOperationException {

		Map<String, String> rawProperties = FeatureDecorationUtil.readFeatureExtensionJSON(extension.getJSON());

		Map<String, String> properties = maybeSubstituteVariables(rawProperties, feature.getVariables());

		for (Map.Entry<String, String> propertyEntry : properties.entrySet()) {
			String propertyName = propertyEntry.getKey();
			String propertyValue = propertyEntry.getValue().toString();

			if (propertyName.startsWith("__")) {
				frameworkProperties.put(propertyName.replaceFirst("_", ""), propertyValue);
			} else if (propertyName.startsWith("_")) {
				customProperties.put(propertyName, propertyValue);
			} else {
				frameworkProperties.put(propertyName, propertyValue);
			}
		}

		return feature;
	}

	/* 
	 * (non-Javadoc)
	 * @see com.kentyou.featurelauncher.impl.decorator.FrameworkLaunchingPropertiesFeatureExtensionHandler#getFrameworkProperties()
	 */
	@Override
	public Map<String, String> getFrameworkProperties() {
		return frameworkProperties;
	}

	/* 
	 * (non-Javadoc)
	 * @see com.kentyou.featurelauncher.impl.decorator.FrameworkLaunchingPropertiesFeatureExtensionHandler#getCustomProperties()
	 */
	@Override
	public Map<String, String> getCustomProperties() {
		return customProperties;
	}

	private Map<String, String> maybeSubstituteVariables(Map<String, String> properties,
			Map<String, Object> featureVariables) {
		if (!properties.isEmpty() && !featureVariables.isEmpty()) {
			StringSubstitutor variablesSubstitutor = new StringSubstitutor(featureVariables);
			Map<String, String> substituted = new HashMap<>();

			for (Map.Entry<String, String> propertyEntry : properties.entrySet()) {
				String propertyName = propertyEntry.getKey();
				String propertyValue = propertyEntry.getValue();

				substituted.put(propertyName, variablesSubstitutor.replace(propertyValue));
			}

			return substituted;
		}

		return properties;
	}
}