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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ServiceLoader;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
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
	private static final String M2_REPO_PROP_KEY = "M2_REPO_PATH"; // TODO: move to common constants

	FeatureLauncher featureLauncher;
	Path localM2RepositoryPath;

	@Before
	public void setUp() {
		// Obtain path of dedicated M2 repo
		if (System.getProperty(M2_REPO_PROP_KEY) == null) {
			throw new IllegalStateException("M2 repository is not defined!");
		}

		localM2RepositoryPath = Paths.get(System.getProperty(M2_REPO_PROP_KEY));

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
	public void testLaunchDefaultFrameworkWithLocalArtifactRepository()
			throws IOException, InterruptedException, URISyntaxException, BundleException {
		// Set up a repository
		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);
		assertNotNull(localArtifactRepository);

		// Read Feature JSON
		Path featureJSONPath = Paths.get(getClass().getResource("/features/gogo-console-feature.json").toURI());

		// Launch the framework
		Framework osgiFramework = featureLauncher.launch(Files.newBufferedReader(featureJSONPath))
				.withRepository(localArtifactRepository).launchFramework();

		// Verify bundles defined in feature are installed and started
		Bundle[] bundles = osgiFramework.getBundleContext().getBundles();
		assertEquals(4, bundles.length);

		assertEquals("org.apache.felix.gogo.command", bundles[1].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[1].getState()));

		assertEquals("org.apache.felix.gogo.shell", bundles[2].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[2].getState()));

		assertEquals("org.apache.felix.gogo.runtime", bundles[3].getSymbolicName());
		assertEquals("ACTIVE", BundleStateUtil.getBundleStateString(bundles[3].getState()));

		osgiFramework.stop();
	}
}
