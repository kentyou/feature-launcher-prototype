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
package com.kentyou.featurelauncher.impl.util;

import static org.osgi.service.featurelauncher.FeatureLauncherConstants.BUNDLE_START_LEVELS;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.FRAMEWORK_LAUNCHING_PROPERTIES;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.LAUNCH_FRAMEWORK;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kentyou.featurelauncher.impl.decorator.BundleStartLevelsFeatureExtensionHandlerImpl;
import com.kentyou.featurelauncher.impl.decorator.DecoratorBuilderFactoryImpl;
import com.kentyou.featurelauncher.impl.decorator.FeatureDecoratorBuilderImpl;
import com.kentyou.featurelauncher.impl.decorator.FeatureExtensionHandlerBuilderImpl;
import com.kentyou.featurelauncher.impl.decorator.FrameworkLaunchingPropertiesFeatureExtensionHandlerImpl;
import com.kentyou.featurelauncher.impl.decorator.LaunchFrameworkFeatureExtensionHandlerImpl;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Util for {@link org.osgi.service.featurelauncher.decorator.FeatureDecorator}
 * and {@link org.osgi.service.feature.FeatureExtension} operations.
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 27, 2024
 */
public class DecorationUtil {

	private static final Logger LOG = LoggerFactory.getLogger(DecorationUtil.class);
	
	private final LaunchFrameworkFeatureExtensionHandlerImpl launchHandler = new LaunchFrameworkFeatureExtensionHandlerImpl();
	private final FrameworkLaunchingPropertiesFeatureExtensionHandlerImpl frameworkHandler = new FrameworkLaunchingPropertiesFeatureExtensionHandlerImpl();
	private final BundleStartLevelsFeatureExtensionHandlerImpl startLevelHandler = new BundleStartLevelsFeatureExtensionHandlerImpl();
	
	// @formatter:off
	private final Map<String, FeatureExtensionHandler> handlers = Map.ofEntries(
			Map.entry(LAUNCH_FRAMEWORK, launchHandler),
			Map.entry(FRAMEWORK_LAUNCHING_PROPERTIES, frameworkHandler),
			Map.entry(BUNDLE_START_LEVELS, startLevelHandler));
	// @formatter:on

	public LaunchFrameworkFeatureExtensionHandlerImpl getLaunchHandler() {
		return launchHandler;
	}

	public FrameworkLaunchingPropertiesFeatureExtensionHandlerImpl getFrameworkHandler() {
		return frameworkHandler;
	}

	public BundleStartLevelsFeatureExtensionHandlerImpl getStartLevelHandler() {
		return startLevelHandler;
	}

	public Feature executeFeatureDecorators(FeatureService featureService, Feature feature,
			List<FeatureDecorator> decorators) throws AbandonOperationException {

		Feature updatedFeature = feature;

		for (FeatureDecorator decorator : decorators) {
			Feature loopFeature = updatedFeature;
			FeatureDecoratorBuilderImpl decoratedFeatureBuilder = new FeatureDecoratorBuilderImpl(featureService, feature);
			updatedFeature = decorator.decorate(feature, decoratedFeatureBuilder,
					new DecoratorBuilderFactoryImpl(featureService));
			enforceValidFeature(loopFeature, updatedFeature, decoratedFeatureBuilder.getBuilt());
		}

		return updatedFeature;
	}

