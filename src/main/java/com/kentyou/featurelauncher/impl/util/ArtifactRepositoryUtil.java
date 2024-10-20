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
package com.kentyou.featurelauncher.impl.util;

import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME;
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME;
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.LOCAL_ARTIFACT_REPOSITORY_PATH;
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.REMOTE_ARTIFACT_REPOSITORY_URI;
import static org.osgi.service.featurelauncher.repository.ArtifactRepositoryConstants.ARTIFACT_REPOSITORY_NAME;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;

/**
 * Util for {@link org.osgi.service.featurelauncher.repository.ArtifactRepository} operations.
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 11, 2024
 */
public class ArtifactRepositoryUtil {

	private ArtifactRepositoryUtil() {
		// hidden constructor
	}

	public static Map<String, ArtifactRepository> getDefaultArtifactRepositories(
			ArtifactRepositoryFactory artifactRepositoryFactory, Path defaultM2RepositoryPath) throws IOException {
		// @formatter:off
		return Map.of(
				DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME, getDefaultLocalArtifactRepository(artifactRepositoryFactory, defaultM2RepositoryPath), 
				DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME, getDefaultRemoteArtifactRepository(artifactRepositoryFactory, defaultM2RepositoryPath));
		// @formatter:on
	}

	public static Path getDefaultM2RepositoryPath() throws IOException {
		File userHome = new File(System.getProperty("user.home"));

		return Paths.get(userHome.getCanonicalPath(), ".m2", "repository");
	}

	public static ArtifactRepository getDefaultLocalArtifactRepository(
			ArtifactRepositoryFactory artifactRepositoryFactory, Path defaultM2RepositoryPath) throws IOException {
		return artifactRepositoryFactory.createRepository(defaultM2RepositoryPath);
	}

	public static ArtifactRepository getDefaultRemoteArtifactRepository(
			ArtifactRepositoryFactory artifactRepositoryFactory, Path defaultM2RepositoryPath) throws IOException {
		return artifactRepositoryFactory.createRepository(REMOTE_ARTIFACT_REPOSITORY_URI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME,
						LOCAL_ARTIFACT_REPOSITORY_PATH, defaultM2RepositoryPath.toString()));
	}
}
