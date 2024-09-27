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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

import org.apache.felix.feature.impl.IDImpl;
import org.junit.Before;
import org.junit.Test;
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
	Path localM2RepositoryPath;

	@Before
	public void setUp() {
		// Obtain path of dedicated local Maven repository
		if (System.getProperty(LOCAL_ARTIFACT_REPOSITORY_PATH) == null) {
			throw new IllegalStateException("Local Maven repository is not defined!");
		}

		localM2RepositoryPath = Paths.get(System.getProperty(LOCAL_ARTIFACT_REPOSITORY_PATH));

		// Load the Artifact Repository Factory
		ServiceLoader<ArtifactRepositoryFactory> loader = ServiceLoader.load(ArtifactRepositoryFactory.class);
		Optional<ArtifactRepositoryFactory> artifactRepositoryFactoryOptional = loader.findFirst();

		if (artifactRepositoryFactoryOptional.isPresent()) {
			artifactRepositoryFactory = artifactRepositoryFactoryOptional.get();
		} else {
			throw new IllegalStateException("Error loading artifact repository factory!");
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

		IDImpl artifactId = IDImpl.fromMavenID("org.osgi:org.osgi.service.feature:1.0.0");
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
