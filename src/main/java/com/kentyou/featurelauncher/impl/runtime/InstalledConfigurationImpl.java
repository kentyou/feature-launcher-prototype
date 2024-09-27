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
package com.kentyou.featurelauncher.impl.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.runtime.InstalledConfiguration;

/**
 * TODO
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
class InstalledConfigurationImpl implements InstalledConfiguration {

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledConfiguration#getPid()
	 */
	@Override
	public String getPid() {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledConfiguration#getFactoryPid()
	 */
	@Override
	public Optional<String> getFactoryPid() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledConfiguration#getProperties()
	 */
	@Override
	public Map<String, Object> getProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledConfiguration#getOwningFeatures()
	 */
	@Override
	public List<ID> getOwningFeatures() {
		// TODO Auto-generated method stub
		return null;
	}

}
