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
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.osgi.service.featurelauncher.repository.ArtifactRepositoryConstants.ARTIFACT_REPOSITORY_NAME;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
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

import com.kentyou.featurelauncher.impl.util.ServiceLoaderUtil;

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
		artifactRepositoryFactory = ServiceLoaderUtil.loadArtifactRepositoryFactoryService();

		// Load the Feature Service
		featureService = ServiceLoaderUtil.loadFeatureService();
	}

	@Test
	public void testCreateRemoteArtifactRepository() throws IOException {
		ArtifactRepository remoteRepository = artifactRepositoryFactory.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME, LOCAL_ARTIFACT_REPOSITORY_PATH,
						localM2RepositoryPath.toString())); // path to local repository is needed for remote repository
															// as well

		assertNotNull(remoteRepository);
		assertTrue(remoteRepository instanceof RemoteArtifactRepositoryImpl);
	}

	@Test
	public void testCreateRemoteArtifactRepositoryWithTemporaryLocalArtifactRepository() throws IOException {
		ArtifactRepository remoteRepository = artifactRepositoryFactory.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME));

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
		ArtifactRepository remoteRepository = artifactRepositoryFactory.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of());

		assertNotNull(remoteRepository);
		assertTrue(remoteRepository instanceof RemoteArtifactRepositoryImpl);
	}

	@Test
	public void testCreateRemoteArtifactRepositoryNoRepositoryName() {
		ArtifactRepository remoteRepository = artifactRepositoryFactory.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
						Map.of(LOCAL_ARTIFACT_REPOSITORY_PATH, localM2RepositoryPath.toString()));
		assertNotNull(remoteRepository);
		assertTrue(remoteRepository instanceof RemoteArtifactRepositoryImpl);
	}

	@Test
	public void testCreateRemoteArtifactRepositoryPathDoesNotExist() throws IOException {
		Path nonExistingRepositoryPath = Paths.get(FileSystems.getDefault().getSeparator(), "tmp",
				UUID.randomUUID().toString());

		assertThrows(IllegalArgumentException.class,
				() -> artifactRepositoryFactory.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
						Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME, LOCAL_ARTIFACT_REPOSITORY_PATH,
								nonExistingRepositoryPath.toString())));
	}

	@Test
	public void testCreateRemoteArtifactRepositoryPathNotADirectory() throws IOException {
		File tmpFile = File.createTempFile("localArtifactRepositoryTest", "tmp");
		tmpFile.deleteOnExit();

		assertThrows(IllegalArgumentException.class,
				() -> artifactRepositoryFactory.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
						Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME, LOCAL_ARTIFACT_REPOSITORY_PATH,
								tmpFile.toPath().toString())));
	}

	@Test
	public void testGetArtifactFromRemoteArtifactRepository() throws IOException {
		ArtifactRepository remoteRepository = artifactRepositoryFactory.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME, LOCAL_ARTIFACT_REPOSITORY_PATH,
						localM2RepositoryPath.toString()));

		assertNotNull(remoteRepository);
		assertTrue(remoteRepository instanceof RemoteArtifactRepositoryImpl);

		ID artifactId = featureService.getIDfromMavenCoordinates("org.apache.felix:org.apache.felix.webconsole:4.8.8");
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
