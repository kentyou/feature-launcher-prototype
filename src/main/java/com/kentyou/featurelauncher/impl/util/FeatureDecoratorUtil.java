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
package com.kentyou.featurelauncher.impl.util;

import java.util.List;

import org.osgi.service.feature.Feature;
import org.osgi.service.featurelauncher.LaunchException;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;

import com.kentyou.featurelauncher.impl.decorator.DecoratorBuilderFactoryImpl;
import com.kentyou.featurelauncher.impl.decorator.FeatureDecoratorBuilderImpl;

/**
 * Util for {@link org.osgi.service.featurelauncher.decorator.FeatureDecorator} operations.
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 19, 2024
 */
public class FeatureDecoratorUtil {

	private FeatureDecoratorUtil() {
		// hidden constructor
	}

	public static Feature executeFeatureDecorators(Feature feature, List<FeatureDecorator> decorators) {
		for (FeatureDecorator decorator : decorators) {
			try {
				feature = decorator.decorate(feature, new FeatureDecoratorBuilderImpl(feature),
						new DecoratorBuilderFactoryImpl());
			} catch (AbandonOperationException e) {
				throw new LaunchException("Feature decoration handling failed!", e);
			}
		}

		return feature;
	}
}
