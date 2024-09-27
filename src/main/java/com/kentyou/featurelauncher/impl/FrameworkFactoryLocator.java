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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.LaunchException;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kentyou.featurelauncher.impl.repository.EnhancedArtifactRepository;

/**
 * 160.4.3.2: Locating a framework implementation
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 18, 2024
 */
class FrameworkFactoryLocator {
	private static final Logger LOG = LoggerFactory.getLogger(FrameworkFactoryLocator.class);

	public static FrameworkFactory locateFrameworkFactory(Feature feature,
			List<ArtifactRepository> artifactRepositories) {
		/*
		 * TODO: "160.4.3.2: #1. If any provider specific configuration has been given to the
		 * Feature Launcher implementation then this should be used to identify the
		 * framework"
		 */

		/*
		 * "160.4.3.2: #2. If the Feature declares an Extension LAUNCH_FRAMEWORK then
		 * the Feature Launcher implementation must use the first listed artifact that
		 * can be found in any configured Artifact Repositories, as described in
		 * Selecting a framework implementation on page 99"
		 */
		if (hasLaunchFrameworkFeatureExtension(feature.getExtensions())) {
			Optional<FrameworkFactory> selectFrameworkFactoryOptional = selectFrameworkFactory(
					feature.getExtensions().get(LAUNCH_FRAMEWORK), artifactRepositories);
			if (selectFrameworkFactoryOptional.isPresent()) {
				return selectFrameworkFactoryOptional.get();
			} else if (isLaunchFrameworkFeatureExtensionMandatory(feature.getExtensions().get(LAUNCH_FRAMEWORK))) {
				throw new LaunchException("No suitable OSGi framework implementation could be selected!");
			}
		}

		/*
		 * TODO: "160.4.3.2: #3. If no framework implementation is found in the previous steps
		 * then the Feature Launcher implementation must search the classpath using the
		 * Thread Context Class Loader, or, if the Thread Context Class Loader is not
		 * set, the Class Loader which loaded the caller of the Feature Launcher's
		 * launch method. The first suitable framework instance located is the instance
		 * that will be used."
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
	 * "160.4.3.2: #2. If the Feature declares an Extension LAUNCH_FRAMEWORK then
	 * the Feature Launcher implementation must use the first listed artifact that
	 * can be found in any configured Artifact Repositories, as described in
	 * Selecting a framework implementation on page 99"
	 */
	private static Optional<FrameworkFactory> selectFrameworkFactory(FeatureExtension featureExtension,
			List<ArtifactRepository> artifactRepositories) {
		// TODO: repackage to 'LaunchFrameworkFeatureExtensionHandlerImpl' ? but, the
		// only method defined there returns Feature.. and this "Feature extension" does
		// not require pre-processing, it is FrameworkFactory that must be returned!
		Optional<FrameworkFactory> selectedFrameworkFactoryOptional = Optional.empty();

		Optional<FrameworkFactory> defaultFrameworkFactoryOptional = loadDefaultFrameworkFactory();

		for (FeatureArtifact featureArtifact : featureExtension.getArtifacts()) {
			Path artifactPath = getArtifactPath(featureArtifact.getID(), artifactRepositories);

			URL artifactJarFileURL = constructArtifactJarFileURL(artifactPath);

			try {

				URLClassLoader urlClassLoader = URLClassLoader.newInstance(new URL[] { artifactJarFileURL },
						Thread.currentThread().getContextClassLoader());

				ServiceLoader<FrameworkFactory> serviceLoader = ServiceLoader.load(FrameworkFactory.class,
						urlClassLoader);

				// @formatter:off
				List<FrameworkFactory> loadedServices = serviceLoader.stream()
						.map(p -> p.get())
						.collect(Collectors.toList());
				// @formatter:on

				if (defaultFrameworkFactoryOptional.isPresent() && loadedServices.size() > 1) {
					// @formatter:off
					loadedServices.stream()
						.filter(ff -> (ff.getClass().isInstance(defaultFrameworkFactoryOptional.get())))
						.findFirst()
						.ifPresent(loadedServices::remove);
					// @formatter:on
				}

				selectedFrameworkFactoryOptional = Optional.of(loadedServices.get(0));

				if (selectedFrameworkFactoryOptional.isPresent()) {
					LOG.info(String.format("Selected '%s' OSGi framework implementation",
							selectedFrameworkFactoryOptional.get()));
					Thread.currentThread().setContextClassLoader(urlClassLoader);
					break;
				} else {
					urlClassLoader.close();
				}
			} catch (Throwable t) {
				LOG.warn(String.format("'%s' is not an OSGi framework implementation!", featureArtifact.getID()), t);
			}
		}

		return selectedFrameworkFactoryOptional;
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

	/**
	 * 160.4.3.2: #4. In the event that no suitable OSGi framework can be found by
	 * any of the previous steps then the Feature Launcher implementation may
	 * provide a default framework implementation to be used.
	 * 
	 * @return
	 */
	private static Optional<FrameworkFactory> loadDefaultFrameworkFactory() {
		ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class);
		return loader.findFirst();
	}

	private static boolean hasLaunchFrameworkFeatureExtension(Map<String, FeatureExtension> featureExtensions) {
		if (!featureExtensions.isEmpty() && featureExtensions.containsKey(LAUNCH_FRAMEWORK)) {
			return ((featureExtensions.get(LAUNCH_FRAMEWORK).getType() == FeatureExtension.Type.ARTIFACTS)
					&& !featureExtensions.get(LAUNCH_FRAMEWORK).getArtifacts().isEmpty());
		}

		return false;
	}

	private static boolean isLaunchFrameworkFeatureExtensionMandatory(FeatureExtension featureExtension) {
		return featureExtension.getKind() == FeatureExtension.Kind.MANDATORY;
	}

	private static Path getArtifactPath(ID artifactId, List<ArtifactRepository> artifactRepositories) {
		for (ArtifactRepository artifactRepository : artifactRepositories) {
			// TODO: remove cast once missing methods are available on {@link
			// org.osgi.service.featurelauncher.repository.ArtifactRepository}
			Path artifactPath = ((EnhancedArtifactRepository) artifactRepository).getArtifactPath(artifactId);
			if (artifactPath != null) {
				return artifactPath;
			}
		}

		return null;
	}

	private static URL constructArtifactJarFileURL(Path jarFilePath) {
		try {
			return jarFilePath.toUri().toURL();
		} catch (MalformedURLException e) {
			LOG.error("Error constructing URL for JAR artifact!", e);
			throw new RuntimeException(e);
		}
	}

	private FrameworkFactoryLocator() {
		// hidden constructor
	}
}
