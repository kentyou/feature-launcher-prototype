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

import java.util.Map;

import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;

/**
 * Handler for
 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.FRAMEWORK_LAUNCHING_PROPERTIES}
 * {@link org.osgi.service.feature.FeatureExtension}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 28, 2024
 */
public interface FrameworkLaunchingPropertiesFeatureExtensionHandler extends FeatureExtensionHandler {
	Map<String, String> getFrameworkProperties();

	Map<String, String> getCustomProperties(); // properties starting with a single underscore
}
