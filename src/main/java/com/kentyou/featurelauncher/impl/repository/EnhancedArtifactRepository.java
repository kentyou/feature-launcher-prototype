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

import java.nio.file.Path;

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;

/**
 * Defines currently missing but very useful methods which should be added to
 * {@link org.osgi.service.featurelauncher.repository.ArtifactRepository}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 26, 2024
 */
@ConsumerType
public interface EnhancedArtifactRepository extends ArtifactRepository {

	public Path getArtifactPath(ID id);
}
