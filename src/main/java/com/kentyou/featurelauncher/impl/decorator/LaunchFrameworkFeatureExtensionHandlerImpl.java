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
			throw new RuntimeException("Error constructing URL for JAR artifact!", e);
		}
	}
}
