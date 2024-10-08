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

import static com.kentyou.featurelauncher.impl.FeatureLauncherImplConstants.FRAMEWORK_STORAGE_CLEAN_TESTONLY;
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.LOCAL_ARTIFACT_REPOSITORY_PATH;
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.REMOTE_ARTIFACT_REPOSITORY_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.REMOTE_ARTIFACT_REPOSITORY_NAME;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.service.featurelauncher.FeatureLauncher;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;

import com.kentyou.featurelauncher.impl.util.BundleStateUtil;

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
	Path frameworkStorageTempDir;

	@BeforeEach
	public void setUp() throws InterruptedException, IOException {
		// Obtain path of dedicated local Maven repository
		if (System.getProperty(LOCAL_ARTIFACT_REPOSITORY_PATH) == null) {
			throw new IllegalStateException("Local Maven repository is not defined!");
		}

		localM2RepositoryPath = Paths.get(System.getProperty(LOCAL_ARTIFACT_REPOSITORY_PATH));

		// Configure framwork properties
		frameworkStorageTempDir = Files.createTempDirectory("osgi_");
		frameworkProperties = Map.of(Constants.FRAMEWORK_STORAGE, frameworkStorageTempDir.toString(),
				Constants.FRAMEWORK_STORAGE_CLEAN, FRAMEWORK_STORAGE_CLEAN_TESTONLY);

		// Load the Feature Launcher
		ServiceLoader<FeatureLauncher> loader = ServiceLoader.load(FeatureLauncher.class);
		Optional<FeatureLauncher> featureLauncherOptional = loader.findFirst();

		if (featureLauncherOptional.isPresent()) {
			featureLauncher = featureLauncherOptional.get();
		} else {
			throw new IllegalStateException("Error loading feature launcher!");
		}
	}

	@Test
	public void testLaunchFeatureWithNoConfigWithDefaultFramework()
			throws IOException, InterruptedException, URISyntaxException, BundleException {
		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureLauncher.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(REMOTE_ARTIFACT_REPOSITORY_NAME, "central", LOCAL_ARTIFACT_REPOSITORY_PATH,
						localM2RepositoryPath.toString()));
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

	@Test
	public void testLaunchFeatureWithConfigWithDefaultFramework()
			throws IOException, InterruptedException, URISyntaxException, BundleException {
		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureLauncher.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(REMOTE_ARTIFACT_REPOSITORY_NAME, "central", LOCAL_ARTIFACT_REPOSITORY_PATH,
						localM2RepositoryPath.toString()));
		assertNotNull(remoteRepository);

		// Read Feature JSON
		Path featureJSONPath = Paths.get(getClass().getResource("/features/console-webconsole-feature.json").toURI());

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

	@Test
	public void testLaunchFeatureWithLaunchFrameworkExtension()
			throws IOException, InterruptedException, URISyntaxException, BundleException {
		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureLauncher.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(REMOTE_ARTIFACT_REPOSITORY_NAME, "central", LOCAL_ARTIFACT_REPOSITORY_PATH,
						localM2RepositoryPath.toString()));
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

	@Test
	public void testLaunchFeatureWithNonMandatoryLaunchFrameworkExtension()
			throws IOException, InterruptedException, URISyntaxException, BundleException {
		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureLauncher.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(REMOTE_ARTIFACT_REPOSITORY_NAME, "central", LOCAL_ARTIFACT_REPOSITORY_PATH,
						localM2RepositoryPath.toString()));
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

	@Test
	public void testLaunchFeatureWithNonFrameworkLaunchFrameworkExtension()
			throws IOException, InterruptedException, URISyntaxException, BundleException {
		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureLauncher.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(REMOTE_ARTIFACT_REPOSITORY_NAME, "central", LOCAL_ARTIFACT_REPOSITORY_PATH,
						localM2RepositoryPath.toString()));
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
}
