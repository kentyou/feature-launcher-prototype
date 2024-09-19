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

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

		if (!path.toFile().exists()) {
			LOG.error(String.format("Path '%s' does not exist!", path.toString()));
			throw new IllegalArgumentException(String.format("Path '%s' does not exist!", path.toString()));
		}

		if (!path.toFile().isDirectory()) {
			LOG.error(String.format("Path '%s' is not a directory!", path.toString()));
			throw new IllegalArgumentException(String.format("Path '%s' is not a directory!", path.toString()));
		}

		return new LocalArtifactRepositoryImpl(path);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory#createRepository(java.net.URI, java.util.Map)
	 */
	@Override
	public ArtifactRepository createRepository(URI uri, Map<String, Object> props) {
		// TODO Auto-generated method stub
		return null;
	}
}
