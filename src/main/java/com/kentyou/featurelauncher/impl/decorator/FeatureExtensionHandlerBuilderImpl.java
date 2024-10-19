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

import org.osgi.service.feature.Feature;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler.FeatureExtensionHandlerBuilder;

/**
 * Implementation of {@link org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler.FeatureExtensionHandlerBuilder}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 19, 2024
 */
public class FeatureExtensionHandlerBuilderImpl extends
		AbstractBaseFeatureDecorationBuilder<FeatureExtensionHandlerBuilder> implements FeatureExtensionHandlerBuilder {

	public FeatureExtensionHandlerBuilderImpl(Feature feature) {
		super(feature);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.BaseFeatureDecorationBuilder#build()
	 */
	@Override
	public Feature build() {
		ensureNotBuiltYet();

		this.isBuilt = true;

		return prebuild().build();
	}
}
