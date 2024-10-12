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
package com.kentyou.featurelauncher.impl.runtime;

import static org.osgi.service.feature.FeatureExtension.Kind.MANDATORY;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.runtime.FeatureRuntime;
import org.osgi.service.featurelauncher.runtime.FeatureRuntimeException;
import org.osgi.service.featurelauncher.runtime.InstalledBundle;
import org.osgi.service.featurelauncher.runtime.InstalledConfiguration;
import org.osgi.service.featurelauncher.runtime.InstalledFeature;
import org.osgi.service.featurelauncher.runtime.RuntimeBundleMerge;
import org.osgi.service.featurelauncher.runtime.RuntimeConfigurationMerge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryFactoryImpl;
import com.kentyou.featurelauncher.impl.repository.FileSystemArtifactRepository;
import com.kentyou.featurelauncher.impl.util.ArtifactRepositoryUtil;

/**
 * 160.5 The Feature Runtime Service
 * 
 * Some parts based on {@link org.eclipse.sensinact.gateway.launcher.FeatureLauncher}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
@Component
public class FeatureRuntimeImpl extends ArtifactRepositoryFactoryImpl implements FeatureRuntime {
	private static final Logger LOG = LoggerFactory.getLogger(FeatureRuntimeImpl.class);

	@Reference
	FeatureService featureService;

	@Reference
	FeatureRuntimeConfigurationManager featureRuntimeConfigurationManager;

	private BundleContext bundleContext;

	private final Path defaultM2RepositoryPath;
	private final Map<String, ArtifactRepository> defaultArtifactRepositories;

	// Bundles installed by this feature runtime
	private final Map<ID, Bundle> installedBundlesByIdentifier;

	// Lists of artifacts for each feature installed
	private final Map<ID, List<ID>> installedFeaturesToBundles;

	// List of configurations for each feature installed
	private final Map<ID, Collection<String>> installedFeaturesConfigurations;

	// List of installed features
	private final List<InstalledFeature> installedFeatures;

	// List of symbolic names of bundles already present in running framework
	private final List<String> existingBundlesSymbolicNames;

	@Activate
	public FeatureRuntimeImpl(BundleContext context) {
		this.bundleContext = context;

		try {
			this.defaultM2RepositoryPath = ArtifactRepositoryUtil.getDefaultM2RepositoryPath();

			// set up default repositories - one local repository ( .m2 ), one remote
			// repository ( Maven Central )
			this.defaultArtifactRepositories = ArtifactRepositoryUtil.getDefaultArtifactRepositories(this, defaultM2RepositoryPath);

			// collect symbolic names of bundles already present in running framework
			this.existingBundlesSymbolicNames = getExistingBundlesSymbolicNames();

		} catch (IOException e) {
			throw new FeatureRuntimeException("Could not create default artifact repositories!");
		}

		this.installedBundlesByIdentifier = new HashMap<>();
		this.installedFeaturesToBundles = new HashMap<>();
		this.installedFeaturesConfigurations = new HashMap<>();
		this.installedFeatures = new ArrayList<>();

		LOG.info("Started FeatureRuntime!");
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#getDefaultRepositories()
	 */
	@Override
	public Map<String, ArtifactRepository> getDefaultRepositories() {
		return defaultArtifactRepositories;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#install(org.osgi.service.feature.Feature)
	 */
	@Override
	public InstallOperationBuilder install(Feature feature) {
		Objects.requireNonNull(feature, "Feature cannot be null!");

		return new InstallOperationBuilderImpl(feature);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#install(java.io.Reader)
	 */
	@Override
	public InstallOperationBuilder install(Reader jsonReader) {
		Objects.requireNonNull(jsonReader, "Feature JSON cannot be null!");

		try {
			Feature feature = featureService.readFeature(jsonReader);

			return install(feature);

		} catch (IOException e) {
			LOG.error("Error reading feature!", e);
			throw new FeatureRuntimeException("Error reading feature!", e);
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#getInstalledFeatures()
	 */
	@Override
	public List<InstalledFeature> getInstalledFeatures() {
		return installedFeatures;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#remove(org.osgi.service.feature.ID)
	 */
	@Override
	public void remove(ID featureId) {
		removeFeature(featureId);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#update(org.osgi.service.feature.ID, org.osgi.service.feature.Feature)
	 */
	@Override
	public UpdateOperationBuilder update(ID featureId, Feature feature) {
		Objects.requireNonNull(featureId, "Feature ID cannot be null!");
		Objects.requireNonNull(feature, "Feature cannot be null!");

		return new UpdateOperationBuilderImpl(feature);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#update(org.osgi.service.feature.ID, java.io.Reader)
	 */
	@Override
	public UpdateOperationBuilder update(ID featureId, Reader jsonReader) {
		Objects.requireNonNull(featureId, "Feature ID cannot be null!");
		Objects.requireNonNull(jsonReader, "Feature JSON cannot be null!");

		try {
			Feature feature = featureService.readFeature(jsonReader);

			return update(featureId, feature);

		} catch (IOException e) {
			LOG.error("Error reading feature!", e);
			throw new FeatureRuntimeException("Error reading feature!", e);
		}
	}

	abstract class AbstractOperationBuilderImpl<T extends OperationBuilder<T>> implements OperationBuilder<T> {
		private final Feature feature;
		private boolean isCompleted;
		private boolean useDefaultRepositories;
		private Map<String, ArtifactRepository> artifactRepositories;
		private RuntimeBundleMerge runtimeBundleMerge;
		private RuntimeConfigurationMerge runtimeConfigurationMerge;
		private Map<String, Object> variables;
		private List<FeatureDecorator> decorators;
		private Map<String, FeatureExtensionHandler> extensionHandlers;

		public AbstractOperationBuilderImpl(Feature feature) {
			Objects.requireNonNull(feature, "Feature cannot be null!");

			this.feature = feature;
			this.isCompleted = false;
			this.useDefaultRepositories = true;
			this.artifactRepositories = getDefaultRepositories();
			this.variables = new HashMap<>();
			this.decorators = new ArrayList<>();
			this.extensionHandlers = new HashMap<>();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#addRepository(java.lang.String, org.osgi.service.featurelauncher.repository.ArtifactRepository)
		 */
		@Override
		public T addRepository(String name, ArtifactRepository repository) {
			Objects.requireNonNull(name, "Artifact Repository name cannot be null!");
			Objects.requireNonNull(repository, "Artifact Repository cannot be null!");

			ensureNotCompletedYet();

			maybeResetDefaultRepositories();

			this.artifactRepositories.put(name, repository);

			return castThis();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#useDefaultRepositories(boolean)
		 */
		@Override
		public T useDefaultRepositories(boolean include) {
			ensureNotCompletedYet();

			this.useDefaultRepositories = include;

			maybeResetDefaultRepositories();

			return castThis();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#withBundleMerge(org.osgi.service.featurelauncher.runtime.RuntimeBundleMerge)
		 */
		@Override
		public T withBundleMerge(RuntimeBundleMerge merge) {
			Objects.requireNonNull(merge, "Runtime bundle merge cannot be null!");

			ensureNotCompletedYet();

			this.runtimeBundleMerge = merge;

			return castThis();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#withConfigurationMerge(org.osgi.service.featurelauncher.runtime.RuntimeConfigurationMerge)
		 */
		@Override
		public T withConfigurationMerge(RuntimeConfigurationMerge merge) {
			Objects.requireNonNull(merge, "Runtime configuration merge cannot be null!");

			ensureNotCompletedYet();

			this.runtimeConfigurationMerge = merge;

			return castThis();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#withVariables(java.util.Map)
		 */
		@Override
		public T withVariables(Map<String, Object> variables) {
			Objects.requireNonNull(variables, "Variables cannot be null!");

			ensureNotCompletedYet();

			this.variables = variables;

			return castThis();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#withDecorator(org.osgi.service.featurelauncher.decorator.FeatureDecorator)
		 */
		@Override
		public T withDecorator(FeatureDecorator decorator) {
			Objects.requireNonNull(decorator, "Feature Decorator cannot be null!");

			ensureNotCompletedYet();

			this.decorators.add(decorator);

			return castThis();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#withExtensionHandler(java.lang.String, org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler)
		 */
		@Override
		public T withExtensionHandler(String extensionName, FeatureExtensionHandler extensionHandler) {
			Objects.requireNonNull(extensionName, "Feature extension name cannot be null!");
			Objects.requireNonNull(extensionHandler, "Feature extension handler cannot be null!");

			ensureNotCompletedYet();

			this.extensionHandlers.put(extensionName, extensionHandler);

			return castThis();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#complete()
		 */
		@Override
		public InstalledFeature complete() throws FeatureRuntimeException {
			this.isCompleted = true;

			return addOrUpdateFeature(feature);
		}

		protected InstalledFeature addOrUpdateFeature(Feature feature) {
			ID featureId = feature.getID();

			validateFeatureExtensions(feature);

			// @formatter:off
	        List<ID> featureBundles = feature.getBundles().stream()
	        		.map(featureBundle -> featureBundle.getID())
	        		.collect(Collectors.toList());
	        // @formatter:on

			// Check if feature is already installed or out of date
			if (installedFeaturesToBundles.containsKey(featureId)) {
				LOG.info(String.format("Updating feature %s", featureId));

				if (installedFeaturesToBundles.get(featureId).equals(featureBundles)) {
					// No work to do, already installed
					LOG.info(String.format("The feature %s is already up to date", featureId));

					return getInstalledFeatureById(featureId);
				} else {
					// Feature is out of date - remove and re-install
					LOG.info(String.format("The feature %s is out of date and will be removed and re-installed",
							featureId));

					removeFeature(featureId);
				}
			}

			// Install bundles
			List<InstalledBundle> installedBundles = installBundles(featureId, featureBundles);
			installedFeaturesToBundles.put(featureId, featureBundles);

			// Create configurations
			featureRuntimeConfigurationManager.createConfigurations(List.copyOf(feature.getConfigurations().values()));

			// Start bundles
			startBundles(featureId, installedBundles);

			boolean isInitialLaunch = false; // TODO: check if feature was installed by FeatureLauncher

			InstalledFeature installedFeature = constructInstalledFeature(feature.getID(), isInitialLaunch,
					installedBundles, constructInstalledConfigurations(feature));
			installedFeatures.add(installedFeature);

			return installedFeature;
		}

		protected List<InstalledBundle> installBundles(ID featureId, List<ID> featureBundles) {
			List<InstalledBundle> installedBundles = new ArrayList<>();
			for (FeatureBundle featureBundle : feature.getBundles()) {
				ID bundleId = featureBundle.getID();

				boolean bundleAlreadyPresentInRunningFramework = duplicatesExistingBundle(bundleId);
				boolean bundleAlreadyInstalledByRuntime = installedBundlesByIdentifier.containsKey(bundleId);

				if (!bundleAlreadyPresentInRunningFramework && !bundleAlreadyInstalledByRuntime) {
					Bundle bundle = installBundle(bundleId);
					if (bundle != null) {
						installedBundlesByIdentifier.put(bundleId, bundle);

						// TODO: check for and include other "owning features"
						installedBundles.add(constructInstalledBundle(bundleId, bundle, List.of(featureId)));
					}
				} else {
					if (bundleAlreadyPresentInRunningFramework) {
						LOG.warn(String.format(
								"Bundle %s duplicates bundle already present in running framework! Skipping..",
								bundleId));
					} else if (bundleAlreadyInstalledByRuntime) {
						LOG.warn(String.format(
								"Bundle %s duplicates bundle already installed by feature runtime! Skipping..",
								bundleId));
					}

					// bundle was not installed after all, so we remove it from from list of feature
					// bundles
					featureBundles.remove(bundleId);
				}
			}

			return installedBundles;
		}

		protected void startBundles(ID featureId, List<InstalledBundle> installedBundles) {
			for (InstalledBundle installedBundle : installedBundles) {
				try {
					BundleRevision rev = installedBundle.getBundle().adapt(BundleRevision.class);
					if (rev != null && (rev.getTypes() & BundleRevision.TYPE_FRAGMENT) == 0) {
						// Start all but fragment bundles
						installedBundle.getBundle().start();
					} else {
						LOG.info(String.format("Not starting bundle %s as it is a fragment",
								installedBundle.getBundle().getSymbolicName()));
					}
				} catch (Exception e) {
					LOG.warn(String.format("An error occurred starting a bundle in feature %s", featureId));
				}
			}
		}

		protected InstalledFeature getInstalledFeatureById(ID featureId) {
			// @formatter:off
			return installedFeatures.stream()
					.filter(f -> featureId.equals(f.getFeatureId()))
					.findFirst()
					.orElse(null);
			// @formatter:on
		}

		protected InstalledFeature constructInstalledFeature(ID featureId, boolean isInitialLaunch,
				List<InstalledBundle> installedBundles, List<InstalledConfiguration> installedConfigurations) {
			// @formatter:off
			return new InstalledFeatureImpl(
					featureId, 
					isInitialLaunch,
					installedBundles,
					installedConfigurations);
			// @formatter:on
		}

		protected InstalledBundle constructInstalledBundle(ID bundleId, Bundle bundle, List<ID> owningFeatures) {
			return new InstalledBundleImpl(bundleId, Collections.emptyList(), bundle, 1, owningFeatures); // TODO:
																											// aliases
		}

		protected List<InstalledConfiguration> constructInstalledConfigurations(Feature feature) {
			// @formatter:off
			return feature.getConfigurations().values().stream()
					.map(fc -> constructInstalledConfiguration(fc, List.of(feature.getID())))
					.toList();
			// @formatter:on
		}

		protected InstalledConfiguration constructInstalledConfiguration(FeatureConfiguration featureConfiguration,
				List<ID> owningFeatures) {
			return new InstalledConfigurationImpl(featureConfiguration.getPid(), featureConfiguration.getFactoryPid(),
					featureConfiguration.getValues(), owningFeatures);
		}

		protected Bundle installBundle(ID featureBundleID) {
			try (InputStream featureBundleIs = getArtifact(featureBundleID)) {
				if (featureBundleIs.available() != 0) {
					Bundle installedBundle = bundleContext.installBundle(featureBundleID.toString(), featureBundleIs);

					LOG.info(String.format("Installed bundle '%s'", installedBundle.getSymbolicName()));

					return installedBundle;
				}
			} catch (IOException | BundleException e) {
				throw new FeatureRuntimeException(String.format("Could not install bundle '%s'!", featureBundleID), e);
			}

			return null;
		}

		protected boolean duplicatesExistingBundle(ID featureBundleID) {
			Path featureBundlePath = getArtifactPath(featureBundleID);
			if (featureBundlePath != null) {
				try {
					JarFile featureBundleJarFile = new JarFile(featureBundlePath.toFile());

					Manifest featureBundleJarMf = featureBundleJarFile.getManifest();

					if ((featureBundleJarMf != null) && (featureBundleJarMf.getMainAttributes() != null)) {
						// TODO: take into account bundle version as well, if different versions of same
						// bundle should be supported
						String featureBundleSymbolicName = featureBundleJarMf.getMainAttributes()
								.getValue("Bundle-SymbolicName");
						if (featureBundleSymbolicName != null) {
							return existingBundlesSymbolicNames.contains(featureBundleSymbolicName);
						}
					}

				} catch (IOException e) {
					LOG.error(String.format("Error checking for if bundle %s duplicates existing bundles!",
							featureBundleID), e);
				}
			}

			return false;
		}

		protected Path getArtifactPath(ID featureBundleID) {
			for (ArtifactRepository artifactRepository : artifactRepositories.values()) {
				if (FileSystemArtifactRepository.class.isInstance(artifactRepository)) {
					Path featureBundlePath = ((FileSystemArtifactRepository) artifactRepository)
							.getArtifactPath(featureBundleID);
					if (featureBundlePath != null) {
						return featureBundlePath;
					}
				}
			}

			return null;
		}

		protected InputStream getArtifact(ID featureBundleID) {
			for (ArtifactRepository artifactRepository : artifactRepositories.values()) {
				InputStream featureBundleIs = artifactRepository.getArtifact(featureBundleID);
				if (featureBundleIs != null) {
					return featureBundleIs;
				}
			}

			return InputStream.nullInputStream();
		}

		protected void validateFeatureExtensions(Feature feature) {
			List<String> unknownMandatoryFeatureExtensions = feature.getExtensions().entrySet().stream()
					.filter(e -> e.getValue().getKind() == MANDATORY).map(Map.Entry::getKey)
					.collect(Collectors.toList());
			if (!unknownMandatoryFeatureExtensions.isEmpty()) {
				throw new FeatureRuntimeException(
						String.format("The feature %d has mandatory extensions for which are not understood",
								unknownMandatoryFeatureExtensions.size()));
			}
		}

		private void maybeResetDefaultRepositories() {
			if (!this.useDefaultRepositories) {
				this.artifactRepositories = new HashMap<>();
			}
		}

		protected void ensureNotCompletedYet() {
			if (this.isCompleted == true) {
				throw new IllegalStateException("Operation already completed!");
			}
		}

		@SuppressWarnings("unchecked")
		protected T castThis() {
			return (T) this;
		}
	}

	public class InstallOperationBuilderImpl extends AbstractOperationBuilderImpl<InstallOperationBuilder>
			implements InstallOperationBuilder {

		public InstallOperationBuilderImpl(Feature feature) {
			super(feature);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.InstallOperationBuilder#install()
		 */
		@Override
		public InstalledFeature install() {
			return complete();
		}
	}

	public class UpdateOperationBuilderImpl extends AbstractOperationBuilderImpl<UpdateOperationBuilder>
			implements UpdateOperationBuilder {

		public UpdateOperationBuilderImpl(Feature feature) {
			super(feature);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.UpdateOperationBuilder#update()
		 */
		@Override
		public InstalledFeature update() {
			return complete();
		}
	}

	private void removeFeatureConfigurations(ID featureId) {
		if (installedFeaturesConfigurations.containsKey(featureId)) {
			featureRuntimeConfigurationManager
					.removeConfigurations(Set.copyOf(installedFeaturesConfigurations.remove(featureId)));
		}
	}

	private void removeFeature(ID featureId) {
		Deque<ID> orderedBundleIDsForRemoval = getBundleIDsForRemoval(featureId);
		LOG.info(String.format("The following bundles %s are no longer required and will be removed.",
				Arrays.toString(orderedBundleIDsForRemoval.toArray())));

		stopBundles(orderedBundleIDsForRemoval);

		uninstallBundles(orderedBundleIDsForRemoval);

		removeFeatureConfigurations(featureId);

		installedFeatures.removeIf(f -> featureId.equals(f.getFeatureId()));
	}

	private Deque<ID> getBundleIDsForRemoval(ID featureId) {
		// Get all the bundles to remove in "install order", clearing the features map
		Set<ID> bundlesToRemove = installedFeaturesToBundles.remove(featureId).stream()
				.collect(Collectors.toCollection(LinkedHashSet::new));

		// Create a deque of bundles to remove, in the order they should be removed
		Deque<ID> orderedBundleIDsForRemoval = new LinkedList<>();
		for (ID bundleToRemove : bundlesToRemove) {
			// Only remove the bundle if no remaining features reference it
			if (installedFeaturesToBundles.values().stream().noneMatch(c -> c.contains(bundleToRemove))) {
				// Add to the start of the deque, so that we reverse the install order
				orderedBundleIDsForRemoval.addFirst(bundleToRemove);
			}
		}

		return orderedBundleIDsForRemoval;
	}

	private void stopBundles(Deque<ID> bundleIDsToStop) {
		for (ID bundleIDToStop : bundleIDsToStop) {
			Bundle bundleForRemoval = installedBundlesByIdentifier.get(bundleIDToStop);
			if (bundleForRemoval != null) {
				try {
					BundleRevision rev = bundleForRemoval.adapt(BundleRevision.class);
					if (rev != null && (rev.getTypes() & BundleRevision.TYPE_FRAGMENT) == 0) {
						bundleForRemoval.stop();
					}
				} catch (BundleException e) {
					LOG.warn(String.format("An error occurred stopping bundle %s", bundleIDToStop), e);
				}
			}
		}
	}

	private void uninstallBundles(Deque<ID> bundleIDsToUninstall) {
		for (ID bundleIDToRemove : bundleIDsToUninstall) {
			Bundle bundleForRemoval = installedBundlesByIdentifier.remove(bundleIDToRemove);
			if (bundleForRemoval != null) {
				try {
					bundleForRemoval.uninstall();
				} catch (BundleException e) {
					LOG.warn(String.format("An error occurred uninstalling bundle %s", bundleIDToRemove), e);
				}
			}
		}
	}

	private List<String> getExistingBundlesSymbolicNames() {
		List<String> existingBundlesSymbolicNames = new ArrayList<>();

		for (Bundle bundle : bundleContext.getBundles()) {
			existingBundlesSymbolicNames.add(bundle.getSymbolicName());
		}

		return existingBundlesSymbolicNames;
	}
}
