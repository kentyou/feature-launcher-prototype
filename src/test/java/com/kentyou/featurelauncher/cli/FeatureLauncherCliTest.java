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
package com.kentyou.featurelauncher.cli;

import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME;
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.REMOTE_ARTIFACT_REPOSITORY_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.REMOTE_ARTIFACT_REPOSITORY_NAME;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link com.kentyou.featurelauncher.cli.FeatureLauncherCli}
 * 
 * As defined in: "160.4.2.4 The Feature Launcher Command Line"
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 10, 2024
 */
public class FeatureLauncherCliTest {
	private static final String FEATURE_FILE_ID = "com.kentyou.featurelauncher:gogo-console-feature:1.0";
	private static final String DECORATOR_CLASS_NAME = "com.kentyou.featurelauncher.impl.decorator.FeatureDecoratorImpl";
	private static final String EXTENSION_HANDLER_NAME = "LaunchFrameworkFeatureExtensionHandler";
	private static final String EXTENSION_HANDLER_CLASS_NAME = "com.kentyou.featurelauncher.impl.decorator.LaunchFrameworkFeatureExtensionHandlerImpl";
	private static final String LAUNCH_PROPERTY_1_KEY = "key1";
	private static final String LAUNCH_PROPERTY_1_VALUE = "value1";
	private static final String LAUNCH_PROPERTY_2_KEY = "key2";
	private static final String LAUNCH_PROPERTY_2_VALUE = "value2";
	private static final String VARIABLE_1_KEY = "key1";
	private static final String VARIABLE_1_VALUE = "value1";
	private static final String VARIABLE_2_KEY = "key2";
	private static final String VARIABLE_2_VALUE = "value2";
	private static final String CONFIGURATION_1_KEY = "key1";
	private static final String CONFIGURATION_1_VALUE = "value1";
	private static final String CONFIGURATION_2_KEY = "key2";
	private static final String CONFIGURATION_2_VALUE = "value2";

	private static String FEATURE_FILE_PATH;

	final PrintStream originalOut = System.out;
	final PrintStream originalErr = System.err;
	final ByteArrayOutputStream out = new ByteArrayOutputStream();
	final ByteArrayOutputStream err = new ByteArrayOutputStream();

	@BeforeAll
	public static void setUpFeatureFilePath() throws URISyntaxException {
		FEATURE_FILE_PATH = Paths
				.get(FeatureLauncherCliTest.class.getResource("/features/gogo-console-feature.json").toURI())
				.toString();
	}

	@BeforeEach
	public void setUpStreams() {
		out.reset();
		err.reset();
		System.setOut(new PrintStream(out));
		System.setErr(new PrintStream(err));
	}

	@AfterEach
	public void restoreStreams() {
		System.setOut(originalOut);
		System.setErr(originalErr);
	}

