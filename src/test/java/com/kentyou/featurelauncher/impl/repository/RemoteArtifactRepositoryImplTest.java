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
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.REMOTE_ARTIFACT_REPOSITORY_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.REMOTE_ARTIFACT_REPOSITORY_NAME;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
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
 * {@link com.kentyou.featurelauncher.impl.repository.RemoteArtifactRepositoryImpl}
 * and
 * {@link com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryFactoryImpl}
 * 
 * As defined in: "160.2.1.3 Remote Repositories"
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 23, 2024
 */
public class RemoteArtifactRepositoryImplTest {
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
	public void testCreateRemoteArtifactRepository() throws IOException {
		ArtifactRepository remoteRepository = artifactRepositoryFactory.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(REMOTE_ARTIFACT_REPOSITORY_NAME, "central", LOCAL_ARTIFACT_REPOSITORY_PATH,
						localM2RepositoryPath.toString())); // FIXME: This is missing from API as is currently - i.e.
															// path to local repository is needed for remote repository as
															// well

		assertNotNull(remoteRepository);
		assertTrue(remoteRepository instanceof RemoteArtifactRepositoryImpl);
	}

	@Test
	public void testCreateRemoteArtifactRepositoryNullURI() {
		assertThrows(NullPointerException.class,
				() -> artifactRepositoryFactory.createRepository(null, Collections.emptyMap()));
	}

	@Test
	public void testCreateRemoteArtifactRepositoryNullConfigurationProperties() {
		assertThrows(NullPointerException.class,
				() -> artifactRepositoryFactory.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI, null));
	}

	@Test
	public void testCreateRemoteArtifactRepositoryEmptyConfigurationProperties() {
		assertThrows(NullPointerException.class, () -> artifactRepositoryFactory
				.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI, Collections.emptyMap()));
	}

	@Test
	public void testCreateRemoteArtifactRepositoryNoRepositoryName() {
		assertThrows(NullPointerException.class,
				() -> artifactRepositoryFactory.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
						Map.of(LOCAL_ARTIFACT_REPOSITORY_PATH, localM2RepositoryPath.toString())));
	}

	@Test
	public void testCreateRemoteArtifactRepositoryNoLocalRepositoryPath() {
		assertThrows(NullPointerException.class, () -> artifactRepositoryFactory
				.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI, Map.of(REMOTE_ARTIFACT_REPOSITORY_NAME, "central")));
	}

	@Test
	public void testCreateRemoteArtifactRepositoryPathDoesNotExist() throws IOException {
		Path nonExistingRepositoryPath = Paths.get(FileSystems.getDefault().getSeparator(), "tmp",
				UUID.randomUUID().toString());

		assertThrows(IllegalArgumentException.class,
				() -> artifactRepositoryFactory.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
						Map.of(REMOTE_ARTIFACT_REPOSITORY_NAME, "central", LOCAL_ARTIFACT_REPOSITORY_PATH,
								nonExistingRepositoryPath.toString())));
	}

	@Test
	public void testCreateRemoteArtifactRepositoryPathNotADirectory() throws IOException {
		File tmpFile = File.createTempFile("localArtifactRepositoryTest", "tmp");
		tmpFile.deleteOnExit();

		assertThrows(IllegalArgumentException.class,
				() -> artifactRepositoryFactory.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
						Map.of(REMOTE_ARTIFACT_REPOSITORY_NAME, "central", LOCAL_ARTIFACT_REPOSITORY_PATH,
								tmpFile.toPath().toString())));
	}

	@Test
	public void testGetArtifactFromRemoteArtifactRepository() throws IOException {
		ArtifactRepository remoteRepository = artifactRepositoryFactory.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(REMOTE_ARTIFACT_REPOSITORY_NAME, "central", LOCAL_ARTIFACT_REPOSITORY_PATH,
						localM2RepositoryPath.toString()));

		assertNotNull(remoteRepository);
		assertTrue(remoteRepository instanceof RemoteArtifactRepositoryImpl);

		IDImpl artifactId = IDImpl.fromMavenID("org.apache.felix:org.apache.felix.webconsole:4.8.8");
		assertNotNull(artifactId);

		try (JarInputStream jarIs = new JarInputStream(remoteRepository.getArtifact(artifactId))) {
			Manifest jarMf = jarIs.getManifest();
			assertTrue(jarMf != null);

			Attributes jarAttributes = jarMf.getMainAttributes();
			assertTrue(jarAttributes != null);
			assertEquals("org.apache.felix.webconsole", jarAttributes.getValue("Bundle-SymbolicName"));
		}
	}
}