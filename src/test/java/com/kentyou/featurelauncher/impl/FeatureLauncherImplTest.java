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

import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME;
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.LOCAL_ARTIFACT_REPOSITORY_PATH;
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.REMOTE_ARTIFACT_REPOSITORY_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.*;
import static org.osgi.service.featurelauncher.repository.ArtifactRepositoryConstants.ARTIFACT_REPOSITORY_NAME;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.service.featurelauncher.FeatureLauncher;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;

import com.kentyou.featurelauncher.impl.decorator.BundleStartLevelsFeatureExtensionHandler;
import com.kentyou.featurelauncher.impl.decorator.FrameworkLaunchingPropertiesFeatureExtensionHandler;
import com.kentyou.featurelauncher.impl.util.BundleStateUtil;
import com.kentyou.featurelauncher.impl.util.FeatureDecorationUtil;
import com.kentyou.featurelauncher.impl.util.ServiceLoaderUtil;

/**
 * Tests {@link com.kentyou.featurelauncher.impl.FeatureLauncherImpl}
 * 
 * As defined in: "160.4 The Feature Launcher"
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 17, 2024
 */
public class FeatureLauncherImplTest {
	FeatureLauncher featureLauncher;
	Path localM2RepositoryPath;
	Map<String, String> frameworkProperties;

	@TempDir
	Path frameworkStorageTempDir;

	@BeforeEach
	public void setUp(TestInfo info) throws InterruptedException, IOException {
		// Obtain path of dedicated local Maven repository
		localM2RepositoryPath = Paths.get(System.getProperty(LOCAL_ARTIFACT_REPOSITORY_PATH, "target/m2Repo"));
		assertTrue(Files.exists(localM2RepositoryPath), "No local artifact repository available at "
				+ localM2RepositoryPath + " missing system property or maven setup.");

		// Configure framework properties
		System.out.println(
				"*** Using " + frameworkStorageTempDir + " for framework storage in test " + info.getDisplayName());
		frameworkProperties = Map.of(Constants.FRAMEWORK_STORAGE, frameworkStorageTempDir.toString());

		// Load the Feature Launcher
		featureLauncher = ServiceLoaderUtil.loadFeatureLauncherService();
	}

	// The use of Gogo Shell requires that Std In is connected to a live
	// terminal, which breaks builds using batch mode (like CI).
	// We therefore tell gogo to be non-interactive

	@BeforeEach
	public void replaceStdInForGogo() throws IOException {
		System.setProperty("gosh.args", "-s");
	}

	@AfterEach
	public void resetStdIn() {
		System.clearProperty("gosh.args");
	}

//	@Disabled
	@Test
	public void testLaunchFeatureWithNoConfigWithDefaultFramework()
			throws IOException, InterruptedException, URISyntaxException, BundleException {
		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureLauncher.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME,
						LOCAL_ARTIFACT_REPOSITORY_PATH, localM2RepositoryPath.toString()));
		assertNotNull(remoteRepository);

		// Read Feature JSON
		Path featureJSONPath = Paths.get(getClass().getResource("/features/gogo-console-feature.json").toURI());

		// Launch the framework
		// @formatter:off
		Framework osgiFramework = featureLauncher.launch(Files.newBufferedReader(featureJSONPath))
				.withRepository(localArtifactRepository)
				.withRepository(remoteRepository)
				.withFrameworkProperties(frameworkProperties)
				.launchFramework();
		// @formatter:on

		// Verify bundles defined in feature are installed and started
		Bundle[] bundles = osgiFramework.getBundleContext().getBundles();
		assertEquals(4, bundles.length);

		assertEquals("org.apache.felix.gogo.command", bundles[1].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[1].getState()));

		assertEquals("org.apache.felix.gogo.shell", bundles[2].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[2].getState()));

		assertEquals("org.apache.felix.gogo.runtime", bundles[3].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[3].getState()));

		// Stop framework
		osgiFramework.stop();
		osgiFramework.waitForStop(0);
	}

