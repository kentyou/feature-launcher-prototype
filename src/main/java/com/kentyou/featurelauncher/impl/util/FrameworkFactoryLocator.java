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
package com.kentyou.featurelauncher.impl.util;

import java.util.Optional;
import java.util.ServiceLoader;

import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.featurelauncher.LaunchException;

/**
 * 160.4.3.2: Locating a framework implementation
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 18, 2024
 */
public class FrameworkFactoryLocator {

	// TODO: clarify regarding this item
	/*
	 * "160.4.3.2: #1. If any provider specific configuration has been given to the
	 * Feature Launcher implementation then this should be used to identify the
	 * framework"
	 */

	/*
	 * "160.4.3.2: #2. If the Feature declares an Extension LAUNCH_FRAMEWORK then
	 * the Feature Launcher implementation must use the first listed artifact that
	 * can be found in any configured Artifact Repositories, as described in
	 * Selecting a framework implementation on page 99"
	 */
	public static FrameworkFactory selectFrameworkFactory() {
		// TODO: 160.4.3.2: #2
		return null;
	}

	/*
	 * "160.4.3.2: #3. If no framework implementation is found in the previous steps
	 * then the Feature Launcher implementation must search the classpath using the
	 * Thread Context Class Loader, or, if the Thread Context Class Loader is not
	 * set, the Class Loader which loaded the caller of the Feature Launcher's
	 * launch method. The first suitable framework instance located is the instance
	 * that will be used."
	 */
	public static FrameworkFactory findFrameworkFactory() {
		// TODO: 160.4.3.2: #3
		return null;
	}

	/**
	 * 160.4.3.2: #4. In the event that no suitable OSGi framework can be found by
	 * any of the previous steps then the Feature Launcher implementation may
	 * provide a default framework implementation to be used.
	 * 
	 * @return
	 */
	public static FrameworkFactory loadDefaultFrameworkFactory() {
		ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class);

		Optional<FrameworkFactory> frameworkFactoryOptional = loader.findFirst();
		if (frameworkFactoryOptional.isPresent()) {
			return frameworkFactoryOptional.get();
		} else {
			throw new LaunchException("Error loading default framework factory!");
		}
	}
}
