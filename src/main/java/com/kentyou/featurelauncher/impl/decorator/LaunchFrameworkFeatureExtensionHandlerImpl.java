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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kentyou.featurelauncher.impl.repository.FileSystemArtifactRepository;

/**
 * Implementation of {@link com.kentyou.featurelauncher.impl.decorator.LaunchFrameworkFeatureExtensionHandler}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
public class LaunchFrameworkFeatureExtensionHandlerImpl implements LaunchFrameworkFeatureExtensionHandler {
	private static final Logger LOG = LoggerFactory.getLogger(LaunchFrameworkFeatureExtensionHandlerImpl.class);

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler#handle(org.osgi.service.feature.Feature, org.osgi.service.feature.FeatureExtension, org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler.FeatureExtensionHandlerBuilder, org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory)
	 */
	@Override
	public Feature handle(Feature feature, FeatureExtension extension,
			FeatureExtensionHandlerBuilder decoratedFeatureBuilder, DecoratorBuilderFactory factory)
			throws AbandonOperationException {
		/**
		 * FIXME: There is a problem with how the Feature Launcher API currently expects
		 * feature extensions ( {@link org.osgi.service.feature.FeatureExtension } to be
		 * processed via feature extension handlers
		 * {@link org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler} -
		 * at least for the built-in feature extensions ( i.e.
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.LAUNCH_FRAMEWORK},
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.FRAMEWORK_LAUNCHING_PROPERTIES}
		 * and
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.BUNDLE_START_LEVELS}
		 * ), information contained in those extensions is required at different stages
		 * of launching / installing feature and cannot be passed back to calling code
		 * in feature {@link org.osgi.service.feature.Feature} object, which is the only
		 * object returned from the only method
		 * {@link org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler.handle(Feature,
		 * FeatureExtension, FeatureExtensionHandlerBuilder, DecoratorBuilderFactory)}
		 * defined on feature extension handler.
		 * 
		 * 1. In the case of
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.LAUNCH_FRAMEWORK}
		 * feature extension, information contained in that extension is required when
		 * locating framework implementation, as defined in section 160.4.3.2. To enable
		 * passing back required information, i.e. instance of
		 * {@link org.osgi.framework.launch.FrameworkFactory}, to where it is needed
		 * during the process of launching feature, necessitated defining additional
		 * interface
		 * ({@link com.kentyou.featurelauncher.impl.decorator.LaunchFrameworkFeatureExtensionHandler}
		 * and calling that additional method defined on that interface when locating
		 * framework implementation from (
		 * {@link com.kentyou.featurelauncher.impl.FrameworkFactoryLocator.locateFrameworkFactory(Feature,
		 * List<ArtifactRepository>)} ) method.
		 * 
		 * 2. In the case of
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.FRAMEWORK_LAUNCHING_PROPERTIES}
		 * feature extension, information contained in that extension is required when
		 * creating framework instance via
		 * {@link org.osgi.framework.launch.FrameworkFactory.newFramework(Map<String,
		 * String>)} method - it cannot be passed back in "processed" feature
		 * {@link org.osgi.service.feature.Feature} object as it is currently defined by
		 * separate OSGi specification ( 159 ), and even if it were, the calling code
		 * would have to check again and extract this information so these user-defined
		 * framework properties can be used when creating new instance of framework.
		 * 
		 * As a side note, this extension appears obsolete when processing extensions in
		 * {@link org.osgi.service.featurelauncher.runtime.FeatureRuntime}
		 * implementation and is only relevant when processing extensions in
		 * {@link org.osgi.service.featurelauncher.FeatureLauncher} implementation.
		 * 
		 * 3. In the case of
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.BUNDLE_START_LEVELS}
		 * feature extension, at least some of the information contained in that
		 * extension, i.e. `org.osgi.framework.startlevel` framework launch property, is
		 * required when creating framework instance via
		 * {@link org.osgi.framework.launch.FrameworkFactory.newFramework(Map<String,
		 * String>)} method, similar to
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.FRAMEWORK_LAUNCHING_PROPERTIES}
		 * feature extension ( see item 2 above ).
		 * 
		 * In addition, similarly to
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.FRAMEWORK_LAUNCHING_PROPERTIES}
		 * extension ( see item 2 above ), it is only relevant to
		 * {@link org.osgi.service.featurelauncher.FeatureLauncher} implementation.
		 * 
		 * 
		 * As can be seen, at least for these built-in extensions, since required
		 * information cannot be passed back to calling code and is required at
		 * different stages of processing, these extensions cannot be processed via
		 * {@link com.kentyou.featurelauncher.impl.util.FeatureExtensionUtil.executeFeatureExtensionHandlers(Feature,
		 * Map<String, FeatureExtensionHandler>)}.
		 * 
		 * One possible solution would be to introduce additional parameter to
		 * {@link org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler.handle(Feature,
		 * FeatureExtension, FeatureExtensionHandlerBuilder, DecoratorBuilderFactory)}
		 * method, which "collects" such information ( e.g. instance of
		 * {@link org.osgi.framework.launch.FrameworkFactory}, framework launching
		 * properties, etc. ) which can then be utilized by calling code when it's
		 * actually needed.
		 **/
		// so, just return the original feature for now
		return feature;
	}

	/* 
	 * (non-Javadoc)
	 * @see com.kentyou.featurelauncher.impl.decorator.LaunchFrameworkFeatureExtensionHandler#selectFrameworkFactory(org.osgi.service.feature.FeatureExtension, java.util.List, java.util.Optional)
	 */
	@Override
	public Optional<FrameworkFactory> selectFrameworkFactory(FeatureExtension featureExtension,
			List<ArtifactRepository> artifactRepositories, Optional<FrameworkFactory> defaultFrameworkFactoryOptional) {
		Optional<FrameworkFactory> selectedFrameworkFactoryOptional = Optional.empty();

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

	private Path getArtifactPath(ID artifactId, List<ArtifactRepository> artifactRepositories) {
		for (ArtifactRepository artifactRepository : artifactRepositories) {
			Path artifactPath = ((FileSystemArtifactRepository) artifactRepository).getArtifactPath(artifactId);
			if (artifactPath != null) {
				return artifactPath;
			}
		}

		return null;
	}

	private URL constructArtifactJarFileURL(Path jarFilePath) {
		try {
			return jarFilePath.toUri().toURL();
		} catch (MalformedURLException e) {
			LOG.error("Error constructing URL for JAR artifact!", e);
			throw new RuntimeException(e);
		}
	}
}
