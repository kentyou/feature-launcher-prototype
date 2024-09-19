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
import java.util.Map;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.featurelauncher.runtime.InstalledConfiguration;
import org.osgi.service.featurelauncher.runtime.MergeOperationType;
import org.osgi.service.featurelauncher.runtime.RuntimeConfigurationMerge;

/**
 * TODO
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
class RuntimeConfigurationMergeImpl implements RuntimeConfigurationMerge {

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.RuntimeConfigurationMerge#mergeConfiguration(org.osgi.service.featurelauncher.runtime.MergeOperationType, org.osgi.service.feature.Feature, org.osgi.service.feature.FeatureConfiguration, org.osgi.service.featurelauncher.runtime.InstalledConfiguration, java.util.List)
	 */
	@Override
	public Map<String, Object> mergeConfiguration(MergeOperationType operation, Feature feature,
			FeatureConfiguration toMerge, InstalledConfiguration configuration,
			List<FeatureConfigurationDefinition> existingFeatureConfigurations) {
		// TODO Auto-generated method stub
		return null;
	}

}
