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

import java.util.List;
import java.util.Optional;

import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;

/**
 * Handler for
 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.LAUNCH_FRAMEWORK}
 * {@link org.osgi.service.feature.FeatureExtension}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 30, 2024
 */
public interface LaunchFrameworkFeatureExtensionHandler extends FeatureExtensionHandler {

	/*
	 * 160.4.2.2 Selecting a framework implementation
	 * 
	 * When defining a feature it is not always possible to be framework
	 * independent. Sometimes specific framework APIs, or licensing restrictions,
	 * will require that a particular implementation is used. In this case a Feature
	 * Extension named LAUNCH_FRAMEWORK with Type ARTIFACTS can be used to list one
	 * or more artifacts representing OSGi framework implementations.
	 * 
	 * The list of artifacts is treated as a preference order, with the first listed
	 * artifact being used if available, and so on, until a framework is found. If a
	 * listed artifact is not an OSGi framework implementation then the Feature
	 * Launcher must log a warning and continue on to the next artifact in the list.
	 * If the Kind of the feature is MANDATORY and none of the listed artifacts are
	 * available then launching must fail with a LaunchException
	 * 
	 * The Feature Launcher implementation may identify that an artifact is an OSGi
	 * framework implementation in any way that it chooses, however it must
	 * recognise framework implementations that provide the Framework Launch API
	 * using the service loader pattern, as described in the Launching and
	 * Controlling a Framework section of OSGi Core Release 8.
	 * 
	 */

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
	Optional<FrameworkFactory> selectFrameworkFactory(FeatureExtension featureExtension,
			List<ArtifactRepository> artifactRepositories, Optional<FrameworkFactory> defaultFrameworkFactoryOptional);
}
