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

import java.io.InputStream;

import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;

/**
 * TODO
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
class RemoteArtifactRepositoryImpl implements ArtifactRepository {

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.repository.ArtifactRepository#getArtifact(org.osgi.service.feature.ID)
	 */
	@Override
	public InputStream getArtifact(ID id) {
		// TODO Auto-generated method stub
		return null;
	}

}