//	@Disabled
	@Test
	public void testLaunchFeatureWithConfigWithDefaultFramework()
			throws IOException, InterruptedException, URISyntaxException, BundleException {
		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureLauncher.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME,
						LOCAL_ARTIFACT_REPOSITORY_PATH, localM2RepositoryPath.toString()));
		assertNotNull(remoteRepository);

		// console-webconsole-feature.unit-tests.json

		// Read Feature JSON
		Path featureJSONPath = Paths
				.get(getClass().getResource("/features/console-webconsole-feature.unit-tests.json").toURI());

		// Launch the framework
		// @formatter:off
		Framework osgiFramework = featureLauncher.launch(Files.newBufferedReader(featureJSONPath))
				.withRepository(localArtifactRepository)
				.withRepository(remoteRepository)
				.withFrameworkProperties(frameworkProperties)
				.launchFramework();
		// @formatter:on

		// Verify bundles defined in feature are installed and started
		Bundle[] bundles = osgiFramework.getBundleContext().getBundles();
		assertEquals(15, bundles.length);

		assertEquals("org.apache.felix.configadmin", bundles[1].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[1].getState()));

		assertEquals("org.apache.felix.gogo.command", bundles[2].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[2].getState()));

		assertEquals("org.apache.felix.gogo.shell", bundles[3].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[3].getState()));

		assertEquals("org.apache.felix.gogo.runtime", bundles[4].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[4].getState()));

		assertEquals("biz.aQute.gogo.commands.provider", bundles[5].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[5].getState()));

		assertEquals("org.apache.felix.webconsole", bundles[14].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[14].getState()));

		// Stop framework
		osgiFramework.stop();
		osgiFramework.waitForStop(0);
	}

//	@Disabled
	@Test
	public void testLaunchFeatureWithLaunchFrameworkExtension()
			throws IOException, InterruptedException, URISyntaxException, BundleException {
		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureLauncher.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME,
						LOCAL_ARTIFACT_REPOSITORY_PATH, localM2RepositoryPath.toString()));
		assertNotNull(remoteRepository);

		// Read Feature JSON
		Path featureJSONPath = Paths
				.get(getClass().getResource("/features/gogo-console-launch-framework-extension-feature.json").toURI());

		// Launch the framework
		// @formatter:off
		Framework osgiFramework = featureLauncher.launch(Files.newBufferedReader(featureJSONPath))
				.withRepository(localArtifactRepository)
				.withRepository(remoteRepository)
				.withFrameworkProperties(frameworkProperties)
				.launchFramework();
		// @formatter:on

		// Verify bundles defined in feature are installed and started
		Bundle[] bundles = osgiFramework.getBundleContext().getBundles();
		assertEquals(4, bundles.length);

		assertEquals("org.apache.felix.gogo.command", bundles[1].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[1].getState()));

		assertEquals("org.apache.felix.gogo.shell", bundles[2].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[2].getState()));

		assertEquals("org.apache.felix.gogo.runtime", bundles[3].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[3].getState()));

		// Stop framework
		osgiFramework.stop();
		osgiFramework.waitForStop(0);
	}

//	@Disabled
	@Test
	public void testLaunchFeatureWithNonMandatoryLaunchFrameworkExtension()
			throws IOException, InterruptedException, URISyntaxException, BundleException {
		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureLauncher.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME,
						LOCAL_ARTIFACT_REPOSITORY_PATH, localM2RepositoryPath.toString()));
		assertNotNull(remoteRepository);

		// Read Feature JSON
		Path featureJSONPath = Paths.get(getClass()
				.getResource("/features/gogo-console-launch-framework-extension-feature.non-mandatory.json").toURI());

		// Launch the framework
		// @formatter:off
		Framework osgiFramework = featureLauncher.launch(Files.newBufferedReader(featureJSONPath))
				.withRepository(localArtifactRepository)
				.withRepository(remoteRepository)
				.withFrameworkProperties(frameworkProperties)
				.launchFramework();
		// @formatter:on

		// Verify bundles defined in feature are installed and started
		Bundle[] bundles = osgiFramework.getBundleContext().getBundles();
		assertEquals(4, bundles.length);

		assertEquals("org.apache.felix.gogo.command", bundles[1].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[1].getState()));

		assertEquals("org.apache.felix.gogo.shell", bundles[2].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[2].getState()));

		assertEquals("org.apache.felix.gogo.runtime", bundles[3].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[3].getState()));

		// Stop framework
		osgiFramework.stop();
		osgiFramework.waitForStop(0);
	}

