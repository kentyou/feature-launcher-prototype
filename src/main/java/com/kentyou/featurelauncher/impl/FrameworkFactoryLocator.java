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
package com.kentyou.featurelauncher.impl;

import static org.osgi.service.featurelauncher.FeatureLauncherConstants.LAUNCH_FRAMEWORK;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.featurelauncher.LaunchException;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;

import com.kentyou.featurelauncher.impl.decorator.LaunchFrameworkFeatureExtensionHandler;
import com.kentyou.featurelauncher.impl.util.FeatureDecorationUtil;

/**
 * 160.4.3.2: Locating a framework implementation
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 18, 2024
 */
class FrameworkFactoryLocator {
	public static FrameworkFactory locateFrameworkFactory(Feature feature,
			List<ArtifactRepository> artifactRepositories) {
		/*
		 * TODO: "160.4.3.2: #1. If any provider specific configuration has been given
		 * to the Feature Launcher implementation then this should be used to identify
		 * the framework"
		 */

		/*
		 * "160.4.3.2: #2. If the Feature declares an Extension LAUNCH_FRAMEWORK then
		 * the Feature Launcher implementation must use the first listed artifact that
		 * can be found in any configured Artifact Repositories, as described in
		 * Selecting a framework implementation on page 99"
		 */
		if (FeatureDecorationUtil.hasLaunchFrameworkFeatureExtension(feature.getExtensions())) {
			LaunchFrameworkFeatureExtensionHandler launchFrameworkFeatureExtensionHandler;
			try {
				launchFrameworkFeatureExtensionHandler = FeatureDecorationUtil
						.getBuiltInHandlerForExtension(LAUNCH_FRAMEWORK);
			} catch (AbandonOperationException e) {
				throw new LaunchException(e.getMessage(), e);
			}

			FeatureExtension launchFrameworkFeatureExtension = feature.getExtensions().get(LAUNCH_FRAMEWORK);

			Optional<FrameworkFactory> selectFrameworkFactoryOptional = launchFrameworkFeatureExtensionHandler
					.selectFrameworkFactory(launchFrameworkFeatureExtension, artifactRepositories,
							loadDefaultFrameworkFactory());
			if (selectFrameworkFactoryOptional.isPresent()) {
				return selectFrameworkFactoryOptional.get();
			} else if (FeatureDecorationUtil.isExtensionMandatory(launchFrameworkFeatureExtension)) {
				throw new LaunchException("No suitable OSGi framework implementation could be selected!");
			}
		}

		/*
		 * TODO: "160.4.3.2: #3. If no framework implementation is found in the previous
		 * steps then the Feature Launcher implementation must search the classpath
		 * using the Thread Context Class Loader, or, if the Thread Context Class Loader
		 * is not set, the Class Loader which loaded the caller of the Feature
		 * Launcher's launch method. The first suitable framework instance located is
		 * the instance that will be used."
		 */

		/*
		 * 160.4.3.2: #4. In the event that no suitable OSGi framework can be found by
		 * any of the previous steps then the Feature Launcher implementation may
		 * provide a default framework implementation to be used.
		 */
		Optional<FrameworkFactory> defaultFrameworkFactoryOptional = loadDefaultFrameworkFactory();
		if (defaultFrameworkFactoryOptional.isPresent()) {
			return defaultFrameworkFactoryOptional.get();
		} else {
			throw new LaunchException("Error loading default framework factory!");
		}
	}

	/*
	 * "160.4.3.2: #3. If no framework implementation is found in the previous steps
	 * then the Feature Launcher implementation must search the classpath using the
	 * Thread Context Class Loader, or, if the Thread Context Class Loader is not
	 * set, the Class Loader which loaded the caller of the Feature Launcher's
	 * launch method. The first suitable framework instance located is the instance
	 * that will be used."
	 */
	@SuppressWarnings("unused")
	private static FrameworkFactory findFrameworkFactory() {
		// TODO: 160.4.3.2: #3
		return null;
	}

	/*
	 * 160.4.3.2: #4. In the event that no suitable OSGi framework can be found by
	 * any of the previous steps then the Feature Launcher implementation may
	 * provide a default framework implementation to be used.
	 */
	private static Optional<FrameworkFactory> loadDefaultFrameworkFactory() {
		ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class);
		return loader.findFirst();
	}

	private FrameworkFactoryLocator() {
		// hidden constructor
	}
}
