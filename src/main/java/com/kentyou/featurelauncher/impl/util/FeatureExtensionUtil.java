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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.featurelauncher.LaunchException;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;

import com.kentyou.featurelauncher.impl.decorator.BundleStartLevelsFeatureExtensionHandler;
import com.kentyou.featurelauncher.impl.decorator.DecoratorBuilderFactoryImpl;
import com.kentyou.featurelauncher.impl.decorator.FeatureExtensionHandlerBuilderImpl;
import com.kentyou.featurelauncher.impl.decorator.FrameworkLaunchingPropertiesFeatureExtensionHandlerImpl;
import com.kentyou.featurelauncher.impl.decorator.LaunchFrameworkFeatureExtensionHandlerImpl;

/**
 * Util for {@link org.osgi.service.feature.FeatureExtension} operations.
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 19, 2024
 */
public class FeatureExtensionUtil {

	// @formatter:off
	private static final Map<String, FeatureExtensionHandler> EXTENSION_TO_BUILTIN_HANDLER_MAP = Map.ofEntries(
			Map.entry(LAUNCH_FRAMEWORK, new LaunchFrameworkFeatureExtensionHandlerImpl()),
			Map.entry(FRAMEWORK_LAUNCHING_PROPERTIES, new FrameworkLaunchingPropertiesFeatureExtensionHandlerImpl()),
			Map.entry(BUNDLE_START_LEVELS, new BundleStartLevelsFeatureExtensionHandler()));
	// @formatter:on

	private FeatureExtensionUtil() {
		// hidden constructor
	}

	public static Optional<FeatureExtensionHandler> getBuiltInHandlerForExtension(String extensionName) {
		Objects.requireNonNull(extensionName, "Feature extension name cannot be null!");

		return Optional.ofNullable(EXTENSION_TO_BUILTIN_HANDLER_MAP.get(extensionName));
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

	public static Feature executeFeatureExtensionHandlers(Feature feature,
			Map<String, FeatureExtensionHandler> extensionHandlers) {
		for (Map.Entry<String, FeatureExtension> featureExtensionEntry : feature.getExtensions().entrySet()) {
			String extensionName = featureExtensionEntry.getKey(); // TODO: clarify regarding extension name to
																	// be used, as it's also available via
																	// `featureExtension.getName()`
			FeatureExtension featureExtension = featureExtensionEntry.getValue();

			Optional<FeatureExtensionHandler> handlerForExtension = Optional
					.ofNullable(extensionHandlers.get(extensionName));
			if (handlerForExtension.isEmpty()) {
				handlerForExtension = FeatureExtensionUtil.getBuiltInHandlerForExtension(extensionName);
			}

			if (handlerForExtension.isPresent()) {
				try {
					feature = handlerForExtension.get().handle(feature, featureExtension,
							new FeatureExtensionHandlerBuilderImpl(feature), new DecoratorBuilderFactoryImpl());
				} catch (AbandonOperationException e) {
					throw new LaunchException("Feature decoration handling failed!", e);
				}

			} else if (FeatureExtensionUtil.isExtensionMandatory(featureExtension)) {
				throw new LaunchException(
						String.format("Feature extension handler for extension %s not found!", extensionName));
			}
		}

		return feature;
	}
}
