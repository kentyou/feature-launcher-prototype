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
import java.util.stream.Stream;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.featurelauncher.runtime.InstalledBundle;
import org.osgi.service.featurelauncher.runtime.MergeOperationType;
import org.osgi.service.featurelauncher.runtime.RuntimeBundleMerge;

/**
 * TODO
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
class RuntimeBundleMergeImpl implements RuntimeBundleMerge {

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.RuntimeBundleMerge#mergeBundle(org.osgi.service.featurelauncher.runtime.MergeOperationType, org.osgi.service.feature.Feature, org.osgi.service.feature.FeatureBundle, java.util.Collection, java.util.List)
	 */
	@Override
	public Stream<BundleMapping> mergeBundle(MergeOperationType operation, Feature feature, FeatureBundle toMerge,
			Collection<InstalledBundle> installedBundles, List<FeatureBundleDefinition> existingFeatureBundles) {
		// TODO Auto-generated method stub
		return null;
	}

}
