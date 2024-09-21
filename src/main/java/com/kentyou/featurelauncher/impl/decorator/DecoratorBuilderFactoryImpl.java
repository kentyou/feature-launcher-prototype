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
package com.kentyou.featurelauncher.impl.decorator;

import org.osgi.service.feature.FeatureArtifactBuilder;
import org.osgi.service.feature.FeatureBundleBuilder;
import org.osgi.service.feature.FeatureConfigurationBuilder;
import org.osgi.service.feature.FeatureExtension.Kind;
import org.osgi.service.feature.FeatureExtension.Type;
import org.osgi.service.feature.FeatureExtensionBuilder;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;

/**
 * TODO
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
class DecoratorBuilderFactoryImpl implements DecoratorBuilderFactory {

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory#newArtifactBuilder(org.osgi.service.feature.ID)
	 */
	@Override
	public FeatureArtifactBuilder newArtifactBuilder(ID id) {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory#newBundleBuilder(org.osgi.service.feature.ID)
	 */
	@Override
	public FeatureBundleBuilder newBundleBuilder(ID id) {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory#newConfigurationBuilder(java.lang.String)
	 */
	@Override
	public FeatureConfigurationBuilder newConfigurationBuilder(String pid) {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory#newConfigurationBuilder(java.lang.String, java.lang.String)
	 */
	@Override
	public FeatureConfigurationBuilder newConfigurationBuilder(String factoryPid, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory#newExtensionBuilder(java.lang.String, org.osgi.service.feature.FeatureExtension.Type, org.osgi.service.feature.FeatureExtension.Kind)
	 */
	@Override
	public FeatureExtensionBuilder newExtensionBuilder(String name, Type type, Kind kind) {
		// TODO Auto-generated method stub
		return null;
	}

}
