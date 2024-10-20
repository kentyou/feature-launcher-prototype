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
 * Handler for {@link org.osgi.service.featurelauncher.FeatureLauncherConstants.FRAMEWORK_LAUNCHING_PROPERTIES} {@link org.osgi.service.feature.FeatureExtension}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 19, 2024
 */
public class FrameworkLaunchingPropertiesFeatureExtensionHandlerImpl implements FeatureExtensionHandler {

	/*
	 * 
	 * 159.8 Framework Launching Properties
	 * 
	 * When a Feature is launched in an OSGi framework it may be necessary to
	 * specify Framework Properties. These are provided in the Framework Launching
	 * Properties extension section of the Feature. The Launcher must be able to
	 * satisfy the specified properties. If it cannot ensure that these are present
	 * in the running Framework the launcher must fail.
	 * 
	 * Framework Launching Properties can reference Variables as defined in
	 * Variables on page 76.
	 * 
	 * These variables are substituted before the properties are set.
	 * 
	 * (...)
	 * 
	 */

	/*
	 * 160.4.2.1 Providing Framework Launch Properties
	 * 
	 * (...)
	 * 
	 * Feature definitions that require particular framework launch properties can
	 * define them using a Feature Extension named FRAMEWORK_LAUNCHING_PROPERTIES.
	 * The Type of this Feature Extension must be JSON, where the value is a single
	 * JSON object. Each JSON property in this object represents a single Framework
	 * Launch Property. The name of each JSON property must be used as the name of a
	 * Framework Launch Property, unless the name starts with a single underscore _.
	 * The value of each property is used as the value of the Framework Launch
	 * Property, and must be a scalar type, that is a JSON string, number or
	 * boolean. If the value is a different JSON type then this must be treated as
	 * an error.
	 * 
	 * If the JSON property starts with a single underscore then it may be used for
	 * implementation specific behaviour, with the prefix _osgi reserved for future
	 * specifications. Implementation specific behaviours may permit JSON values to
	 * be any value JSON type
	 * 
	 * If users require their Framework Launch Property name to start with an
	 * underscore then they must use two underscores __ in the JSON property name.
	 * When the implementation detects more than one underscore at the beginning of
	 * a JSON property defined in this extension the leading underscore must be
	 * removed, and the remaining string used as the Framework Launch Property name.
	 * 
	 * All implementations of the Feature Launcher must support this extension, and
	 * use it to populate the Framework Launch Properties. The version of this
	 * extension is 1.0.0, and may be declared in the extension JSON using the
	 * property FRAMEWORK_LAUNCHING_PROPERTIES_VERSION.
	 * 
	 * (...)
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
