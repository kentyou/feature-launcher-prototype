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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
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
import com.kentyou.featurelauncher.impl.util.FileSystemUtil;
import com.kentyou.featurelauncher.impl.util.FrameworkEventUtil;
import com.kentyou.featurelauncher.impl.util.ServiceLoaderUtil;

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

		FeatureService featureService = ServiceLoaderUtil.loadFeatureService();

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
		private List<ArtifactRepository> artifactRepositories;
		private Map<String, Object> configuration;
		private Map<String, Object> variables;
		private Map<String, String> frameworkProps;
		private List<FeatureDecorator> decorators;
		private Map<String, FeatureExtensionHandler> extensionHandlers;
		private FeatureLauncherConfigurationManager featureConfigurationManager;

		LaunchBuilderImpl(Feature feature) {
			Objects.requireNonNull(feature, "Feature cannot be null!");

			this.feature = feature;
			this.isLaunched = false;
			this.installedBundles = new ArrayList<>();
			this.artifactRepositories = new ArrayList<>();
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

			this.artifactRepositories.add(repository);

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

			this.configuration = Map.copyOf(configuration);

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

			this.variables = Map.copyOf(variables);

			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#withFrameworkProperties(java.util.Map)
		 */
		@Override
		public LaunchBuilder withFrameworkProperties(Map<String, String> frameworkProps) {
			Objects.requireNonNull(frameworkProps, "Framework launch properties cannot be null!");

			ensureNotLaunchedYet();

			this.frameworkProps = Map.copyOf(frameworkProps);

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

			if (this.artifactRepositories.isEmpty()) {
				LOG.error("At least one Artifact Repository is required!");
				throw new NullPointerException("At least one Artifact Repository is required!");
			}

			ensureNotLaunchedYet();
			this.isLaunched = true;

			//////////////////////////////////////
			// TODO: 160.4.3.1: Feature Decoration

			/////////////////////////////////////////////////
			// 160.4.3.2: Locating a framework implementation
			FrameworkFactory frameworkFactory = FrameworkFactoryLocator.locateFrameworkFactory(feature,
					artifactRepositories);

			///////////////////////////////////////////
			// 160.4.3.3: Creating a Framework instance
			Framework framework = createFramework(frameworkFactory, frameworkProps);

			/////////////////////////////////////////////////////////
			// 160.4.3.4: Installing bundles and configurations
			installBundles(framework);

			createConfigurationAdminTrackerIfNeeded(framework.getBundleContext());

			//////////////////////////////////////////
			// 160.4.3.5: Starting the framework
			startFramework(framework);

			try {
				waitForConfigurationAdminTrackerIfNeeded(
						FeatureLauncherConfigurationManager.CONFIGURATION_TIMEOUT_DEFAULT);
			} finally {
				stopConfigurationAdminTracker();
			}

			return framework;
		}

		private Framework createFramework(FrameworkFactory frameworkFactory, Map<String, String> frameworkProperties) {
			Framework framework = frameworkFactory.newFramework(frameworkProperties);
			try {
				framework.init();

				addLogListeners(framework);

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

				// TODO: once start levels are involved, bundles can be started transiently,
				// before call to framework.start()
				startBundles();

			} catch (BundleException | InterruptedException e) {
				////////////////////////////////////
				// 160.4.3.6: Cleanup after failure
				cleanup(framework);

				LOG.error("Could not start framework!", e);
				throw new LaunchException("Could not start framework!", e);
			}
		}

		private void startBundles() throws BundleException, InterruptedException {
			for (Bundle installedBundle : installedBundles) {
				LOG.debug("Starting bundle {}", installedBundle);
				startBundle(installedBundle);
			}
		}

		private void startBundle(Bundle installedBundle) throws BundleException, InterruptedException {
			if (installedBundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
				installedBundle.start();
			}
		}

		private void createConfigurationAdminTrackerIfNeeded(BundleContext bundleContext) {
			if (featureConfigurationManager == null && !feature.getConfigurations().isEmpty()) {
				featureConfigurationManager = new FeatureLauncherConfigurationManager(bundleContext,
						feature.getConfigurations());

				LOG.info(String.format("Started ConfigurationAdmin service tracker for bundle '%s'",
						bundleContext.getBundle().getSymbolicName()));
			}
		}

		private void waitForConfigurationAdminTrackerIfNeeded(long timeout) {
			if (featureConfigurationManager != null) {
				featureConfigurationManager.waitForService(timeout);

				LOG.info("'ConfigurationAdmin' service is available!");
			}
		}

		private void stopConfigurationAdminTracker() {
			if (featureConfigurationManager != null) {
				featureConfigurationManager.stop();
				featureConfigurationManager = null;

				LOG.info("Stopped ConfigurationAdmin service tracker");
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

			try (InputStream featureBundleIs = getArtifact(featureBundleID)) {
				if (featureBundleIs.available() != 0) {
					Bundle installedBundle = framework.getBundleContext().installBundle(featureBundleID.toString(),
							featureBundleIs);
					installedBundles.add(installedBundle);

					LOG.info(String.format("Installed bundle '%s'", installedBundle.getSymbolicName()));
				}
			} catch (IOException | BundleException e) {
				LOG.error(String.format("Could not install bundle '%s'!", featureBundleID.toString()), e);
				throw new LaunchException(String.format("Could not install bundle '%s'!", featureBundleID.toString()),
						e);
			}
		}

		private InputStream getArtifact(ID featureBundleID) {
			for (ArtifactRepository artifactRepository : artifactRepositories) {
				InputStream featureBundleIs = artifactRepository.getArtifact(featureBundleID);
				if (featureBundleIs != null) {
					return featureBundleIs;
				}
			}

			return InputStream.nullInputStream();
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

			// Stopping the framework will stop all of the bundles
			try {
				framework.stop();
				framework.waitForStop(0);
			} catch (BundleException | InterruptedException e) {
				LOG.error("A problem occurred while cleaning up the framework", e);
			}

			Collections.reverse(installedBundles);

			if (!installedBundles.isEmpty()) {
				Iterator<Bundle> installedBundlesIt = installedBundles.iterator();
				while (installedBundlesIt.hasNext()) {
					Bundle installedBundle = installedBundlesIt.next();

					try {
						if (installedBundle.getState() != Bundle.UNINSTALLED) {
							installedBundle.stop();
							installedBundle.uninstall();
							LOG.info(String.format("Uninstalled bundle '%s'", installedBundle.getSymbolicName()));
						}

						installedBundlesIt.remove();

					} catch (BundleException exc) {
						LOG.error(String.format("Cannot uninstall bundle '%s'", installedBundle.getSymbolicName()));
					}
				}
			}

			// Cleaning up the storage area will uninstall any bundles left over, but we
			// must
			// only do it if the storage area is allowed to be cleaned at startup
			if (frameworkProps.containsKey(Constants.FRAMEWORK_STORAGE) && Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT
					.equals(frameworkProps.get(Constants.FRAMEWORK_STORAGE_CLEAN))) {
				try {
					FileSystemUtil.recursivelyDelete(
							Paths.get(String.valueOf(frameworkProps.get(Constants.FRAMEWORK_STORAGE))));
				} catch (IOException e) {
					LOG.warn("Could not delete framework storage area!", e);
				}
			}
		}

		private void ensureNotLaunchedYet() {
			if (this.isLaunched == true) {
				LOG.error("Framework already launched!");
				throw new IllegalStateException("Framework already launched!");
			}
		}
	}
}
