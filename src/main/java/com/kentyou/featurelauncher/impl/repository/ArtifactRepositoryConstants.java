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

import java.net.URI;

/**
 * 
 * @author michal
 * @since Sep 25, 2024
 */
public interface ArtifactRepositoryConstants {
	static final String REMOTE_ARTIFACT_REPOSITORY_TYPE = "default";

	static final URI REMOTE_ARTIFACT_REPOSITORY_URI = URI.create("https://repo1.maven.org/maven2/");

	static final String LOCAL_ARTIFACT_REPOSITORY_PATH = "localRepositoryPath";
	
	static final String DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME = "local";
	
	static final String DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME = "central";
}
