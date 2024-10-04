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
package com.kentyou.featurelauncher.impl.runtime.integration;

import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME;
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME;
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.LOCAL_ARTIFACT_REPOSITORY_PATH;
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.REMOTE_ARTIFACT_REPOSITORY_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.REMOTE_ARTIFACT_REPOSITORY_NAME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.runtime.FeatureRuntime;
import org.osgi.service.featurelauncher.runtime.InstalledBundle;
import org.osgi.service.featurelauncher.runtime.InstalledFeature;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.service.ServiceAware;

import com.kentyou.featurelauncher.impl.FeatureLauncherConfigurationManager;

/**
 * Tests {@link com.kentyou.featurelauncher.impl.runtime.FeatureRuntimeImpl}
 * 
 * As defined in: "160.5 The Feature Runtime Service"
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 2, 2024
 */
public class FeatureRuntimeIntegrationTest {
	Path localM2RepositoryPath;

	@InjectBundleContext
	BundleContext bundleContext;

	@BeforeEach
	public void setUp() throws InterruptedException, IOException {
		// Obtain path of dedicated local Maven repository
		if (System.getProperty(LOCAL_ARTIFACT_REPOSITORY_PATH) == null) {
			throw new IllegalStateException("Local Maven repository is not defined!");
		}

		localM2RepositoryPath = Paths.get(System.getProperty(LOCAL_ARTIFACT_REPOSITORY_PATH));
	}

	@Test
	public void testServices(
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<ConfigurationAdmin> configurationAdminServiceAware,
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureLauncherConfigurationManager> featureConfigurationManagerServiceAware,
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureService> featureServiceAware,
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureRuntime> featureRuntimeServiceAware) {

		// ConfigurationAdmin
		assertEquals(1, configurationAdminServiceAware.getServices().size());
		ServiceReference<ConfigurationAdmin> configurationAdminServiceReference = configurationAdminServiceAware
				.getServiceReference();
		assertNotNull(configurationAdminServiceReference);

		// FeatureConfigurationManager
		assertEquals(1, featureConfigurationManagerServiceAware.getServices().size());
		ServiceReference<FeatureLauncherConfigurationManager> featureConfigurationManagerServiceReference = featureConfigurationManagerServiceAware
				.getServiceReference();
		assertNotNull(featureConfigurationManagerServiceReference);

		// FeatureService
		assertEquals(1, featureServiceAware.getServices().size());
		ServiceReference<FeatureService> featureServiceReference = featureServiceAware.getServiceReference();
		assertNotNull(featureServiceReference);

		// FeatureRuntime
		assertEquals(1, featureRuntimeServiceAware.getServices().size());
		ServiceReference<FeatureRuntime> featureRuntimeServiceReference = featureRuntimeServiceAware
				.getServiceReference();
		assertNotNull(featureRuntimeServiceReference);
	}

	@Test
	public void testGetDefaultRepositories(
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureRuntime> featureRuntimeServiceAware)
			throws URISyntaxException, IOException {
		assertEquals(1, featureRuntimeServiceAware.getServices().size());
		FeatureRuntime featureRuntimeService = featureRuntimeServiceAware.getService();
		assertNotNull(featureRuntimeService);

		Map<String, ArtifactRepository> defaultArtifactRepositories = featureRuntimeService.getDefaultRepositories();
		assertNotNull(defaultArtifactRepositories);
		assertFalse(defaultArtifactRepositories.isEmpty());
		assertEquals(2, defaultArtifactRepositories.size());
		assertTrue(defaultArtifactRepositories.containsKey(DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME));
		assertTrue(defaultArtifactRepositories.containsKey(DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME));
	}

	@Test
	public void testInstallFeatureWithNoConfigWithDefaultRepositories(
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureRuntime> featureRuntimeServiceAware)
			throws URISyntaxException, IOException {
		assertEquals(1, featureRuntimeServiceAware.getServices().size());
		FeatureRuntime featureRuntimeService = featureRuntimeServiceAware.getService();
		assertNotNull(featureRuntimeService);

		try (InputStream featureIs = getClass().getClassLoader()
				.getResourceAsStream("/features/gogo-console-feature.json");
				Reader featureReader = new BufferedReader(
						new InputStreamReader(featureIs, Charset.forName("UTF-8").newDecoder()));) {

			// Install Feature using default repositories
			// @formatter:off
			InstalledFeature installedFeature = featureRuntimeService.install(featureReader)
					.useDefaultRepositories(true)
					.install();
			// @formatter:on
			assertNotNull(installedFeature);
			assertFalse(installedFeature.isInitialLaunch());

			assertNotNull(installedFeature.getFeatureId());
			assertEquals("com.kentyou.featurelauncher:gogo-console-feature:1.0",
					installedFeature.getFeatureId().toString());

			assertNotNull(installedFeature.getInstalledBundles());
			List<InstalledBundle> installedBundles = installedFeature.getInstalledBundles();
			assertEquals(3, installedBundles.size());

			assertEquals("org.apache.felix.gogo.command", installedBundles.get(0).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(installedFeature.getFeatureId()));

			assertEquals("org.apache.felix.gogo.shell", installedBundles.get(1).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(1).getOwningFeatures().contains(installedFeature.getFeatureId()));

			assertEquals("org.apache.felix.gogo.runtime", installedBundles.get(2).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(2).getOwningFeatures().contains(installedFeature.getFeatureId()));
		}
	}

	@Test
	public void testInstallFeatureWithNoConfigWithCustomRepositories(
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureRuntime> featureRuntimeServiceAware)
			throws URISyntaxException, IOException {
		assertEquals(1, featureRuntimeServiceAware.getServices().size());
		FeatureRuntime featureRuntimeService = featureRuntimeServiceAware.getService();
		assertNotNull(featureRuntimeService);

		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureRuntimeService.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureRuntimeService.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(REMOTE_ARTIFACT_REPOSITORY_NAME, "central", LOCAL_ARTIFACT_REPOSITORY_PATH,
						localM2RepositoryPath.toString()));
		assertNotNull(remoteRepository);

		try (InputStream featureIs = getClass().getClassLoader()
				.getResourceAsStream("/features/gogo-console-feature.json");
				Reader featureReader = new BufferedReader(
						new InputStreamReader(featureIs, Charset.forName("UTF-8").newDecoder()));) {

			// Install Feature using default repositories
			// @formatter:off
			InstalledFeature installedFeature = featureRuntimeService.install(featureReader)
					.useDefaultRepositories(false)
					.addRepository("local", localArtifactRepository)
					.addRepository("central", remoteRepository)
					.install();
			// @formatter:on
			assertNotNull(installedFeature);
			assertFalse(installedFeature.isInitialLaunch());

			assertNotNull(installedFeature.getFeatureId());
			assertEquals("com.kentyou.featurelauncher:gogo-console-feature:1.0",
					installedFeature.getFeatureId().toString());

			assertNotNull(installedFeature.getInstalledBundles());
			List<InstalledBundle> installedBundles = installedFeature.getInstalledBundles();
			assertEquals(3, installedBundles.size());

			assertEquals("org.apache.felix.gogo.command", installedBundles.get(0).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(installedFeature.getFeatureId()));

			assertEquals("org.apache.felix.gogo.shell", installedBundles.get(1).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(1).getOwningFeatures().contains(installedFeature.getFeatureId()));

			assertEquals("org.apache.felix.gogo.runtime", installedBundles.get(2).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(2).getOwningFeatures().contains(installedFeature.getFeatureId()));
		}
	}
}
