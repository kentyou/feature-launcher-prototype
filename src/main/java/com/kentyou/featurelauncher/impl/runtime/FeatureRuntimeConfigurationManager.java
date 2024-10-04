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
package com.kentyou.featurelauncher.impl.runtime;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.feature.FeatureConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages feature configurations via Configuration Admin Service for {@link com.kentyou.featurelauncher.impl.runtime.FeatureRuntimeImpl}
 * 
 * As defined in the following sections of the "160. Feature Launcher Service Specification":
 *  - 160.4.3.4
 *  - 160.4.3.5
 *  - 160.5.2.1.3
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 4, 2024
 */
@Component(service = FeatureRuntimeConfigurationManager.class)
public class FeatureRuntimeConfigurationManager {
	private static final Logger LOG = LoggerFactory.getLogger(FeatureRuntimeConfigurationManager.class);

	private static final String CONFIGURATIONS_FILTER = ".featurelauncher.config";

	@Reference
	ConfigurationAdmin configurationAdmin;

	public void createConfigurations(List<FeatureConfiguration> featureConfigurations) {
		featureConfigurations.forEach(this::createConfiguration);
	}

	public void removeConfigurations(Set<String> featuresConfigurationsPids) {
		try {
			Map<String, Configuration> existingConfigurations = getExistingConfigurations();

			if (!existingConfigurations.isEmpty()) {
				for (String featuresConfigurationsPid : featuresConfigurationsPids) {
					if (existingConfigurations.containsKey(featuresConfigurationsPid)) {
						existingConfigurations.remove(featuresConfigurationsPid).delete();
					}
				}
			}

		} catch (IOException | InvalidSyntaxException e) {
			LOG.error("Error removing configurations!", e);
		}
	}

	private void createConfiguration(FeatureConfiguration featureConfiguration) {
		if (featureConfiguration.getFactoryPid().isPresent()) {
			createFactoryConfiguration(featureConfiguration);
			return;
		}

		try {
			LOG.info(String.format("Creating configuration %s", featureConfiguration.getPid()));

			Configuration configuration = configurationAdmin.getConfiguration(featureConfiguration.getPid());

			updateConfigurationProperties(configuration, featureConfiguration);

		} catch (IllegalArgumentException | IOException e) {
			LOG.error(String.format("Error creating configuration %s!", featureConfiguration.getPid()), e);
		}
	}

	private void createFactoryConfiguration(FeatureConfiguration featureConfiguration) {
		try {
			LOG.info(String.format("Creating factory configuration %s", featureConfiguration.getPid()));

			Configuration configuration = configurationAdmin
					.getFactoryConfiguration(featureConfiguration.getFactoryPid().get(), featureConfiguration.getPid());

			updateConfigurationProperties(configuration, featureConfiguration);

		} catch (IllegalArgumentException | IOException e) {
			LOG.error(String.format("Error creating configuration %s!", featureConfiguration.getPid()), e);
		}
	}

	private void updateConfigurationProperties(Configuration configuration, FeatureConfiguration featureConfiguration) {
		Dictionary<String, Object> configurationProperties = FrameworkUtil
				.asDictionary(featureConfiguration.getValues());

		configurationProperties.put(CONFIGURATIONS_FILTER, Boolean.TRUE);

		try {
			configuration.updateIfDifferent(configurationProperties);
		} catch (IOException e) {
			LOG.error(String.format("Error updating configuration properties %s!", featureConfiguration.getPid()), e);
		}
	}

	private Map<String, Configuration> getExistingConfigurations() throws IOException, InvalidSyntaxException {
		// @formatter:off
		return Optional.ofNullable(configurationAdmin.listConfigurations(constructConfigurationsFilter()))
				.map(Arrays::stream)
				.map(s -> s.collect(Collectors.toMap(Configuration::getPid, Function.identity())))
				.orElse(Map.of());
		// @formatter:on
	}

	private String constructConfigurationsFilter() {
		StringBuilder sb = new StringBuilder();

		sb.append("(");
		sb.append(CONFIGURATIONS_FILTER);
		sb.append("=");
		sb.append(Boolean.TRUE);
		sb.append(")");

		return sb.toString();
	}
}
