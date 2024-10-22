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

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kentyou.featurelauncher.impl.util.FileSystemUtil;

/**
 * 160.2.1 The Artifact Repository Factory
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
public class ArtifactRepositoryFactoryImpl implements ArtifactRepositoryFactory {
	private static final Logger LOG = LoggerFactory.getLogger(ArtifactRepositoryFactoryImpl.class);
	
	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory#createRepository(java.nio.file.Path)
	 */
	@Override
	public ArtifactRepository createRepository(Path path) {
		Objects.requireNonNull(path, "Path cannot be null!");

		FileSystemUtil.validateDirectory(path);

		return new LocalArtifactRepositoryImpl(path);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory#createRepository(java.net.URI, java.util.Map)
	 */
	@Override
	public ArtifactRepository createRepository(URI uri, Map<String, Object> configurationProperties) {
		Objects.requireNonNull(uri, "URI cannot be null!");
		Objects.requireNonNull(configurationProperties, "Configuration properties cannot be null!");

		if(isLocalArtifactRepository(uri)) {
			return new LocalArtifactRepositoryImpl(Paths.get(uri), configurationProperties);
		}
		
		if (configurationProperties.containsKey(LOCAL_ARTIFACT_REPOSITORY_PATH)) {
			FileSystemUtil.validateDirectory(
					Paths.get(String.valueOf(configurationProperties.get(LOCAL_ARTIFACT_REPOSITORY_PATH))));
		}

		return new RemoteArtifactRepositoryImpl(uri, configurationProperties);
	}
	
	public static boolean isLocalArtifactRepository(URI uri) {
		return "file".equals(uri.getScheme());
	}
}
