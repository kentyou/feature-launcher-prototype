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

import static com.kentyou.featurelauncher.impl.FeatureLauncherConfigurationManager.CONFIGURATION_TIMEOUT_DEFAULT;
import static com.kentyou.featurelauncher.impl.FeatureLauncherImplConstants.CONFIGURATION_ADMIN_IMPL_DEFAULT;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.BUNDLE_START_LEVELS;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.BUNDLE_START_LEVEL_METADATA;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.CONFIGURATION_TIMEOUT;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.FRAMEWORK_LAUNCHING_PROPERTIES;

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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.FeatureLauncher;
import org.osgi.service.featurelauncher.LaunchException;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kentyou.featurelauncher.impl.decorator.BundleStartLevelsFeatureExtensionHandler;
import com.kentyou.featurelauncher.impl.decorator.FrameworkLaunchingPropertiesFeatureExtensionHandler;
import com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryFactoryImpl;
import com.kentyou.featurelauncher.impl.util.BundleEventUtil;
import com.kentyou.featurelauncher.impl.util.DecorationUtil;
import com.kentyou.featurelauncher.impl.util.FileSystemUtil;
import com.kentyou.featurelauncher.impl.util.FrameworkEventUtil;
import com.kentyou.featurelauncher.impl.util.ServiceLoaderUtil;
import com.kentyou.featurelauncher.impl.util.VariablesUtil;

