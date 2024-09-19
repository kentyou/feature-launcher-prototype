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
package com.kentyou.featurelauncher.impl.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;

import org.apache.felix.feature.impl.IDImpl;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.featurelauncher.FeatureLauncher;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;

/**
 * Tests
 * {@link com.kentyou.featurelauncher.impl.repository.LocalArtifactRepositoryImpl}
 * and
 * {@link com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryFactoryImpl}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 17, 2024
 */
public class LocalArtifactRepositoryImplTest {
	FeatureLauncher featureLauncher;

	@Before
	public void setUp() {
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
	public void testCreateLocalArtifactRepository() throws IOException {
		File userHome = new File(System.getProperty("user.home"));

		Path localM2RepositoryPath = Paths.get(userHome.getCanonicalPath(), ".m2", "repository");

		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);

		assertNotNull(localArtifactRepository);
		assertTrue(localArtifactRepository instanceof LocalArtifactRepositoryImpl);
	}

	@Test
	public void testCreateLocalArtifactRepositoryNullPath() {
		assertThrows(NullPointerException.class, () -> featureLauncher.createRepository(null));
	}

	@Test
	public void testCreateLocalArtifactRepositoryPathDoesNotExist() throws IOException {
		Path nonExistingRepositoryPath = Paths.get(FileSystems.getDefault().getSeparator(), "tmp",
				UUID.randomUUID().toString());

		assertThrows(IllegalArgumentException.class, () -> featureLauncher.createRepository(nonExistingRepositoryPath));
	}

	@Test
	public void testCreateLocalArtifactRepositoryPathNotADirectory() throws IOException {
		File tmpFile = File.createTempFile("localArtifactRepositoryTest", "tmp");
		tmpFile.deleteOnExit();

		assertThrows(IllegalArgumentException.class, () -> featureLauncher.createRepository(tmpFile.toPath()));
	}

	@Test
	public void testGetArtifactFromLocalArtifactRepository() throws IOException {
		File userHome = new File(System.getProperty("user.home"));

		Path localM2RepositoryPath = Paths.get(userHome.getCanonicalPath(), ".m2", "repository");

		ArtifactRepository localArtifactRepository = featureLauncher.createRepository(localM2RepositoryPath);

		assertNotNull(localArtifactRepository);
		assertTrue(localArtifactRepository instanceof LocalArtifactRepositoryImpl);

		IDImpl artifactId = IDImpl.fromMavenID("org.osgi:org.osgi.service.feature:1.0.0");
		assertNotNull(artifactId);

		try (InputStream is = localArtifactRepository.getArtifact(artifactId)) {

			File tmpFile = File.createTempFile("artifactFromLocalArtifactRepositoryTest", "tmp");
			tmpFile.deleteOnExit();

			try (OutputStream tmpFileOutputStream = new FileOutputStream(tmpFile)) {
				tmpFileOutputStream.write(is.readAllBytes());

				assertEquals(37779, tmpFile.length());
			}
		}
	}
}