	public Feature executeFeatureExtensionHandlers(FeatureService featureService, final Feature feature,
			Map<String, FeatureExtensionHandler> extensionHandlers) throws AbandonOperationException {
		Map<String, FeatureExtensionHandler> toUse = new HashMap<String, FeatureExtensionHandler>(extensionHandlers);
		
		handlers.entrySet().forEach(e -> {
			String extension = e.getKey();
			if(toUse.containsKey(extension)) {
				LOG.warn("The extension handler {} is provided by the implementation and may not be overridden.", extension);
			}
			toUse.put(extension, e.getValue());
		});
		
		Feature updatedFeature = feature;

		for (Map.Entry<String, FeatureExtension> featureExtensionEntry : feature.getExtensions().entrySet()) {
			Feature loopFeature = updatedFeature;
			String extensionName = featureExtensionEntry.getKey();
			FeatureExtension featureExtension = featureExtensionEntry.getValue();

			FeatureExtensionHandler handlerForExtension = toUse.get(extensionName);

			if (handlerForExtension != null) {
				FeatureExtensionHandlerBuilderImpl decoratedFeatureBuilder = new FeatureExtensionHandlerBuilderImpl(featureService, feature);
				updatedFeature = handlerForExtension.handle(feature, featureExtension,
						decoratedFeatureBuilder, new DecoratorBuilderFactoryImpl(featureService));

				enforceValidFeature(loopFeature, updatedFeature, decoratedFeatureBuilder.getBuilt());
			} else if (isExtensionMandatory(featureExtension)) {
				throw new AbandonOperationException(String
						.format("Feature extension handler for mandatory extension %s not found!", extensionName));
			}
		}

		return updatedFeature;
	}

	public static boolean isExtensionMandatory(FeatureExtension featureExtension) {
		return featureExtension.getKind() == FeatureExtension.Kind.MANDATORY;
	}

	public static boolean hasLaunchFrameworkFeatureExtension(Map<String, FeatureExtension> featureExtensions) {
		if (!featureExtensions.isEmpty() && featureExtensions.containsKey(LAUNCH_FRAMEWORK)) {
			return ((featureExtensions.get(LAUNCH_FRAMEWORK).getType() == FeatureExtension.Type.ARTIFACTS)
					&& !featureExtensions.get(LAUNCH_FRAMEWORK).getArtifacts().isEmpty());
		}

		return false;
	}

	public static boolean hasFrameworkLaunchingPropertiesFeatureExtension(
			Map<String, FeatureExtension> featureExtensions) {
		if (!featureExtensions.isEmpty() && featureExtensions.containsKey(FRAMEWORK_LAUNCHING_PROPERTIES)) {
			return ((featureExtensions.get(FRAMEWORK_LAUNCHING_PROPERTIES).getType() == FeatureExtension.Type.JSON)
					&& !featureExtensions.get(FRAMEWORK_LAUNCHING_PROPERTIES).getJSON().isBlank());
		}

		return false;
	}

	public static boolean hasBundleStartLevelsFeatureExtension(Map<String, FeatureExtension> featureExtensions) {
		if (!featureExtensions.isEmpty() && featureExtensions.containsKey(BUNDLE_START_LEVELS)) {
			return ((featureExtensions.get(BUNDLE_START_LEVELS).getType() == FeatureExtension.Type.JSON)
					&& !featureExtensions.get(BUNDLE_START_LEVELS).getJSON().isBlank());
		}

		return false;
	}

	public static Map<String, Object> readFeatureExtensionJSON(String jsonString) {
		try (JsonReader jsonReader = Json.createReader(new StringReader(jsonString))) {
			Map<String, Object> properties = new HashMap<>();

			JsonObject json = jsonReader.readObject();

			for (String propertyName : json.keySet()) {

				JsonValue propertyValue = json.get(propertyName);

				if (propertyValue instanceof JsonString) {
					properties.put(propertyName, ((JsonString) propertyValue).getString());
				} else if (propertyValue instanceof JsonNumber) {
					properties.put(propertyName, ((JsonNumber) propertyValue).bigDecimalValue());
				} else if (propertyValue == JsonValue.TRUE) {
					properties.put(propertyName, Boolean.TRUE);
				} else if (propertyValue == JsonValue.FALSE) {
					properties.put(propertyName, Boolean.FALSE);
				} else if (propertyValue.equals(JsonValue.NULL)) {
					properties.put(propertyName, null);
				}
			}

			return properties;
		}
	}

	private static void enforceValidFeature(Feature originalFeature, Feature returnedFeature, Feature builtFeature)
			throws AbandonOperationException {
		if (!((originalFeature == returnedFeature) || (builtFeature == returnedFeature))) {
			throw new AbandonOperationException(
					"The feature returned by the decorator was not the original, or one created by the supplied builder");
		}
	}
}