	@Test
	public void testFeatureFileParameter() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertEquals("", err.toString());
	}

	@Test
	public void testFeatureFileOptionShort() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("-f=");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertEquals("", err.toString());
	}

	@Test
	public void testFeatureFileOptionLong() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("--feature-file=");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertEquals("", err.toString());
	}

	@Test
	public void testMissingFeatureFileParameter() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(2, exitCode);

		assertTrue(err.toString().contains(
				"Missing required argument (specify one of these): (feature file path | -f=feature file path)"));
	}

	@Test
	public void testMissingRequiredParameterForFeatureFileOptionShort() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("-f");

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(2, exitCode);

		assertTrue(
				err.toString().contains("Missing required parameter for option '--feature-file' (feature file path)"));
	}

	@Test
	public void testMissingRequiredParameterForFeatureFileOptionLong() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("--feature-file");

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(2, exitCode);

		assertTrue(
				err.toString().contains("Missing required parameter for option '--feature-file' (feature file path)"));
	}

	@Test
	public void testInvalidValueForFeatureFileOptionShort() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("-f=");

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(2, exitCode);

		assertTrue(err.toString().contains("Invalid value for option '--feature-file'"));
	}

	@Test
	public void testInvalidValueForFeatureFileOptionLong() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("--feature-file=");

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(2, exitCode);

		assertTrue(err.toString().contains("Invalid value for option '--feature-file'"));
	}

	@Test
	public void testArtifactRepositoryOptionShort() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("-a=");
		args.append(REMOTE_ARTIFACT_REPOSITORY_URI);
		args.append(" ");
		args.append("-a=");
		args.append(REMOTE_ARTIFACT_REPOSITORY_NAME);
		args.append("=");
		args.append(DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME);
		args.append(" ");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using artifact repositories:"));
		assertTrue(out.toString().contains(String.format("repositoryURI=%s", REMOTE_ARTIFACT_REPOSITORY_URI)));
		assertTrue(out.toString().contains(String.format("name=%s", DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME)));

		assertEquals("", err.toString());
	}

	@Test
	public void testArtifactRepositoryOptionLong() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("--artifact-repository=");
		args.append(REMOTE_ARTIFACT_REPOSITORY_URI);
		args.append(" ");
		args.append("--artifact-repository=");
		args.append(REMOTE_ARTIFACT_REPOSITORY_NAME);
		args.append("=");
		args.append(DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME);
		args.append(" ");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using artifact repositories:"));
		assertTrue(out.toString().contains(String.format("repositoryURI=%s", REMOTE_ARTIFACT_REPOSITORY_URI)));
		assertTrue(out.toString().contains(String.format("name=%s", DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME)));

		assertEquals("", err.toString());
	}

	@Test
	public void testDecoratorOptionShort() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("-d=");
		args.append(DECORATOR_CLASS_NAME);
		args.append(" ");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using decorators:"));
		assertTrue(out.toString().contains(String.format("class %s", DECORATOR_CLASS_NAME)));

		assertEquals("", err.toString());
	}

	@Test
	public void testDecoratorOptionLong() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("--decorator=");
		args.append(DECORATOR_CLASS_NAME);
		args.append(" ");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using decorators:"));
		assertTrue(out.toString().contains(String.format("class %s", DECORATOR_CLASS_NAME)));

		assertEquals("", err.toString());
	}

	@Test
	public void testExtensionHandlerOptionShort() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("-e=");
		args.append(EXTENSION_HANDLER_NAME);
		args.append("=");
		args.append(EXTENSION_HANDLER_CLASS_NAME);
		args.append(" ");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using extension handlers:"));
		assertTrue(out.toString()
				.contains(String.format("%s = class %s", EXTENSION_HANDLER_NAME, EXTENSION_HANDLER_CLASS_NAME)));

		assertEquals("", err.toString());
	}

	@Test
	public void testExtensionHandlerOptionLong() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("--extension-handler=");
		args.append(EXTENSION_HANDLER_NAME);
		args.append("=");
		args.append(EXTENSION_HANDLER_CLASS_NAME);
		args.append(" ");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using extension handlers:"));
		assertTrue(out.toString()
				.contains(String.format("%s = class %s", EXTENSION_HANDLER_NAME, EXTENSION_HANDLER_CLASS_NAME)));

		assertEquals("", err.toString());
	}

	@Test
	public void testLaunchPropertyOptionShort() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("-l=");
		args.append(LAUNCH_PROPERTY_1_KEY);
		args.append("=");
		args.append(LAUNCH_PROPERTY_1_VALUE);
		args.append(" ");
		args.append("-l=");
		args.append(LAUNCH_PROPERTY_2_KEY);
		args.append("=");
		args.append(LAUNCH_PROPERTY_2_VALUE);
		args.append(" ");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using framework properties:"));
		assertTrue(out.toString().contains(String.format("%s = %s", LAUNCH_PROPERTY_1_KEY, LAUNCH_PROPERTY_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", LAUNCH_PROPERTY_2_KEY, LAUNCH_PROPERTY_2_VALUE)));

		assertEquals("", err.toString());
	}

	@Test
	public void testLaunchPropertyOptionLong() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("--launch-property=");
		args.append(LAUNCH_PROPERTY_1_KEY);
		args.append("=");
		args.append(LAUNCH_PROPERTY_1_VALUE);
		args.append(" ");
		args.append("--launch-property=");
		args.append(LAUNCH_PROPERTY_2_KEY);
		args.append("=");
		args.append(LAUNCH_PROPERTY_2_VALUE);
		args.append(" ");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using framework properties:"));
		assertTrue(out.toString().contains(String.format("%s = %s", LAUNCH_PROPERTY_1_KEY, LAUNCH_PROPERTY_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", LAUNCH_PROPERTY_2_KEY, LAUNCH_PROPERTY_2_VALUE)));

		assertEquals("", err.toString());
	}

	@Test
	public void testVariableOverrideOptionShort() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("-v=");
		args.append(VARIABLE_1_KEY);
		args.append("=");
		args.append(VARIABLE_1_VALUE);
		args.append(" ");
		args.append("-v=");
		args.append(VARIABLE_2_KEY);
		args.append("=");
		args.append(VARIABLE_2_VALUE);
		args.append(" ");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using variables:"));
		assertTrue(out.toString().contains(String.format("%s = %s", VARIABLE_1_KEY, VARIABLE_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", VARIABLE_2_KEY, VARIABLE_2_VALUE)));

		assertEquals("", err.toString());
	}

	@Test
	public void testVariableOverrideOptionLong() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("--variable-override=");
		args.append(VARIABLE_1_KEY);
		args.append("=");
		args.append(VARIABLE_1_VALUE);
		args.append(" ");
		args.append("--variable-override=");
		args.append(VARIABLE_2_KEY);
		args.append("=");
		args.append(VARIABLE_2_VALUE);
		args.append(" ");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using variables:"));
		assertTrue(out.toString().contains(String.format("%s = %s", VARIABLE_1_KEY, VARIABLE_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", VARIABLE_2_KEY, VARIABLE_2_VALUE)));

		assertEquals("", err.toString());
	}

	@Test
	public void testConfigurationOptionShort() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("-c=");
		args.append(CONFIGURATION_1_KEY);
		args.append("=");
		args.append(CONFIGURATION_1_VALUE);
		args.append(" ");
		args.append("-c=");
		args.append(CONFIGURATION_2_KEY);
		args.append("=");
		args.append(CONFIGURATION_2_VALUE);
		args.append(" ");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using configuration:"));
		assertTrue(out.toString().contains(String.format("%s = %s", CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE)));

		assertEquals("", err.toString());
	}

	@Test
	public void testConfigurationOptionLong() {
		StringBuilder args = new StringBuilder();
		args.append("--dry-run");
		args.append(" ");
		args.append("--configuration=");
		args.append(CONFIGURATION_1_KEY);
		args.append("=");
		args.append(CONFIGURATION_1_VALUE);
		args.append(" ");
		args.append("--configuration=");
		args.append(CONFIGURATION_2_KEY);
		args.append("=");
		args.append(CONFIGURATION_2_VALUE);
		args.append(" ");
		args.append(FEATURE_FILE_PATH);

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute((args.toString().split(" ")));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using configuration:"));
		assertTrue(out.toString().contains(String.format("%s = %s", CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE)));

		assertEquals("", err.toString());
	}
}