/**
 * 160.4 The Feature Launcher
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
public class FeatureLauncherImpl extends ArtifactRepositoryFactoryImpl implements FeatureLauncher {
	private static final Logger LOG = LoggerFactory.getLogger(FeatureLauncherImpl.class);
	
	private final FeatureService featureService;
	
	public FeatureLauncherImpl() {
		this.featureService = ServiceLoaderUtil.loadFeatureService();
	}

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

		try {
			Feature feature = featureService.readFeature(jsonReader);

			return launch(feature);

		} catch (IOException e) {
			throw new LaunchException("Error reading feature!", e);
		}
	}

	class LaunchBuilderImpl implements LaunchBuilder {
		private Feature feature;
		private boolean isLaunched;
		private List<Bundle> installedBundles;
		private List<ArtifactRepository> artifactRepositories;
		private Map<String, Object> configuration;
		private Map<String, Object> variables;
		private Map<String, String> frameworkProps;
		private List<FeatureDecorator> decorators;
		private Map<String, FeatureExtensionHandler> extensionHandlers;
		private FeatureLauncherConfigurationManager featureConfigurationManager;
		private long configurationTimeout;

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
			this.configurationTimeout = CONFIGURATION_TIMEOUT_DEFAULT;
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
				throw new NullPointerException("At least one Artifact Repository is required!");
			}

			ensureNotLaunchedYet();

			this.isLaunched = true;

			//////////////////////////////////////
			// 160.4.3.1: Feature Decoration
			try {
				feature = DecorationUtil.executeFeatureDecorators(featureService, feature, decorators);

				feature = DecorationUtil.executeFeatureExtensionHandlers(featureService, feature, extensionHandlers);
			} catch (AbandonOperationException e) {
				throw new LaunchException("Feature decoration handling failed!", e);
			}

			/////////////////////////////////////////////////
			// 160.4.3.2: Locating a framework implementation
			FrameworkFactory frameworkFactory = FrameworkFactoryLocator.locateFrameworkFactory(feature,
					artifactRepositories);

			///////////////////////////////////////////
			// 160.4.3.3: Creating a Framework instance
			Framework framework = createFramework(frameworkFactory, mergeFrameworkProperties());

			/////////////////////////////////////////////////////////
			// 160.4.3.4: Installing bundles and configurations
			installBundles(framework);

			maybeCreateConfigurationAdminTracker(framework.getBundleContext());

			maybeSetCustomConfigurationTimeout();

			//////////////////////////////////////////
			// 160.4.3.5: Starting the framework
			startFramework(framework);

			maybeWaitForConfigurationAdminTracker();

			return framework;
		}

		private Map<String, String> mergeFrameworkProperties() {
			Map<String, Object> rawProperties = new HashMap<>(frameworkProps);

			if (DecorationUtil.hasFrameworkLaunchingPropertiesFeatureExtension(feature.getExtensions())) {
				try {
					FrameworkLaunchingPropertiesFeatureExtensionHandler frameworkLaunchingPropertiesFeatureExtensionHandler = DecorationUtil
							.getBuiltInHandlerForExtension(FRAMEWORK_LAUNCHING_PROPERTIES);

					rawProperties.putAll(frameworkLaunchingPropertiesFeatureExtensionHandler.getFrameworkProperties());

				} catch (AbandonOperationException e) {
					throw new LaunchException(e.getMessage(), e);
				}
			}

			Map<String, Object> properties = VariablesUtil.INSTANCE.maybeSubstituteVariables(rawProperties,
					mergeVariables());

			return properties.entrySet().stream()
					.collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));
		}

		private Map<String, Object> mergeVariables() {
			Map<String, Object> allVariables = new HashMap<>(feature.getVariables());

			if (!variables.isEmpty()) {
				allVariables.putAll(variables);
			}

			ensureVariablesNotNull(allVariables);

			return allVariables;
		}

		private void ensureVariablesNotNull(Map<String, Object> allVariables) {
			for (Map.Entry<String, Object> variable : allVariables.entrySet()) {
				if (variable.getValue() == null) {
					throw new LaunchException(String.format("No value provided for variable %s!", variable.getKey()));
				}
			}
		}

		private Framework createFramework(FrameworkFactory frameworkFactory, Map<String, String> frameworkProperties) {
			Framework framework = frameworkFactory.newFramework(frameworkProperties);
			try {
				framework.init();

				maybeSetInitialBundleStartLevel(framework);

				addLogListeners(framework);

			} catch (BundleException e) {
				throw new LaunchException("Could not initialize framework!", e);
			}
			return framework;
		}

		private void maybeSetFrameworkStartLevel(Framework framework) {
			if (DecorationUtil.hasBundleStartLevelsFeatureExtension(feature.getExtensions())) {
				try {
					BundleStartLevelsFeatureExtensionHandler bundleStartLevelsFeatureExtensionHandler = DecorationUtil
							.getBuiltInHandlerForExtension(BUNDLE_START_LEVELS);

					if (bundleStartLevelsFeatureExtensionHandler.hasMinimumFrameworkStartLevel()) {
						framework.adapt(FrameworkStartLevel.class).setStartLevel(
								bundleStartLevelsFeatureExtensionHandler.getMinimumFrameworkStartLevel().intValue());
					}

				} catch (AbandonOperationException e) {
					throw new LaunchException(e.getMessage(), e);
				}
			}
		}

		private void maybeSetInitialBundleStartLevel(Framework framework) {
			if (DecorationUtil.hasBundleStartLevelsFeatureExtension(feature.getExtensions())) {
				try {
					BundleStartLevelsFeatureExtensionHandler bundleStartLevelsFeatureExtensionHandler = DecorationUtil
							.getBuiltInHandlerForExtension(BUNDLE_START_LEVELS);

					if (bundleStartLevelsFeatureExtensionHandler.hasDefaultBundleStartLevel()) {
						framework.adapt(FrameworkStartLevel.class).setInitialBundleStartLevel(
								bundleStartLevelsFeatureExtensionHandler.getDefaultBundleStartLevel().intValue());
					}

				} catch (AbandonOperationException e) {
					throw new LaunchException(e.getMessage(), e);
				}
			}
		}

		private void startFramework(Framework framework) {
			LOG.info("Starting framework..");
			try {
				framework.start();

				maybeSetFrameworkStartLevel(framework);

				maybeInstallAndStartDefaultConfigurationAdminTracker(framework.getBundleContext());

				maybeWaitForConfigurationsToBeCreated();

				startBundles();

			} catch (BundleException | InterruptedException e) {
				////////////////////////////////////
				// 160.4.3.6: Cleanup after failure
				cleanup(framework);

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

		private void maybeCreateConfigurationAdminTracker(BundleContext bundleContext) {
			if (featureConfigurationManager == null && !feature.getConfigurations().isEmpty()) {
				featureConfigurationManager = new FeatureLauncherConfigurationManager(bundleContext,
						feature.getConfigurations(), mergeVariables());

				LOG.info(String.format("Started ConfigurationAdmin service tracker for bundle '%s'",
						bundleContext.getBundle().getSymbolicName()));
			}
		}

		private void maybeInstallAndStartDefaultConfigurationAdminTracker(BundleContext bundleContext)
				throws BundleException {
			if (this.configurationTimeout == 0) {
				ID configadminImplBundleID = featureService.getIDfromMavenCoordinates(CONFIGURATION_ADMIN_IMPL_DEFAULT);
				Bundle configadminImplBundle = installBundle(bundleContext, configadminImplBundleID);
				if (configadminImplBundle != null) {
					configadminImplBundle.start();
				}
			}
		}

		private void maybeWaitForConfigurationAdminTracker() {
			if ((featureConfigurationManager != null) && (this.configurationTimeout == CONFIGURATION_TIMEOUT_DEFAULT)) {
				try {
					featureConfigurationManager.waitForService(this.configurationTimeout);

					LOG.info("'ConfigurationAdmin' service is available!");
				} finally {
					maybeStopConfigurationAdminTracker();
				}
			}
		}

		private void maybeStopConfigurationAdminTracker() {
			if (featureConfigurationManager != null) {
				featureConfigurationManager.stop();
				featureConfigurationManager = null;

				LOG.info("Stopped ConfigurationAdmin service tracker");
			}
		}

		private boolean maybeWaitForConfigurationsToBeCreated() throws InterruptedException {
			boolean waitForConfigurationsToBeCreated = false;

			if (!feature.getConfigurations().isEmpty() && (featureConfigurationManager != null)) {
				waitForConfigurationsToBeCreated = ((this.configurationTimeout == 0)
						&& (!featureConfigurationManager.serviceAdded()
								|| !featureConfigurationManager.configurationsCreated()));

				while (waitForConfigurationsToBeCreated) {

					TimeUnit.MILLISECONDS.sleep(10);

					waitForConfigurationsToBeCreated = (!featureConfigurationManager.serviceAdded()
							|| !featureConfigurationManager.configurationsCreated());

					System.out.println();
				}
			}

			return waitForConfigurationsToBeCreated;
		}

		private void maybeSetCustomConfigurationTimeout() {
			if (!this.configuration.isEmpty() && this.configuration.containsKey(CONFIGURATION_TIMEOUT)) {
				long customConfigurationTimeout = Long
						.parseLong(this.configuration.get(CONFIGURATION_TIMEOUT).toString());

				if (customConfigurationTimeout == 0 || customConfigurationTimeout == -1) {
					this.configurationTimeout = customConfigurationTimeout;
				}
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
					installBundle(framework.getBundleContext(), featureBundle);
				}

			} else {
				LOG.warn("There are no bundles to install!");
			}
		}

		private void installBundle(BundleContext bundleContext, FeatureBundle featureBundle) {
			Bundle installedBundle = installBundle(bundleContext, featureBundle.getID());

			if (installedBundle != null) {
				maybeSetBundleStartLevel(installedBundle, featureBundle.getMetadata());

				installedBundles.add(installedBundle);
			}
		}

		private Bundle installBundle(BundleContext bundleContext, ID featureBundleID) {
			try (InputStream featureBundleIs = getArtifact(featureBundleID)) {
				if (featureBundleIs.available() != 0) {
					Bundle installedBundle = bundleContext.installBundle(featureBundleID.toString(), featureBundleIs);

					LOG.info(String.format("Installed bundle '%s'", installedBundle.getSymbolicName()));

					return installedBundle;
				}
			} catch (IOException | BundleException e) {
				throw new LaunchException(String.format("Could not install bundle '%s'!", featureBundleID.toString()),
						e);
			}

			return null;
		}

		protected void maybeSetBundleStartLevel(Bundle bundle, Map<String, Object> metadata) {
			if (metadata != null && metadata.containsKey(BUNDLE_START_LEVEL_METADATA)) {
				int startlevel = Integer.valueOf(metadata.get(BUNDLE_START_LEVEL_METADATA).toString()).intValue();

				bundle.adapt(BundleStartLevel.class).setStartLevel(startlevel);
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
				throw new IllegalStateException("Framework already launched!");
			}
		}
	}
}
