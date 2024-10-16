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
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.DEFAULT_REMOTE_ARTIFACT_REPOSITORY_TYPE;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.REMOTE_ARTIFACT_REPOSITORY_NAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.internal.impl.resolver.MavenSessionBuilderSupplier;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.osgi.service.feature.ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kentyou.featurelauncher.impl.util.FileSystemUtil;

/**
 * 160.2.1.3 Remote Repositories
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
class RemoteArtifactRepositoryImpl implements FileSystemArtifactRepository {
	private static final Logger LOG = LoggerFactory.getLogger(RemoteArtifactRepositoryImpl.class);

	private final URI repositoryURI;
	private final Map<String, Object> configurationProperties;
	private final Path localRepositoryPath;
	private final RemoteRepository remoteRepository;

	public RemoteArtifactRepositoryImpl(URI repositoryURI, Map<String, Object> configurationProperties) {
		this.repositoryURI = repositoryURI;
		this.configurationProperties = configurationProperties;

		if (!configurationProperties.isEmpty() && configurationProperties.containsKey(LOCAL_ARTIFACT_REPOSITORY_PATH)) {
			this.localRepositoryPath = Paths
					.get(String.valueOf(configurationProperties.get(LOCAL_ARTIFACT_REPOSITORY_PATH)));
		} else {
			this.localRepositoryPath = createTemporaryLocalArtifactRepository();
		}

		// @formatter:off
		this.remoteRepository = new RemoteRepository.Builder(
				String.valueOf(this.configurationProperties.get(REMOTE_ARTIFACT_REPOSITORY_NAME)), 
				DEFAULT_REMOTE_ARTIFACT_REPOSITORY_TYPE, 
				this.repositoryURI.toASCIIString())
				.build();
		// @formatter:on
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.repository.ArtifactRepository#getArtifact(org.osgi.service.feature.ID)
	 */
	@Override
	public InputStream getArtifact(ID id) {
		Objects.requireNonNull(id, "ID cannot be null!");

		Path path = getArtifactPath(id);
		if (path != null) {
			File file = path.toFile();

			if (file.exists()) {
				try {
					return new FileInputStream(file);
				} catch (FileNotFoundException e) {
					LOG.error(String.format("Error getting artifact ID '%s'", id.toString()), e);
				}
			} else {
				LOG.warn(String.format("Artifact ID '%s' does not exist in this repository!", id.toString()));
			}
		}

		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see com.kentyou.featurelauncher.impl.repository.EnhancedArtifactRepository#getArtifactPath(org.osgi.service.feature.ID)
	 */
	@Override
	public Path getArtifactPath(ID id) {
		Objects.requireNonNull(id, "ID cannot be null!");

		try (RepositorySystem repositorySystem = newRepositorySystem();
				CloseableSession repositorySystemSession = newSession(repositorySystem)) {

			Artifact artifact = new DefaultArtifact(id.toString());

			ArtifactRequest artifactRequest = new ArtifactRequest();
			artifactRequest.setArtifact(artifact);
			artifactRequest.addRepository(remoteRepository);

			ArtifactResult artifactResult = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);

			if (artifactResult.isResolved() && !artifactResult.isMissing()) {
				return artifactResult.getArtifact().getPath();
			}

		} catch (ArtifactResolutionException e) {
			LOG.error(String.format("Error getting artifact ID '%s'", id.toString()), e);
		}

		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see com.kentyou.featurelauncher.impl.repository.FileSystemArtifactRepository#getLocalRepositoryPath()
	 */
	@Override
	public Path getLocalRepositoryPath() {
		return localRepositoryPath;
	}

	private RepositorySystem newRepositorySystem() {
		return new RepositorySystemSupplier().get();
	}

	private CloseableSession newSession(RepositorySystem system) {
		RepositorySystemSession uninitializedSession = new DefaultRepositorySystemSession(h -> false);

		LocalRepository localRepository = new LocalRepository(localRepositoryPath);
		LocalRepositoryManager localRepositoryManager = system.newLocalRepositoryManager(uninitializedSession,
				localRepository);

		MavenSessionBuilderSupplier sessionBuilderSupplier = new MavenSessionBuilderSupplier(system);

		RepositorySystemSession.SessionBuilder sessionBuilder = sessionBuilderSupplier.get();
		sessionBuilder.setLocalRepositoryManager(localRepositoryManager);

		return sessionBuilder.build();
	}

	private Path createTemporaryLocalArtifactRepository() {
		try {
			Path localRepositoryPath = Files.createTempDirectory("featurelauncherM2repo_");

			deleteOnShutdown(localRepositoryPath);

			return localRepositoryPath;

		} catch (IOException e) {
			throw new IllegalStateException("Could not create temporary local artifact repository!", e);
		}
	}

	private void deleteOnShutdown(Path localRepositoryPath) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					FileSystemUtil.recursivelyDelete(localRepositoryPath);
				} catch (IOException e) {
					LOG.warn("Could not delete temporary local artifact repository!");
				}
			}
		});
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RemoteArtifactRepositoryImpl [repositoryURI=" + repositoryURI + ", configurationProperties="
				+ configurationProperties + ", localRepositoryPath=" + localRepositoryPath + "]";
	}
}
