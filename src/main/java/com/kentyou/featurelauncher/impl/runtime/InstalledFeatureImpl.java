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

import java.util.List;

import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.runtime.InstalledBundle;
import org.osgi.service.featurelauncher.runtime.InstalledConfiguration;
import org.osgi.service.featurelauncher.runtime.InstalledFeature;

/**
 * TODO
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
class InstalledFeatureImpl implements InstalledFeature {

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledFeature#getFeatureId()
	 */
	@Override
	public ID getFeatureId() {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledFeature#isInitialLaunch()
	 */
	@Override
	public boolean isInitialLaunch() {
		// TODO Auto-generated method stub
		return false;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledFeature#getInstalledBundles()
	 */
	@Override
	public List<InstalledBundle> getInstalledBundles() {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledFeature#getInstalledConfigurations()
	 */
	@Override
	public List<InstalledConfiguration> getInstalledConfigurations() {
		// TODO Auto-generated method stub
		return null;
	}

}
