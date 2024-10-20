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
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;

/**
 * Handler for {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.BUNDLE_START_LEVELS} {@link org.osgi.service.feature.FeatureExtension}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 19, 2024
 */
public class BundleStartLevelsFeatureExtensionHandlerImpl implements FeatureExtensionHandler {

	/*
	 * 160.3.2 Setting the bundle start levels
	 * 
	 * An OSGi framework contains a number of bundles which collaborate to produce a
	 * functioning application. There are times when some bundles require the system
	 * to have reached a certain state before they can be started. To address this
	 * use case the OSGi framework has the concept of start levels as described in
	 * the Start Level API Specification chapter of OSGi Core Release 8.
	 * 
	 * Setting the initial start level for the OSGi framework when bootstrapping can
	 * easily be achieved using the framework launch property
	 * org.osgi.framework.startlevel.beginning as defined by the OSGi core
	 * specification.
	 * 
	 * Controlling the start levels assigned to the bundles in a feature is managed
	 * through the use of Feature Bundle metadata. Specifically the Feature Launcher
	 * will look for a Feature Bundle metadata property named
	 * BUNDLE_START_LEVEL_METADATA which is of type integer and has a value between
	 * 1 and Integer.MAX_VALUE inclusive. If the property does not exist then the
	 * default start level will be used. If the property does exist and is not a
	 * suitable integer then launching must fail with a LaunchException.
	 * 
	 * Setting the default start level for the bundles, and the minimum start level
	 * required for an installed Feature is accomplished by using a Feature
	 * Extension named BUNDLE_START_LEVELS with Type JSON. The JSON contained in
	 * this extension is used to configure the default start level for the bundles,
	 * and the target start level for the framework. (...)
	 */

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler#handle(org.osgi.service.feature.Feature, org.osgi.service.feature.FeatureExtension, org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler.FeatureExtensionHandlerBuilder, org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory)
	 */
	@Override
	public Feature handle(Feature feature, FeatureExtension extension,
			FeatureExtensionHandlerBuilder decoratedFeatureBuilder, DecoratorBuilderFactory factory)
			throws AbandonOperationException {
		/**
		 * FIXME: There is a problem with how the Feature Launcher API currently expects
		 * feature extensions ( {@link org.osgi.service.feature.FeatureExtension } to be
		 * processed via feature extension handlers
		 * {@link org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler} -
		 * at least for the built-in feature extensions ( i.e.
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.LAUNCH_FRAMEWORK},
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.FRAMEWORK_LAUNCHING_PROPERTIES}
		 * and
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.BUNDLE_START_LEVELS}
		 * ), information contained in those extensions is required at different stages
		 * of launching / installing feature and cannot be passed back to calling code
		 * in feature {@link org.osgi.service.feature.Feature} object, which is the only
		 * object returned from the only method
		 * {@link org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler.handle(Feature,
		 * FeatureExtension, FeatureExtensionHandlerBuilder, DecoratorBuilderFactory)}
		 * defined on feature extension handler.
		 * 
		 * 1. In the case of
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.LAUNCH_FRAMEWORK}
		 * feature extension, information contained in that extension is required when
		 * locating framework implementation, as defined in section 160.4.3.2. To enable
		 * passing back required information, i.e. instance of
		 * {@link org.osgi.framework.launch.FrameworkFactory}, to where it is needed
		 * during the process of launching feature, necessitated defining additional
		 * interface
		 * ({@link com.kentyou.featurelauncher.impl.decorator.LaunchFrameworkFeatureExtensionHandler}
		 * and calling that additional method defined on that interface when locating
		 * framework implementation from (
		 * {@link com.kentyou.featurelauncher.impl.FrameworkFactoryLocator.locateFrameworkFactory(Feature,
		 * List<ArtifactRepository>)} ) method.
		 * 
		 * 2. In the case of
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.FRAMEWORK_LAUNCHING_PROPERTIES}
		 * feature extension, information contained in that extension is required when
		 * creating framework instance via
		 * {@link org.osgi.framework.launch.FrameworkFactory.newFramework(Map<String,
		 * String>)} method - it cannot be passed back in "processed" feature
		 * {@link org.osgi.service.feature.Feature} object as it is currently defined by
		 * separate OSGi specification ( 159 ), and even if it were, the calling code
		 * would have to check again and extract this information so these user-defined
		 * framework properties can be used when creating new instance of framework.
		 * 
		 * As a side note, this extension appears obsolete when processing extensions in
		 * {@link org.osgi.service.featurelauncher.runtime.FeatureRuntime}
		 * implementation and is only relevant when processing extensions in
		 * {@link org.osgi.service.featurelauncher.FeatureLauncher} implementation.
		 * 
		 * 3. In the case of
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.BUNDLE_START_LEVELS}
		 * feature extension, at least some of the information contained in that
		 * extension, i.e. `org.osgi.framework.startlevel` framework launch property, is
		 * required when creating framework instance via
		 * {@link org.osgi.framework.launch.FrameworkFactory.newFramework(Map<String,
		 * String>)} method, similar to
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.FRAMEWORK_LAUNCHING_PROPERTIES}
		 * feature extension ( see item 2 above ).
		 * 
		 * In addition, similarly to
		 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.FRAMEWORK_LAUNCHING_PROPERTIES}
		 * extension ( see item 2 above ), it is only relevant to
		 * {@link org.osgi.service.featurelauncher.FeatureLauncher} implementation.
		 * 
		 * 
		 * As can be seen, at least for these built-in extensions, since required
		 * information cannot be passed back to calling code and is required at
		 * different stages of processing, these extensions cannot be processed via
		 * {@link com.kentyou.featurelauncher.impl.util.FeatureExtensionUtil.executeFeatureExtensionHandlers(Feature,
		 * Map<String, FeatureExtensionHandler>)}.
		 * 
		 * One possible solution would be to introduce additional parameter to
		 * {@link org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler.handle(Feature,
		 * FeatureExtension, FeatureExtensionHandlerBuilder, DecoratorBuilderFactory)}
		 * method, which "collects" such information ( e.g. instance of
		 * {@link org.osgi.framework.launch.FrameworkFactory}, framework launching
		 * properties, etc. ) which can then be utilized by calling code when it's
		 * actually needed.
		 **/
		// so, just return the original feature for now
		return feature;
	}
}
