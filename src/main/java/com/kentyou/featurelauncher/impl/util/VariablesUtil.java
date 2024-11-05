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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;

/**
 * Util for variables substitution operations.
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Nov 2, 2024
 */
public enum VariablesUtil {
	INSTANCE;

	public Map<String, Object> maybeSubstituteVariables(Map<String, Object> properties, Map<String, Object> variables) {
		if (!properties.isEmpty() && !variables.isEmpty()) {
			StringSubstitutor variablesSubstitutor = new StringSubstitutor(variables);
			Map<String, Object> substituted = new HashMap<>();

			for (Map.Entry<String, Object> propertyEntry : properties.entrySet()) {
				String propertyName = String.valueOf(propertyEntry.getKey());

				Object rawPropertyValue = propertyEntry.getValue();

				Object convertedPropertyValue = null;

				if (rawPropertyValue != null) {
					convertedPropertyValue = convertSubstitutedValue(rawPropertyValue.getClass(),
							variablesSubstitutor.replace(rawPropertyValue));
				}

				substituted.put(propertyName, convertedPropertyValue);
			}

			return substituted;
		}

		return new HashMap<>(properties);
	}

	private Object convertSubstitutedValue(Class<?> rawPropertyValueType, String convertedPropertyValue) {
		if (rawPropertyValueType.isAssignableFrom(BigDecimal.class)) {
			return new BigDecimal(convertedPropertyValue);

		} else if (rawPropertyValueType.isAssignableFrom(Boolean.class)) {
			return Boolean.valueOf(convertedPropertyValue);

		} else {
			return convertedPropertyValue;
		}
	}
}
