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
 * Defines additional constants for artifact repositories, supplementing those
 * defined in {@link org.osgi.service.featurelauncher.FeatureLauncherConstants}
 * and {@link org.osgi.service.featurelauncher.runtime.FeatureRuntimeConstants}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 25, 2024
 */
public interface ArtifactRepositoryConstants {
	String REMOTE_ARTIFACT_REPOSITORY_TYPE = "type";

	String DEFAULT_REMOTE_ARTIFACT_REPOSITORY_TYPE = "default";

	URI REMOTE_ARTIFACT_REPOSITORY_URI = URI.create("https://repo1.maven.org/maven2/");

	String LOCAL_ARTIFACT_REPOSITORY_PATH = "localRepositoryPath";

	String DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME = "local";

	String DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME = "central";
}
