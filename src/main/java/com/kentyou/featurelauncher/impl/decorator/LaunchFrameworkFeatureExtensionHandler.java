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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;

/**
 * Handler for {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.LAUNCH_FRAMEWORK} {@link org.osgi.service.feature.FeatureExtension}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 30, 2024
 */
public interface LaunchFrameworkFeatureExtensionHandler extends FeatureExtensionHandler {

	default Feature handle(Feature feature, FeatureExtension extension,
			FeatureExtensionHandlerBuilder decoratedFeatureBuilder, DecoratorBuilderFactory factory)
			throws AbandonOperationException {
		throw new UnsupportedOperationException(
				"Method not implemented as it's obsolete in `LaunchFrameworkFeatureExtensionHandler`. Use the other methods defined on 'LaunchFrameworkFeatureExtensionHandler'");
	}

	Optional<FrameworkFactory> selectFrameworkFactory(FeatureExtension featureExtension,
			List<ArtifactRepository> artifactRepositories, Optional<FrameworkFactory> defaultFrameworkFactoryOptional);

	boolean hasLaunchFrameworkFeatureExtension(Map<String, FeatureExtension> featureExtensions);

	boolean isLaunchFrameworkFeatureExtensionMandatory(FeatureExtension featureExtension);
}
