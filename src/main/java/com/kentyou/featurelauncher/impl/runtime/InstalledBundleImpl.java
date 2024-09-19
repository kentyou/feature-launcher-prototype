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

import java.util.Collection;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.runtime.InstalledBundle;

/**
 * TODO
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
class InstalledBundleImpl implements InstalledBundle {

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledBundle#getBundleId()
	 */
	@Override
	public ID getBundleId() {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledBundle#getAliases()
	 */
	@Override
	public Collection<ID> getAliases() {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledBundle#getBundle()
	 */
	@Override
	public Bundle getBundle() {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledBundle#getStartLevel()
	 */
	@Override
	public int getStartLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledBundle#getOwningFeatures()
	 */
	@Override
	public List<ID> getOwningFeatures() {
		// TODO Auto-generated method stub
		return null;
	}

}
