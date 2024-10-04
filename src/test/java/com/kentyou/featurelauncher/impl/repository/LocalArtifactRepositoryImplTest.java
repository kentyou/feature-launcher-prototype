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
package com.kentyou.featurelauncher.impl.repository;

import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.LOCAL_ARTIFACT_REPOSITORY_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;

/**
 * Tests
 * {@link com.kentyou.featurelauncher.impl.repository.LocalArtifactRepositoryImpl}
 * and
 * {@link com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryFactoryImpl}
 * 
 * As defined in: "160.2.1.2 Local Repositories"
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 17, 2024
 */
public class LocalArtifactRepositoryImplTest {
	ArtifactRepositoryFactory artifactRepositoryFactory;
	FeatureService featureService;
	Path localM2RepositoryPath;

	@BeforeEach
	public void setUp() {
		// Obtain path of dedicated local Maven repository
		if (System.getProperty(LOCAL_ARTIFACT_REPOSITORY_PATH) == null) {
			throw new IllegalStateException("Local Maven repository is not defined!");
		}

		localM2RepositoryPath = Paths.get(System.getProperty(LOCAL_ARTIFACT_REPOSITORY_PATH));

		// Load the Artifact Repository Factory
		ServiceLoader<ArtifactRepositoryFactory> artifactRepositoryFactoryServiceLoader = ServiceLoader
				.load(ArtifactRepositoryFactory.class);
		Optional<ArtifactRepositoryFactory> artifactRepositoryFactoryServiceLoaderOptional = artifactRepositoryFactoryServiceLoader
				.findFirst();
		if (artifactRepositoryFactoryServiceLoaderOptional.isPresent()) {
			artifactRepositoryFactory = artifactRepositoryFactoryServiceLoaderOptional.get();
		} else {
			throw new IllegalStateException("Error loading artifact repository factory!");
		}

		// Load the Feature Service
		ServiceLoader<FeatureService> featureServiceLoader = ServiceLoader.load(FeatureService.class);
		Optional<FeatureService> featureServiceLoaderOptional = featureServiceLoader.findFirst();
		if (featureServiceLoaderOptional.isPresent()) {
			featureService = featureServiceLoaderOptional.get();
		} else {
			throw new IllegalStateException("Error loading feature service!");
		}
	}

	@Test
	public void testCreateLocalArtifactRepository() throws IOException {
		ArtifactRepository localArtifactRepository = artifactRepositoryFactory.createRepository(localM2RepositoryPath);

		assertNotNull(localArtifactRepository);
		assertTrue(localArtifactRepository instanceof LocalArtifactRepositoryImpl);
	}

	@Test
	public void testCreateLocalArtifactRepositoryNullPath() {
		assertThrows(NullPointerException.class, () -> artifactRepositoryFactory.createRepository(null));
	}

	@Test
	public void testCreateLocalArtifactRepositoryPathDoesNotExist() throws IOException {
		Path nonExistingRepositoryPath = Paths.get(FileSystems.getDefault().getSeparator(), "tmp",
				UUID.randomUUID().toString());

		assertThrows(IllegalArgumentException.class,
				() -> artifactRepositoryFactory.createRepository(nonExistingRepositoryPath));
	}

	@Test
	public void testCreateLocalArtifactRepositoryPathNotADirectory() throws IOException {
		File tmpFile = File.createTempFile("localArtifactRepositoryTest", "tmp");
		tmpFile.deleteOnExit();

		assertThrows(IllegalArgumentException.class,
				() -> artifactRepositoryFactory.createRepository(tmpFile.toPath()));
	}

	@Test
	public void testGetArtifactFromLocalArtifactRepository() throws IOException {
		ArtifactRepository localArtifactRepository = artifactRepositoryFactory.createRepository(localM2RepositoryPath);

		assertNotNull(localArtifactRepository);
		assertTrue(localArtifactRepository instanceof LocalArtifactRepositoryImpl);

		ID artifactId = featureService.getIDfromMavenCoordinates("org.osgi:org.osgi.service.feature:1.0.0");
		assertNotNull(artifactId);

		try (JarInputStream jarIs = new JarInputStream(localArtifactRepository.getArtifact(artifactId))) {
			Manifest jarMf = jarIs.getManifest();
			assertTrue(jarMf != null);

			Attributes jarAttributes = jarMf.getMainAttributes();
			assertTrue(jarAttributes != null);
			assertEquals("org.osgi.service.feature", jarAttributes.getValue("Bundle-SymbolicName"));
		}
	}
}
