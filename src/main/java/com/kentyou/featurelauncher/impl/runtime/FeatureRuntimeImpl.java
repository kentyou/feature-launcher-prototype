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
package com.kentyou.featurelauncher.impl.runtime;

import java.io.Reader;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.runtime.FeatureRuntime;
import org.osgi.service.featurelauncher.runtime.InstalledFeature;

/**
 * TODO
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
class FeatureRuntimeImpl implements FeatureRuntime {

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory#createRepository(java.nio.file.Path)
	 */
	@Override
	public ArtifactRepository createRepository(Path path) {
		// TODO Auto-generated method stub
		return null;
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

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#getDefaultRepositories()
	 */
	@Override
	public Map<String, ArtifactRepository> getDefaultRepositories() {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#install(org.osgi.service.feature.Feature)
	 */
	@Override
	public InstallOperationBuilder install(Feature feature) {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#install(java.io.Reader)
	 */
	@Override
	public InstallOperationBuilder install(Reader jsonReader) {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#getInstalledFeatures()
	 */
	@Override
	public List<InstalledFeature> getInstalledFeatures() {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#remove(org.osgi.service.feature.ID)
	 */
	@Override
	public void remove(ID featureId) {
		// TODO Auto-generated method stub

	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#update(org.osgi.service.feature.ID, org.osgi.service.feature.Feature)
	 */
	@Override
	public UpdateOperationBuilder update(ID featureId, Feature feature) {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#update(org.osgi.service.feature.ID, java.io.Reader)
	 */
	@Override
	public UpdateOperationBuilder update(ID featureId, Reader jsonReader) {
		// TODO Auto-generated method stub
		return null;
	}

}
