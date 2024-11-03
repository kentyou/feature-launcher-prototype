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
import static com.kentyou.featurelauncher.impl.util.ConfigurationUtil.CONFIGURATIONS_FILTER;
import static com.kentyou.featurelauncher.impl.util.ConfigurationUtil.constructConfigurationsFilter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.runtime.FeatureRuntime;
import org.osgi.service.featurelauncher.runtime.FeatureRuntimeConstants;
import org.osgi.service.featurelauncher.runtime.InstalledBundle;
import org.osgi.service.featurelauncher.runtime.InstalledConfiguration;
import org.osgi.service.featurelauncher.runtime.InstalledFeature;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.service.ServiceAware;

import com.kentyou.featurelauncher.impl.runtime.FeatureRuntimeConfigurationManager;
import com.kentyou.featurelauncher.impl.util.ArtifactRepositoryUtil;

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
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureRuntimeConfigurationManager> featureRuntimeConfigurationManagerServiceAware,
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureService> featureServiceAware,
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureRuntime> featureRuntimeServiceAware) {

		// ConfigurationAdmin
		assertEquals(1, configurationAdminServiceAware.getServices().size());
		ServiceReference<ConfigurationAdmin> configurationAdminServiceReference = configurationAdminServiceAware
				.getServiceReference();
		assertNotNull(configurationAdminServiceReference);

		// FeatureRuntimeConfigurationManager
		assertEquals(1, featureRuntimeConfigurationManagerServiceAware.getServices().size());
		ServiceReference<FeatureRuntimeConfigurationManager> featureRuntimeConfigurationManagerServiceReference = featureRuntimeConfigurationManagerServiceAware
				.getServiceReference();
		assertNotNull(featureRuntimeConfigurationManagerServiceReference);

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
			assertFalse(installedFeature.isDecorated());

			assertNotNull(installedFeature.getFeature().getID());
			assertEquals("com.kentyou.featurelauncher:gogo-console-feature:1.0",
					installedFeature.getFeature().getID().toString());

			assertNotNull(installedFeature.getInstalledBundles());
			List<InstalledBundle> installedBundles = installedFeature.getInstalledBundles();
			assertEquals(3, installedBundles.size());

			assertEquals("org.apache.felix.gogo.command", installedBundles.get(0).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.shell", installedBundles.get(1).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(1).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.runtime", installedBundles.get(2).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(2).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			// Verify also via installed features
			List<InstalledFeature> installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertFalse(installedFeatures.isEmpty());
			assertEquals(1, installedFeatures.size());

			// Remove feature
			featureRuntimeService.remove(installedFeature.getFeature().getID());

			// Verify again via installed features
			installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertTrue(installedFeatures.isEmpty());
			assertEquals(0, installedFeatures.size());
		}
	}

	@Test
	public void testInstallFeatureWithNoConfigWithDefaultRepositoriesWithDummyDecorator(
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureRuntime> featureRuntimeServiceAware)
			throws URISyntaxException, IOException {
		assertEquals(1, featureRuntimeServiceAware.getServices().size());
		FeatureRuntime featureRuntimeService = featureRuntimeServiceAware.getService();
		assertNotNull(featureRuntimeService);

		FeatureDecorator featureDummyDecorator = new FeatureDecorator() {

			@Override
			public Feature decorate(Feature feature, FeatureDecoratorBuilder decoratedFeatureBuilder,
					DecoratorBuilderFactory factory) throws AbandonOperationException {

				return decoratedFeatureBuilder.build();
			}
		};

		try (InputStream featureIs = getClass().getClassLoader()
				.getResourceAsStream("/features/gogo-console-feature.json");
				Reader featureReader = new BufferedReader(
						new InputStreamReader(featureIs, Charset.forName("UTF-8").newDecoder()));) {

			// Install Feature using default repositories
			// @formatter:off
			InstalledFeature installedFeature = featureRuntimeService.install(featureReader)
					.useDefaultRepositories(true)
					.withDecorator(featureDummyDecorator)
					.install();
			// @formatter:on
			assertNotNull(installedFeature);
			assertFalse(installedFeature.isInitialLaunch());
			assertTrue(installedFeature.isDecorated());

			assertNotNull(installedFeature.getFeature().getID());
			assertEquals("com.kentyou.featurelauncher:gogo-console-feature:jar:osgi.feature.decorated:1.0",
					installedFeature.getFeature().getID().toString());

			assertNotNull(installedFeature.getInstalledBundles());
			List<InstalledBundle> installedBundles = installedFeature.getInstalledBundles();
			assertEquals(3, installedBundles.size());

			assertEquals("org.apache.felix.gogo.command", installedBundles.get(0).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.shell", installedBundles.get(1).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(1).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.runtime", installedBundles.get(2).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(2).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			// Verify also via installed features
			List<InstalledFeature> installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertFalse(installedFeatures.isEmpty());
			assertEquals(1, installedFeatures.size());

			// Remove feature
			featureRuntimeService.remove(installedFeature.getOriginalFeature().getID());

			// Verify again via installed features
			installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertTrue(installedFeatures.isEmpty());
			assertEquals(0, installedFeatures.size());
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void testInstallFeatureWithNoConfigWithCustomRepositories(boolean useDefault,
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureRuntime> featureRuntimeServiceAware)
			throws URISyntaxException, IOException {
		assertEquals(1, featureRuntimeServiceAware.getServices().size());
		FeatureRuntime featureRuntimeService = featureRuntimeServiceAware.getService();
		assertNotNull(featureRuntimeService);

		// Set up a repositories
		ArtifactRepository localArtifactRepository = ArtifactRepositoryUtil
				.getDefaultLocalArtifactRepository(featureRuntimeService, localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = ArtifactRepositoryUtil
				.getDefaultRemoteArtifactRepository(featureRuntimeService, localM2RepositoryPath);
		assertNotNull(remoteRepository);

		try (InputStream featureIs = getClass().getClassLoader()
				.getResourceAsStream("/features/gogo-console-feature.json");
				Reader featureReader = new BufferedReader(
						new InputStreamReader(featureIs, Charset.forName("UTF-8").newDecoder()));) {

			// Install Feature using default repositories
			// @formatter:off
			InstalledFeature installedFeature = featureRuntimeService.install(featureReader)
					.useDefaultRepositories(useDefault)
					.addRepository(DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME, localArtifactRepository)
					.addRepository(DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME, remoteRepository)
					.install();
			// @formatter:on
			assertNotNull(installedFeature);
			assertFalse(installedFeature.isInitialLaunch());
			assertFalse(installedFeature.isDecorated());

			assertNotNull(installedFeature.getFeature().getID());
			assertEquals("com.kentyou.featurelauncher:gogo-console-feature:1.0",
					installedFeature.getFeature().getID().toString());

			assertNotNull(installedFeature.getInstalledBundles());
			List<InstalledBundle> installedBundles = installedFeature.getInstalledBundles();
			assertEquals(3, installedBundles.size());

			assertEquals("org.apache.felix.gogo.command", installedBundles.get(0).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.shell", installedBundles.get(1).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(1).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.runtime", installedBundles.get(2).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(2).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			// Verify also via installed features
			List<InstalledFeature> installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertFalse(installedFeatures.isEmpty());
			assertEquals(1, installedFeatures.size());

			// Remove feature
			featureRuntimeService.remove(installedFeature.getFeature().getID());

			// Verify again via installed features
			installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertTrue(installedFeatures.isEmpty());
			assertEquals(0, installedFeatures.size());
		}
	}

	@Test
	public void testInstallFeatureWithConfigWithDefaultRepositories(
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureRuntime> featureRuntimeServiceAware,
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureRuntimeConfigurationManager> featureRuntimeConfigurationManagerServiceAware,
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureService> featureServiceAware)
			throws URISyntaxException, IOException {

		assertEquals(1, featureRuntimeServiceAware.getServices().size());
		FeatureRuntime featureRuntimeService = featureRuntimeServiceAware.getService();
		assertNotNull(featureRuntimeService);

		assertEquals(1, featureRuntimeConfigurationManagerServiceAware.getServices().size());
		FeatureRuntimeConfigurationManager featureRuntimeConfigurationManagerService = featureRuntimeConfigurationManagerServiceAware
				.getService();
		assertNotNull(featureRuntimeConfigurationManagerService);

		assertEquals(1, featureServiceAware.getServices().size());
		FeatureService featureService = featureServiceAware.getService();
		assertNotNull(featureService);

		ID externalFeatureId = featureService.getIDfromMavenCoordinates(FeatureRuntimeConstants.EXTERNAL_FEATURE_ID);
		assertNotNull(externalFeatureId);

		try (InputStream featureIs = getClass().getClassLoader()
				.getResourceAsStream("/features/console-webconsole-feature.integration-tests.json");
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
			assertFalse(installedFeature.isDecorated());

			assertNotNull(installedFeature.getFeature().getID());
			assertEquals("com.kentyou.featurelauncher:console-webconsole-feature:1.0",
					installedFeature.getFeature().getID().toString());

			assertNotNull(installedFeature.getInstalledBundles());
			List<InstalledBundle> installedBundles = installedFeature.getInstalledBundles();
			assertEquals(13, installedBundles.size());
			// FIXME: temporary workaround for Felix OSGi framework bug - apparently it does
			// not allow installing fragment bundles with same symbolic name as those
			// already installed in running framework; Equinox does not have such problem;
//			assertEquals(14, installedBundles.size());

			assertEquals("org.apache.felix.configadmin", installedBundles.get(0).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(installedFeature.getFeature().getID()));
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(externalFeatureId));

			assertEquals("org.apache.felix.gogo.command", installedBundles.get(1).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(1).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.shell", installedBundles.get(2).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(2).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.runtime", installedBundles.get(3).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(3).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("biz.aQute.gogo.commands.provider", installedBundles.get(4).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(4).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			// FIXME: temporary workaround for Felix OSGi framework bug - apparently it does
			// not allow installing fragment bundles with same symbolic name as those
			// already installed in running framework; Equinox does not have such problem;
//			assertEquals("org.apache.felix.webconsole", installedBundles.get(13).getBundle().getSymbolicName());
//			assertTrue(installedBundles.get(13).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.webconsole", installedBundles.get(12).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(12).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			List<InstalledConfiguration> installedConfigurations = installedFeature.getInstalledConfigurations();
			assertFalse(installedConfigurations.isEmpty());
			assertEquals(2, installedConfigurations.size());

			assertEquals("org.apache.felix.http~httpFeatureLauncherTest", installedConfigurations.get(0).getPid());
			assertTrue(installedConfigurations.get(0).getFactoryPid().isPresent());
			assertEquals("org.apache.felix.http", installedConfigurations.get(0).getFactoryPid().get());

			assertEquals("org.apache.felix.webconsole.internal.servlet.OsgiManager",
					installedConfigurations.get(1).getPid());

			List<Configuration> configurations = featureRuntimeConfigurationManagerService
					.getConfigurations(constructConfigurationsFilter());
			assertFalse(configurations.isEmpty());
			assertEquals(2, configurations.size());

			for (Configuration configuration : configurations) {
				assertTrue(Boolean.valueOf(String.valueOf(configuration.getProperties().get(CONFIGURATIONS_FILTER)))
						.booleanValue());
			}

			// Verify also via installed features
			List<InstalledFeature> installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertFalse(installedFeatures.isEmpty());
			assertEquals(1, installedFeatures.size());

			// Remove feature
			featureRuntimeService.remove(installedFeature.getFeature().getID());

			// Verify again via installed features
			installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertTrue(installedFeatures.isEmpty());
			assertEquals(0, installedFeatures.size());
		}
	}

	@Test
	public void testUpdateFeatureWithConfigWithDefaultRepositories(
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureRuntime> featureRuntimeServiceAware,
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureRuntimeConfigurationManager> featureRuntimeConfigurationManagerServiceAware,
			@InjectService(cardinality = 1, timeout = 5000) ServiceAware<FeatureService> featureServiceAware)
			throws URISyntaxException, IOException {

		assertEquals(1, featureRuntimeServiceAware.getServices().size());
		FeatureRuntime featureRuntimeService = featureRuntimeServiceAware.getService();
		assertNotNull(featureRuntimeService);

		assertEquals(1, featureRuntimeConfigurationManagerServiceAware.getServices().size());
		FeatureRuntimeConfigurationManager featureRuntimeConfigurationManagerService = featureRuntimeConfigurationManagerServiceAware
				.getService();
		assertNotNull(featureRuntimeConfigurationManagerService);

		assertEquals(1, featureServiceAware.getServices().size());
		FeatureService featureService = featureServiceAware.getService();
		assertNotNull(featureService);

		ID externalFeatureId = featureService.getIDfromMavenCoordinates(FeatureRuntimeConstants.EXTERNAL_FEATURE_ID);
		assertNotNull(externalFeatureId);

		// Install Feature using default repositories
		try (InputStream featureIs = getClass().getClassLoader()
				.getResourceAsStream("/features/gogo-console-feature.json");
				Reader featureReader = new BufferedReader(
						new InputStreamReader(featureIs, Charset.forName("UTF-8").newDecoder()));) {

			// @formatter:off
			InstalledFeature installedFeature = featureRuntimeService.install(featureReader)
					.useDefaultRepositories(true)
					.install();
			// @formatter:on
			assertNotNull(installedFeature);
			assertFalse(installedFeature.isInitialLaunch());
			assertFalse(installedFeature.isDecorated());

			assertNotNull(installedFeature.getFeature().getID());
			assertEquals("com.kentyou.featurelauncher:gogo-console-feature:1.0",
					installedFeature.getFeature().getID().toString());

			assertNotNull(installedFeature.getInstalledBundles());
			List<InstalledBundle> installedBundles = installedFeature.getInstalledBundles();
			assertEquals(3, installedBundles.size());

			assertEquals("org.apache.felix.gogo.command", installedBundles.get(0).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.shell", installedBundles.get(1).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(1).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.runtime", installedBundles.get(2).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(2).getOwningFeatures().contains(installedFeature.getFeature().getID()));
		}

		// Verify via installed features
		List<InstalledFeature> installedFeatures = featureRuntimeService.getInstalledFeatures();
		assertFalse(installedFeatures.isEmpty());
		assertEquals(1, installedFeatures.size());

		ID featureId = installedFeatures.get(0).getFeature().getID();

		// Update Feature with same ID with additional bundles
		try (InputStream featureIs = getClass().getClassLoader()
				.getResourceAsStream("/features/gogo-console-feature.update-with-webconsole.json");
				Reader featureReader = new BufferedReader(
						new InputStreamReader(featureIs, Charset.forName("UTF-8").newDecoder()));) {

			// Update Feature using default repositories
			// @formatter:off
			InstalledFeature updatedFeature = featureRuntimeService.update(featureId, featureReader)
					.useDefaultRepositories(true)
					.update();
			// @formatter:on

			assertNotNull(updatedFeature);
			assertFalse(updatedFeature.isInitialLaunch());
			assertFalse(updatedFeature.isDecorated());

			assertNotNull(updatedFeature.getFeature().getID());
			assertEquals("com.kentyou.featurelauncher:gogo-console-feature:1.0",
					updatedFeature.getFeature().getID().toString());

			assertNotNull(updatedFeature.getInstalledBundles());
			List<InstalledBundle> installedBundles = updatedFeature.getInstalledBundles();
			assertEquals(13, installedBundles.size());
			// FIXME: temporary workaround for Felix OSGi framework bug - apparently it does
			// not allow installing fragment bundles with same symbolic name as those
			// already installed in running framework; Equinox does not have such problem;
//			assertEquals(14, installedBundles.size());

			assertEquals("org.apache.felix.configadmin", installedBundles.get(0).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(updatedFeature.getFeature().getID()));
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(externalFeatureId));

			assertEquals("org.apache.felix.gogo.command", installedBundles.get(1).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(1).getOwningFeatures().contains(updatedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.shell", installedBundles.get(2).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(2).getOwningFeatures().contains(updatedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.runtime", installedBundles.get(3).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(3).getOwningFeatures().contains(updatedFeature.getFeature().getID()));

			assertEquals("biz.aQute.gogo.commands.provider", installedBundles.get(4).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(4).getOwningFeatures().contains(updatedFeature.getFeature().getID()));

			// FIXME: temporary workaround for Felix OSGi framework bug - apparently it does
			// not allow installing fragment bundles with same symbolic name as those
			// already installed in running framework; Equinox does not have such problem;
//			assertEquals("org.apache.felix.webconsole", installedBundles.get(13).getBundle().getSymbolicName());
//			assertTrue(installedBundles.get(13).getOwningFeatures().contains(updatedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.webconsole", installedBundles.get(12).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(12).getOwningFeatures().contains(updatedFeature.getFeature().getID()));

			List<InstalledConfiguration> installedConfigurations = updatedFeature.getInstalledConfigurations();
			assertFalse(installedConfigurations.isEmpty());
			assertEquals(2, installedConfigurations.size());

			assertEquals("org.apache.felix.http~httpFeatureLauncherTest", installedConfigurations.get(0).getPid());
			assertTrue(installedConfigurations.get(0).getFactoryPid().isPresent());
			assertEquals("org.apache.felix.http", installedConfigurations.get(0).getFactoryPid().get());

			assertEquals("org.apache.felix.webconsole.internal.servlet.OsgiManager",
					installedConfigurations.get(1).getPid());

			List<Configuration> configurations = featureRuntimeConfigurationManagerService
					.getConfigurations(constructConfigurationsFilter());
			assertFalse(configurations.isEmpty());
			assertEquals(2, configurations.size());

			for (Configuration configuration : configurations) {
				assertTrue(Boolean.valueOf(String.valueOf(configuration.getProperties().get(CONFIGURATIONS_FILTER)))
						.booleanValue());
			}

			// Remove feature
			featureRuntimeService.remove(updatedFeature.getFeature().getID());

			// Verify again via installed features
			installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertTrue(installedFeatures.isEmpty());
			assertEquals(0, installedFeatures.size());
		}
	}
}
