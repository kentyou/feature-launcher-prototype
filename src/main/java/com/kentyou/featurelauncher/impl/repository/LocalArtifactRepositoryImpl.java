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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 160.2.1.2 Local Repositories
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
class LocalArtifactRepositoryImpl implements ArtifactRepository {
	private static final Logger LOG = LoggerFactory.getLogger(LocalArtifactRepositoryImpl.class);

	private static final String DEFAULT_EXTENSION = "jar";

	/**
	 * As per <a href=
	 * "https://maven.apache.org/ref/3.9.9/maven-core/artifact-handlers.html">"Default
	 * Artifact Handlers Reference"</a>
	 */
	// @formatter:off
	private static final Map<String, String> TYPE_TO_EXTENSION_MAP = Map.ofEntries(
			Map.entry("pom", "pom"),
			Map.entry("jar", DEFAULT_EXTENSION),
			Map.entry("test-jar", DEFAULT_EXTENSION),
			Map.entry("maven-plugin", DEFAULT_EXTENSION),
			Map.entry("ejb", DEFAULT_EXTENSION),
			Map.entry("ejb-client", DEFAULT_EXTENSION),
			Map.entry("war", "war"),
			Map.entry("ear", "ear"),
			Map.entry("rar", "rar"),
			Map.entry("java-source", DEFAULT_EXTENSION),
			Map.entry("javadoc", DEFAULT_EXTENSION));
	// @formatter:on

	private final Path m2RepositoryPath;

	LocalArtifactRepositoryImpl(Path m2RepositoryPath) {
		this.m2RepositoryPath = m2RepositoryPath;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.repository.ArtifactRepository#getArtifact(org.osgi.service.feature.ID)
	 */
	@Override
	public InputStream getArtifact(ID id) {
		Objects.requireNonNull(id, "ID cannot be null!");

		Path path = getArtifactM2RepoPath(id);

		File file = path.toFile();

		if (file.exists()) {
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException e) {
				LOG.error(String.format("Error getting artifact ID '%s'", id.toString()), e);
				throw new RuntimeException(e); // TODO: clarify with Tim regarding exception thrown
			}
		} else {
			LOG.warn(String.format("Artifact ID '%s' does not exist in this repository!", id.toString()));
			return null;
		}
	}

	private Path getArtifactM2RepoPath(ID id) {
		Path projectHome = Paths.get(m2RepositoryPath.toAbsolutePath().toString(), id.getGroupId().replace('.', '/'));

		StringBuilder artifactName = new StringBuilder();
		artifactName.append(id.getArtifactId());
		artifactName.append("-");
		artifactName.append(id.getVersion());
		if (id.getClassifier().isPresent()) {
			artifactName.append("-");
			artifactName.append(id.getClassifier().get());
		}
		artifactName.append(".");
		artifactName.append(getExtensionForType(id.getType()));

		return Paths.get(projectHome.toAbsolutePath().toString(), id.getArtifactId(), id.getVersion(),
				artifactName.toString());
	}

	private String getExtensionForType(Optional<String> typeOptional) {
		if (typeOptional.isPresent()) {
			String type = typeOptional.get();
			if (TYPE_TO_EXTENSION_MAP.containsKey(type)) {
				return TYPE_TO_EXTENSION_MAP.get(type);
			}
		}

		return DEFAULT_EXTENSION;
	}
}