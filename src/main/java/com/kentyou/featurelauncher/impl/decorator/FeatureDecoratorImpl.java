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
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;

/**
 * TODO
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
public class FeatureDecoratorImpl implements FeatureDecorator {

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.FeatureDecorator#decorate(org.osgi.service.feature.Feature, org.osgi.service.featurelauncher.decorator.FeatureDecorator.FeatureDecoratorBuilder, org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory)
	 */
	@Override
	public Feature decorate(Feature feature, FeatureDecoratorBuilder decoratedFeatureBuilder,
			DecoratorBuilderFactory factory) throws AbandonOperationException {
		// TODO Auto-generated method stub
		return null;
	}

}
