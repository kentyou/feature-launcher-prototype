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

import static com.kentyou.featurelauncher.impl.decorator.FeatureDecorationConstants.DEFAULT_DECORATED_TYPE;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.BUNDLE_START_LEVELS;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.FRAMEWORK_LAUNCHING_PROPERTIES;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.LAUNCH_FRAMEWORK;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;

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
public class FeatureDecorationUtil {

	// @formatter:off
	private static final Map<String, FeatureExtensionHandler> EXTENSION_TO_BUILTIN_HANDLER_MAP = Map.ofEntries(
			Map.entry(LAUNCH_FRAMEWORK, new LaunchFrameworkFeatureExtensionHandlerImpl()),
			Map.entry(FRAMEWORK_LAUNCHING_PROPERTIES, new FrameworkLaunchingPropertiesFeatureExtensionHandlerImpl()),
			Map.entry(BUNDLE_START_LEVELS, new BundleStartLevelsFeatureExtensionHandlerImpl()));
	// @formatter:on

	private FeatureDecorationUtil() {
		// hidden constructor
	}

	public static Feature executeFeatureDecorators(Feature feature, List<FeatureDecorator> decorators)
			throws AbandonOperationException {
		Feature originalFeature = feature;

		for (FeatureDecorator decorator : decorators) {
			feature = decorator.decorate(feature, new FeatureDecoratorBuilderImpl(feature),
					new DecoratorBuilderFactoryImpl());
		}

		enforceValidFeature(originalFeature, feature);

		return feature;
	}

	public static Feature executeFeatureExtensionHandlers(Feature feature,
			Map<String, FeatureExtensionHandler> extensionHandlers) throws AbandonOperationException {
		Feature originalFeature = feature;

		for (Map.Entry<String, FeatureExtension> featureExtensionEntry : feature.getExtensions().entrySet()) {
			String extensionName = featureExtensionEntry.getKey();
			FeatureExtension featureExtension = featureExtensionEntry.getValue();

			FeatureExtensionHandler handlerForExtension = extensionHandlers.get(extensionName);
			if (handlerForExtension == null) {
				handlerForExtension = getBuiltInHandlerForExtension(extensionName);
			}

			if (handlerForExtension != null) {
				feature = handlerForExtension.handle(feature, featureExtension,
						new FeatureExtensionHandlerBuilderImpl(feature), new DecoratorBuilderFactoryImpl());

			} else if (isExtensionMandatory(featureExtension)) {
				throw new AbandonOperationException(String
						.format("Feature extension handler for mandatory extension %s not found!", extensionName));
			}
		}

		enforceValidFeature(originalFeature, feature);

		return feature;
	}

	public static <T extends FeatureExtensionHandler> T getBuiltInHandlerForExtension(String extensionName)
			throws AbandonOperationException {
		Objects.requireNonNull(extensionName, "Feature extension name cannot be null!");

		@SuppressWarnings("unchecked")
		T handlerForExtension = (T) EXTENSION_TO_BUILTIN_HANDLER_MAP.get(extensionName);

		if (handlerForExtension != null) {
			return handlerForExtension;
		} else {
			throw new AbandonOperationException(
					String.format("Built-in feature extension handler for extension %s not found!", extensionName));
		}
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

	public static Map<String, String> readFeatureExtensionJSON(String jsonString) {
		try (JsonReader jsonReader = Json.createReader(new StringReader(jsonString))) {
			Map<String, String> properties = new HashMap<>();

			JsonObject json = jsonReader.readObject();

			for (String propertyName : json.keySet()) {

				JsonValue propertyValue = json.get(propertyName);

				if (propertyValue instanceof JsonString) {
					properties.put(propertyName, ((JsonString) propertyValue).getString());
				} else if (propertyValue instanceof JsonNumber) {
					properties.put(propertyName, ((JsonNumber) propertyValue).numberValue().toString());
				} else if (propertyValue == JsonValue.TRUE) {
					properties.put(propertyName, Boolean.TRUE.toString());
				} else if (propertyValue == JsonValue.FALSE) {
					properties.put(propertyName, Boolean.FALSE.toString());
				} else if (propertyValue.equals(JsonValue.NULL)) {
					properties.put(propertyName, null);
				}
			}

			return properties;
		}
	}

	private static void enforceValidFeature(Feature originalFeature, Feature returnedFeature)
			throws AbandonOperationException {
		if (!((originalFeature == returnedFeature) || idMatches(originalFeature.getID(), returnedFeature.getID()))) {
			throw new AbandonOperationException(
					"The feature returned by the decorator was not the original, or one created by the supplied builder");
		}
	}

	/*
	 * All data in the original feature can be replaced, except for ID, excluding
	 * classifier, therefore all the other parts of ID must match or default to
	 * those used internally.
	 * 
	 * See {@link com.kentyou.featurelauncher.impl.decorator.
	 * AbstractBaseFeatureDecorationBuilder.prebuild()}
	 */
	private static boolean idMatches(ID originalFeatureId, ID returnedFeatureId) {
		boolean groupIdMatches = originalFeatureId.getGroupId().equals(returnedFeatureId.getGroupId());

		boolean artifactIdMatches = originalFeatureId.getArtifactId().equals(returnedFeatureId.getArtifactId());

		boolean versionMatches = originalFeatureId.getVersion().equals(returnedFeatureId.getVersion());

		/**
		 * this check is necessitated by contract and implementation of
		 * {@link org.osgi.service.feature.FeatureService} which requires specifying
		 * type if classifier is specified as well, therefore if type was initially
		 * empty, it can no longer be empty and default type is used - see
		 * {@link com.kentyou.featurelauncher.impl.decorator.AbstractBaseFeatureDecorationBuilder.prebuild()}
		 **/
		boolean typeMatches = (originalFeatureId.getType().isPresent() && returnedFeatureId.getType().isPresent()
				&& originalFeatureId.getType().get().equals(returnedFeatureId.getType().get()))
				|| (originalFeatureId.getType().isEmpty() && returnedFeatureId.getType().isPresent()
						&& DEFAULT_DECORATED_TYPE.equals(returnedFeatureId.getType().get()));

		return groupIdMatches && artifactIdMatches && versionMatches && typeMatches;
	}
}
