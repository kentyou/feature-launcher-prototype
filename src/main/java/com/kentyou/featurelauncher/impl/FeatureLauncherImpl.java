/**
 * Copyright (c) 2012 - 2024 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package com.kentyou.featurelauncher.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.FeatureLauncher;
import org.osgi.service.featurelauncher.LaunchException;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryFactoryImpl;
import com.kentyou.featurelauncher.impl.util.BundleEventUtil;
import com.kentyou.featurelauncher.impl.util.FrameworkEventUtil;
import com.kentyou.featurelauncher.impl.util.FrameworkFactoryLocator;

/**
 * 160.4 The Feature Launcher
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
public class FeatureLauncherImpl extends ArtifactRepositoryFactoryImpl implements FeatureLauncher {
	private static final Logger LOG = LoggerFactory.getLogger(FeatureLauncherImpl.class);
	
	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.FeatureLauncher#launch(org.osgi.service.feature.Feature)
	 */
	@Override
	public LaunchBuilder launch(Feature feature) {
		Objects.requireNonNull(feature, "Feature cannot be null!");

		return new LaunchBuilderImpl(feature);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.FeatureLauncher#launch(java.io.Reader)
	 */
	@Override
	public LaunchBuilder launch(Reader jsonReader) {
		Objects.requireNonNull(jsonReader, "Feature JSON cannot be null!");

		FeatureService featureService = loadFeatureService();

		try {
			Feature feature = featureService.readFeature(jsonReader);

			return launch(feature);

		} catch (IOException e) {
			LOG.error("Error reading feature!", e);
			throw new LaunchException("Error reading feature!", e);
		}
	}

	class LaunchBuilderImpl implements LaunchBuilder {
		private final Feature feature;
		private boolean isLaunched;
		private List<Bundle> installedBundles;
		private ArtifactRepository artifactRepository;
		private Map<String, Object> configuration;
		private Map<String, Object> variables;
		private Map<String, Object> frameworkProps;
		private List<FeatureDecorator> decorators;
		private Map<String, FeatureExtensionHandler> extensionHandlers;

		LaunchBuilderImpl(Feature feature) {
			Objects.requireNonNull(feature, "Feature cannot be null!");

			this.feature = feature;
			this.isLaunched = false;
			this.installedBundles = new ArrayList<>();
			this.configuration = new HashMap<>();
			this.variables = new HashMap<>();
			this.frameworkProps = new HashMap<>();
			this.decorators = new ArrayList<>();
			this.extensionHandlers = new HashMap<>();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#withRepository(org.osgi.service.featurelauncher.repository.ArtifactRepository)
		 */
		@Override
		public LaunchBuilder withRepository(ArtifactRepository repository) {
			Objects.requireNonNull(repository, "Artifact Repository cannot be null!");

			ensureNotLaunchedYet();

			this.artifactRepository = repository;

			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#withConfiguration(java.util.Map)
		 */
		@Override
		public LaunchBuilder withConfiguration(Map<String, Object> configuration) {
			Objects.requireNonNull(configuration, "Configuration cannot be null!");

			ensureNotLaunchedYet();

			this.configuration = configuration;

			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#withVariables(java.util.Map)
		 */
		@Override
		public LaunchBuilder withVariables(Map<String, Object> variables) {
			Objects.requireNonNull(variables, "Variables cannot be null!");

			ensureNotLaunchedYet();

			this.variables = variables;

			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#withFrameworkProperties(java.util.Map)
		 */
		@Override
		public LaunchBuilder withFrameworkProperties(Map<String, Object> frameworkProps) {
			Objects.requireNonNull(frameworkProps, "Framework launch properties cannot be null!");

			ensureNotLaunchedYet();

			this.frameworkProps = frameworkProps;

			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#withDecorator(org.osgi.service.featurelauncher.decorator.FeatureDecorator)
		 */
		@Override
		public LaunchBuilder withDecorator(FeatureDecorator decorator) {
			Objects.requireNonNull(decorator, "Feature Decorator cannot be null!");

			ensureNotLaunchedYet();

			this.decorators.add(decorator);

			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#withExtensionHandler(java.lang.String, org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler)
		 */
		@Override
		public LaunchBuilder withExtensionHandler(String extensionName, FeatureExtensionHandler extensionHandler) {
			Objects.requireNonNull(extensionName, "Feature extension name cannot be null!");
			Objects.requireNonNull(extensionHandler, "Feature extension handler cannot be null!");

			ensureNotLaunchedYet();

			this.extensionHandlers.put(extensionName, extensionHandler);

			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#launchFramework()
		 */
		@Override
		public Framework launchFramework() {
			Objects.requireNonNull(feature, "Feature is required!");
			Objects.requireNonNull(artifactRepository, "Artifact Repository is required!");

			ensureNotLaunchedYet();

			//////////////////////////////////////
			// TODO: 160.4.3.1: Feature Decoration

			/////////////////////////////////////////////////
			// 160.4.3.2: Locating a framework implementation

			// use default framework implementation for now
			FrameworkFactory frameworkFactory = FrameworkFactoryLocator.loadDefaultFrameworkFactory();
			Objects.requireNonNull(frameworkFactory, "Framework Factory cannot be null!");

			///////////////////////////////////////////
			// 160.4.3.3: Creating a Framework instance

			Framework framework = createFramework(frameworkFactory, Collections.emptyMap());

			addLogListeners(framework);

			/////////////////////////////////////////////////////////
			// 160.4.3.4: Installing bundles and configurations
			installBundles(framework);

			// TODO: install configurations

			//////////////////////////////////////////
			// 160.4.3.5: Starting the framework
			startFramework(framework);

			this.isLaunched = true;

			return framework;
		}

		private Framework createFramework(FrameworkFactory frameworkFactory, Map<String, String> frameworkProperties) {
			Framework framework = frameworkFactory.newFramework(frameworkProperties);
			try {
				framework.init();
			} catch (BundleException e) {
				LOG.error("Could not initialize framework!", e);
				throw new LaunchException("Could not initialize framework!", e);
			}
			return framework;
		}

		private void startFramework(Framework framework) {
			LOG.info("Starting framework..");
			try {
				framework.start();

				startBundles();

				createShutdownHook(framework);

			} catch (BundleException e) {
				////////////////////////////////////
				// 160.4.3.6: Cleanup after failure
				cleanup(framework);

				LOG.error("Could not start framework!", e);
				throw new LaunchException("Could not start framework!", e);
			}
		}

		private void startBundles() throws BundleException {
			for (Bundle installedBundle : installedBundles) {
				startBundle(installedBundle);
			}
		}

		private void startBundle(Bundle installedBundle) throws BundleException {
			if (installedBundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
				installedBundle.start();
			}
		}

		private void addLogListeners(Framework framework) {
			framework.getBundleContext().addFrameworkListener(this::logFrameworkEvent);
			framework.getBundleContext().addBundleListener(this::logBundleEvent);
		}

		private void installBundles(Framework framework) {
			if (this.feature.getBundles() != null && this.feature.getBundles().size() > 0) {

				LOG.info(String.format("There are %d bundle(s) to install", this.feature.getBundles().size()));

				for (FeatureBundle featureBundle : this.feature.getBundles()) {
					installBundle(framework, featureBundle);
				}

			} else {
				LOG.error("There are no bundles to install!");
			}
		}

		private void installBundle(Framework framework, FeatureBundle featureBundle) {
			ID featureBundleID = featureBundle.getID();

			try (InputStream featureBundleIs = artifactRepository.getArtifact(featureBundleID)) {
				Bundle installedBundle = framework.getBundleContext().installBundle(featureBundleID.toString(),
						featureBundleIs);
				installedBundles.add(installedBundle);

				LOG.info(String.format("Installed bundle '%s'", installedBundle.getSymbolicName()));

			} catch (IOException | BundleException e) {
				LOG.error(String.format("Could not install bundle '%s'!", featureBundleID.toString()), e);
				throw new LaunchException(String.format("Could not install bundle '%s'!", featureBundleID.toString()),
						e);
			}
		}

		private void logFrameworkEvent(FrameworkEvent frameworkEvent) {
			if (frameworkEvent.getType() == FrameworkEvent.ERROR) {
				LOG.error(String.format("Framework ERROR event %s", frameworkEvent.toString()));
			} else {
				LOG.info(String.format("Framework event type %s: %s",
						FrameworkEventUtil.getFrameworkEventString(frameworkEvent.getType()),
						frameworkEvent.toString()));
			}
		}

		private void logBundleEvent(BundleEvent bundleEvent) {
			LOG.info(String.format("Bundle '%s' event type %s: %s", bundleEvent.getBundle().getSymbolicName(),
					BundleEventUtil.getBundleEventString(bundleEvent.getType()), bundleEvent.toString()));
		}

		private void cleanup(Framework framework) {
			if (!installedBundles.isEmpty()) {
				Iterator<Bundle> installedBundlesIt = installedBundles.iterator();
				while (installedBundlesIt.hasNext()) {
					Bundle installedBundle = installedBundlesIt.next();

					try {
						installedBundle.uninstall();

						installedBundlesIt.remove();

						LOG.info(String.format("Uninstalled bundle '%s'", installedBundle.getSymbolicName()));
					} catch (BundleException exc) {
						LOG.error(String.format("Cannot uninstall bundle '%s'", installedBundle.getSymbolicName()));
					}
				}
			}
		}

		private void createShutdownHook(Framework framework) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						if (framework != null) {
							LOG.info("Stopping framework..");

							cleanup(framework);

							framework.stop();
							framework.waitForStop(0);
						}
					} catch (Exception e) {
						LOG.error("Error stopping framework!", e);
					}
				}
			});
		}

		private void ensureNotLaunchedYet() {
			if (this.isLaunched == true) {
				LOG.error("Framework already launched!");
				throw new IllegalStateException("Framework already launched!");
			}
		}
	}

	private FeatureService loadFeatureService() {
		ServiceLoader<FeatureService> loader = ServiceLoader.load(FeatureService.class);

		Optional<FeatureService> featureServiceOptional = loader.findFirst();
		if (featureServiceOptional.isPresent()) {
			return featureServiceOptional.get();
		} else {
			LOG.error("Error loading FeatureService!");
			throw new LaunchException("Error loading FeatureService!");
		}
	}
}