//	@Disabled
	@Test
	public void testLaunchFeatureWithNonFrameworkLaunchFrameworkExtension()
			throws IOException, InterruptedException, URISyntaxException, BundleException {
		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureLauncher.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME,
						LOCAL_ARTIFACT_REPOSITORY_PATH, localM2RepositoryPath.toString()));
		assertNotNull(remoteRepository);

		// Read Feature JSON
		Path featureJSONPath = Paths.get(getClass()
				.getResource("/features/gogo-console-launch-framework-extension-feature.non-framework.json").toURI());

		// Launch the framework
		// @formatter:off
		Framework osgiFramework = featureLauncher.launch(Files.newBufferedReader(featureJSONPath))
				.withRepository(localArtifactRepository)
				.withRepository(remoteRepository)
				.withFrameworkProperties(frameworkProperties)
				.launchFramework();
		// @formatter:on

		// Verify bundles defined in feature are installed and started
		Bundle[] bundles = osgiFramework.getBundleContext().getBundles();
		assertEquals(4, bundles.length);

		assertEquals("org.apache.felix.gogo.command", bundles[1].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[1].getState()));

		assertEquals("org.apache.felix.gogo.shell", bundles[2].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[2].getState()));

		assertEquals("org.apache.felix.gogo.runtime", bundles[3].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[3].getState()));

		// Stop framework
		osgiFramework.stop();
		osgiFramework.waitForStop(0);
	}

	@Test
	public void testFrameworkLaunchingPropertiesFeatureExtension()
			throws IOException, InterruptedException, URISyntaxException, BundleException, AbandonOperationException {
		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureLauncher.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME,
						LOCAL_ARTIFACT_REPOSITORY_PATH, localM2RepositoryPath.toString()));
		assertNotNull(remoteRepository);

		// Read Feature JSON
		Path featureJSONPath = Paths.get(getClass()
				.getResource("/features/gogo-console-framework-launching-properties-extension-feature.json").toURI());

		// Launch the framework
		// @formatter:off
		Framework osgiFramework = featureLauncher.launch(Files.newBufferedReader(featureJSONPath))
				.withRepository(localArtifactRepository)
				.withRepository(remoteRepository)
				.withFrameworkProperties(frameworkProperties)
				.launchFramework();
		// @formatter:on

		FrameworkLaunchingPropertiesFeatureExtensionHandler frameworkLaunchingPropertiesFeatureExtensionHandler = FeatureDecorationUtil
				.getBuiltInHandlerForExtension(FRAMEWORK_LAUNCHING_PROPERTIES);

		// Verify framework properties
		Map<String, String> frameworkProperties = frameworkLaunchingPropertiesFeatureExtensionHandler
				.getFrameworkProperties();
		assertTrue(frameworkProperties.containsKey("org.osgi.framework.bootdelegation"));
		assertEquals("sun.*,com.sun.*", frameworkProperties.get("org.osgi.framework.bootdelegation"));
		assertNotNull(osgiFramework.getBundleContext().getProperty("org.osgi.framework.bootdelegation"));
		assertEquals("sun.*,com.sun.*",
				osgiFramework.getBundleContext().getProperty("org.osgi.framework.bootdelegation"));

		assertTrue(frameworkProperties.containsKey("org.osgi.framework.storage"));
		assertEquals("/tmp", frameworkProperties.get("org.osgi.framework.storage"));
		assertNotNull(osgiFramework.getBundleContext().getProperty("org.osgi.framework.storage"));
		assertEquals("/tmp", osgiFramework.getBundleContext().getProperty("org.osgi.framework.storage"));

		assertTrue(frameworkProperties.containsKey("org.osgi.framework.storage.clean"));
		assertEquals("onFirstInit", frameworkProperties.get("org.osgi.framework.storage.clean"));
		assertNotNull(osgiFramework.getBundleContext().getProperty("org.osgi.framework.storage.clean"));
		assertEquals("onFirstInit", osgiFramework.getBundleContext().getProperty("org.osgi.framework.storage.clean"));

		assertTrue(frameworkProperties.containsKey("org.osgi.framework.bsnversion"));
		assertEquals("multiple", frameworkProperties.get("org.osgi.framework.bsnversion"));
		assertNotNull(osgiFramework.getBundleContext().getProperty("org.osgi.framework.bsnversion"));
		assertEquals("multiple", osgiFramework.getBundleContext().getProperty("org.osgi.framework.bsnversion"));

		assertTrue(frameworkProperties.containsKey("felix.log.level"));
		assertEquals("4", frameworkProperties.get("felix.log.level"));
		assertNotNull(osgiFramework.getBundleContext().getProperty("felix.log.level"));
		assertEquals("4", osgiFramework.getBundleContext().getProperty("felix.log.level"));

		assertTrue(frameworkProperties.containsKey("_custom_featurelauncher_launchprop"));
		assertEquals("test", frameworkProperties.get("_custom_featurelauncher_launchprop"));
		assertNotNull(osgiFramework.getBundleContext().getProperty("_custom_featurelauncher_launchprop"));
		assertEquals("test", osgiFramework.getBundleContext().getProperty("_custom_featurelauncher_launchprop"));

		// Verify custom properties
		Map<String, String> customProperties = frameworkLaunchingPropertiesFeatureExtensionHandler
				.getCustomProperties();
		assertTrue(customProperties.containsKey("_osgi_featurelauncher_launchprops_version"));
		assertEquals("1.0.0", customProperties.get("_osgi_featurelauncher_launchprops_version"));

		// Stop framework
		osgiFramework.stop();
		osgiFramework.waitForStop(0);
	}

	@Test
	public void testBundleStartLevelsFeatureExtension()
			throws IOException, InterruptedException, URISyntaxException, BundleException, AbandonOperationException {
		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureLauncher.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME,
						LOCAL_ARTIFACT_REPOSITORY_PATH, localM2RepositoryPath.toString()));
		assertNotNull(remoteRepository);
		
		// Read Feature JSON
		Path featureJSONPath = Paths
				.get(getClass().getResource("/features/gogo-console-bundle-start-levels-extension-feature.json").toURI());

		// Launch the framework
		// @formatter:off
		Framework osgiFramework = featureLauncher.launch(Files.newBufferedReader(featureJSONPath))
				.withRepository(localArtifactRepository)
				.withRepository(remoteRepository)
				.withFrameworkProperties(frameworkProperties)
				.launchFramework();
		// @formatter:on
		
		BundleStartLevelsFeatureExtensionHandler bundleStartLevelsFeatureExtensionHandler = FeatureDecorationUtil
				.getBuiltInHandlerForExtension(BUNDLE_START_LEVELS);
		
		assertTrue(bundleStartLevelsFeatureExtensionHandler.hasDefaultBundleStartLevel());
		assertEquals(2, bundleStartLevelsFeatureExtensionHandler.getDefaultBundleStartLevel().intValue());
		
		assertTrue(bundleStartLevelsFeatureExtensionHandler.hasMinimumFrameworkStartLevel());
		assertEquals(1, bundleStartLevelsFeatureExtensionHandler.getMinimumFrameworkStartLevel().intValue());
		
		// Stop framework
		osgiFramework.stop();
		osgiFramework.waitForStop(0);
	}
	
	@Disabled // TODO
	@Test
	public void testLaunchFeatureWithBundleMetadata()
			throws IOException, InterruptedException, URISyntaxException, BundleException {
		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureLauncher.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME,
						LOCAL_ARTIFACT_REPOSITORY_PATH, localM2RepositoryPath.toString()));
		assertNotNull(remoteRepository);

		// Read Feature JSON
		Path featureJSONPath = Paths.get(getClass().getResource("/features/gogo-console-bundle-start-levels-metadata-feature.json").toURI());

		// Launch the framework
		// @formatter:off
		Framework osgiFramework = featureLauncher.launch(Files.newBufferedReader(featureJSONPath))
				.withRepository(localArtifactRepository)
				.withRepository(remoteRepository)
				.withFrameworkProperties(frameworkProperties)
				.launchFramework();
		// @formatter:on

		// Verify bundles defined in feature are installed and started
//		Bundle[] bundles = osgiFramework.getBundleContext().getBundles();
//		assertEquals(4, bundles.length);
//
//		assertEquals("org.apache.felix.gogo.command", bundles[1].getSymbolicName());
//		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[1].getState()));
//
//		assertEquals("org.apache.felix.gogo.shell", bundles[2].getSymbolicName());
//		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[2].getState()));
//
//		assertEquals("org.apache.felix.gogo.runtime", bundles[3].getSymbolicName());
//		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[3].getState()));

		// Stop framework
		osgiFramework.stop();
		osgiFramework.waitForStop(0);
	}
}
