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

import static com.kentyou.featurelauncher.impl.FeatureLauncherImplConstants.FRAMEWORK_STORAGE_CLEAN_TESTONLY;
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME;
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME;
import static com.kentyou.featurelauncher.impl.repository.ArtifactRepositoryConstants.LOCAL_ARTIFACT_REPOSITORY_PATH;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.REMOTE_ARTIFACT_REPOSITORY_NAME;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.featurelauncher.FeatureLauncher;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;

import com.kentyou.featurelauncher.cli.CommandLine.ArgGroup;
import com.kentyou.featurelauncher.cli.CommandLine.Command;
import com.kentyou.featurelauncher.cli.CommandLine.ITypeConverter;
import com.kentyou.featurelauncher.cli.CommandLine.Model.CommandSpec;
import com.kentyou.featurelauncher.cli.CommandLine.Option;
import com.kentyou.featurelauncher.cli.CommandLine.Parameters;
import com.kentyou.featurelauncher.cli.CommandLine.Spec;
import com.kentyou.featurelauncher.impl.util.ArtifactRepositoryUtil;
import com.kentyou.featurelauncher.impl.util.ServiceLoaderUtil;

/**
 * 160.4.2.4 The Feature Launcher Command Line
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 8, 2024
 */

// @formatter:off
@Command(
		name = "", 
		description = FeatureLauncherCli.DESCRIPTION, 
		mixinStandardHelpOptions = true, 
		version = FeatureLauncherCli.VERSION, 
		header = FeatureLauncherCli.HEADING, 
		headerHeading = "@|bold,underline Usage|@:%n%n", 
		descriptionHeading = "%n@|bold,underline Description|@:%n%n", 
		parameterListHeading = "%n@|bold,underline Parameters|@:%n", 
		optionListHeading = "%n@|bold,underline Options|@:%n", 
		sortOptions = false, 
		abbreviateSynopsis = true)
// @formatter:on
public class FeatureLauncherCli implements Runnable {
	static final String HEADING = "The Feature Launcher Command Line 1.0";
	static final String DESCRIPTION = "In order to support the Zero Code goal of the "
			+ "Feature Launcher Service it is not sufficient to provide a Java API, "
			+ "it must also be possible to launch a feature from the command line "
			+ "in a standard way. To support this implementations of the Feature "
			+ "Launcher provides an executable JAR file which allows a Feature "
			+ "to be launched from the command line.";
	static final String VERSION = HEADING + " 1.0";
	static final int EXITCODE_SUCCESS = 0;
	static final String REMOTE_ARTIFACT_REPOSITORY_URI_VALUE = "REMOTE_ARTIFACT_REPOSITORY_URI";

	@ArgGroup(exclusive = true, multiplicity = "1", order = -10)
	private FeatureFile featureFile;

	@Option(names = { "-a",
			"--artifact-repository" }, paramLabel = "uri [key=value]", description = "Specifies an artifact repository URI and optionally one "
					+ "or more configuration properties for that artifact repository, "
					+ "such as those described in Remote Repositories on page 1381. "
					+ "This property may be repeated to add more than one artifact "
					+ "repository.", order = -9, mapFallbackValue = REMOTE_ARTIFACT_REPOSITORY_URI_VALUE)
	private Map<Object, Object> remoteArtifactRepositories;

	@Option(names = { "-d", "--decorator" }, paramLabel = "class name", description = "Provides "
			+ "the name of a decorator class that should be used when launching "
			+ "the feature. The decorator class must be public, available on the "
			+ "classpath, and have a public zero-argument constructor. This "
			+ "property may be repeated to add more than one decorator.", order = -8)
	private List<Class<?>> decorators;

	@Option(names = { "-e",
			"--extension-handler" }, paramLabel = "extension name=class name", description = "Provides the name of an extension, and the extension handler "
					+ "class that should be used to handle the extension when launching "
					+ "the feature. The extension handler class must be public, available "
					+ "on the classpath, and have a public zero-argument constructor. This "
					+ "property may be repeated to add more than one extension handler.", order = -7)
	private Map<String, Class<?>> extensionHandlers;

	@Option(names = { "-l",
			"--launch-property" }, paramLabel = "key=value", description = "Provides one or more launch properties that should be passed "
					+ "to the framework when it is launched.", order = -6)
	private Map<String, String> frameworkProperties;

	@Option(names = { "-v",
			"--variable-override" }, paramLabel = "key=value", description = "Provides one or more variables that should be used to set or "
					+ "override variables defined in the feature.", order = -5)
	private Map<String, Object> variables;

	@Option(names = { "-c",
			"--configuration" }, paramLabel = "key=value", description = "Provides one or more configuration properties that should "
					+ "be used to control implementation specific behaviour.", order = -4)
	private Map<String, Object> configuration;

	@Option(names = {
			"--dry-run" }, description = "Evaluates all options, processes them and displays output, but does not launch framework. Hidden option used for testing", hidden = true)
	private boolean dryRun;

	@Spec
	private CommandSpec commandSpec;

	private FeatureService featureService;
	private FeatureLauncher featureLauncher;

	private Path defaultM2RepositoryPath;
	private Map<String, ArtifactRepository> defaultArtifactRepositories;

	private Path defaultFrameworkStorageDir;

	private Map<String, String> defaultFrameworkProperties;

	public void run() {
		if (commandSpec.commandLine().getParseResult().expandedArgs().isEmpty()) {
			commandSpec.commandLine().usage(commandSpec.commandLine().getOut());
			return;
		}

		Path featureFilePath = (featureFile.featureFilePathOption != null) ? featureFile.featureFilePathOption
				: featureFile.featureFilePathParameter;

		remoteArtifactRepositories = (remoteArtifactRepositories != null) ? remoteArtifactRepositories
				: Collections.emptyMap();
		decorators = (decorators != null) ? decorators : Collections.emptyList();
		extensionHandlers = (extensionHandlers != null) ? extensionHandlers : Collections.emptyMap();
		frameworkProperties = (frameworkProperties != null) ? frameworkProperties : Collections.emptyMap();
		variables = (variables != null) ? variables : Collections.emptyMap();
		configuration = (configuration != null) ? configuration : Collections.emptyMap();

		this.featureService = ServiceLoaderUtil.loadFeatureService();

		this.featureLauncher = ServiceLoaderUtil.loadFeatureLauncherService();

		Feature feature;

		try (Reader featureFileReader = Files.newBufferedReader(featureFilePath)) {
			feature = featureService.readFeature(featureFileReader);
		} catch (IOException e) {
			throw new FeatureLauncherCliException("Error reading feature!", e);
		}

		try {
			this.defaultM2RepositoryPath = ArtifactRepositoryUtil.getDefaultM2RepositoryPath();

			this.defaultArtifactRepositories = ArtifactRepositoryUtil.getDefaultArtifactRepositories(featureLauncher,
					defaultM2RepositoryPath);

		} catch (IOException e) {
			throw new FeatureLauncherCliException("Could not create default artifact repositories!", e);
		}

		Map<String, ArtifactRepository> artifactRepositories = getArtifactRepositories(featureLauncher,
				remoteArtifactRepositories, defaultArtifactRepositories);

		try {
			this.defaultFrameworkStorageDir = createDefaultFrameworkStorageDir();
		} catch (IOException e) {
			throw new FeatureLauncherCliException("Could not create default framework storage directory!", e);
		}

		this.defaultFrameworkProperties = getDefaultFrameworkProperties(defaultFrameworkStorageDir);

		frameworkProperties = !frameworkProperties.isEmpty() ? frameworkProperties : defaultFrameworkProperties;

		System.out.println(String.format("Launching feature %s", feature.getID()));
		System.out.println("------------------------------------------------------------------------");

		FeatureLauncher.LaunchBuilder featureLaunchBuilder = featureLauncher.launch(feature);

		System.out.println("Using artifact repositories: ");
		for (Map.Entry<String, ArtifactRepository> artifactRepositoryEntry : artifactRepositories.entrySet()) {
			System.out.println(
					String.format("%s = %s", artifactRepositoryEntry.getKey(), artifactRepositoryEntry.getValue()));
		}
		System.out.println("------------------------------------------------------------------------");

		featureLaunchBuilder.withRepository(artifactRepositories.remove(DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME));
		for (ArtifactRepository artifactRepository : artifactRepositories.values()) {
			featureLaunchBuilder.withRepository(artifactRepository);
		}

		if (!frameworkProperties.isEmpty()) {
			System.out.println("Using framework properties: ");
			for (Map.Entry<String, String> frameworkPropertyEntry : frameworkProperties.entrySet()) {
				System.out.println(
						String.format("%s = %s", frameworkPropertyEntry.getKey(), frameworkPropertyEntry.getValue()));
			}
			System.out.println("------------------------------------------------------------------------");

			featureLaunchBuilder.withFrameworkProperties(frameworkProperties);
		}

		if (!configuration.isEmpty()) {
			System.out.println("Using configuration: ");
			for (Map.Entry<String, Object> configurationEntry : configuration.entrySet()) {
				System.out
						.println(String.format("%s = %s", configurationEntry.getKey(), configurationEntry.getValue()));
			}
			System.out.println("------------------------------------------------------------------------");

			featureLaunchBuilder.withConfiguration(configuration);
		}

		if (!variables.isEmpty()) {
			System.out.println("Using variables: ");
			for (Map.Entry<String, Object> variableEntry : variables.entrySet()) {
				System.out.println(String.format("%s = %s", variableEntry.getKey(), variableEntry.getValue()));
			}
			System.out.println("------------------------------------------------------------------------");

			featureLaunchBuilder.withVariables(variables);
		}

		if (!decorators.isEmpty()) {
			System.out.println("Using decorators: ");
			for (Class<?> decorator : decorators) {
				System.out.println(String.format("%s", decorator));
			}
			System.out.println("------------------------------------------------------------------------");

			for (Class<?> decorator : decorators) {
				try {
					featureLaunchBuilder
							.withDecorator((FeatureDecorator) decorator.getDeclaredConstructor().newInstance());
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new FeatureLauncherCliException("Could not create instance of FeatureDecorator!", e);
				}
			}
		}

		if (!extensionHandlers.isEmpty()) {
			System.out.println("Using extension handlers: ");
			for (Map.Entry<String, Class<?>> extensionHandlerEntry : extensionHandlers.entrySet()) {
				System.out.println(
						String.format("%s = %s", extensionHandlerEntry.getKey(), extensionHandlerEntry.getValue()));
			}
			System.out.println("------------------------------------------------------------------------");

			for (Map.Entry<String, Class<?>> extensionHandler : extensionHandlers.entrySet()) {
				try {
					featureLaunchBuilder.withExtensionHandler(extensionHandler.getKey(),
							(FeatureExtensionHandler) extensionHandler.getValue().getDeclaredConstructor()
									.newInstance());
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new FeatureLauncherCliException("Could not create instance of FeatureExtensionHandler!", e);
				}
			}
		}

		if (!dryRun) {
			Framework osgiFramework = featureLaunchBuilder.launchFramework();

			try {
				osgiFramework.waitForStop(0);
			} catch (InterruptedException e) {
				System.err.println("Error stopping framework!");
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args);

		System.exit(exitCode);
	}

	private Map<String, ArtifactRepository> getArtifactRepositories(ArtifactRepositoryFactory artifactRepositoryFactory,
			Map<Object, Object> userSpecifiedRemoteArtifactRepositories,
			Map<String, ArtifactRepository> defaultArtifactRepositories) {

		Map<String, ArtifactRepository> artifactRepositories = new HashMap<>();
		artifactRepositories.put(DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME,
				defaultArtifactRepositories.get(DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME));

		if (!userSpecifiedRemoteArtifactRepositories.isEmpty()) {
			Map<URI, Map<String, Object>> processedUserSpecifiedRemoteArtifactRepositories = processUserSpecifiedRemoteArtifactRepositories(
					userSpecifiedRemoteArtifactRepositories);

			System.out.println(processedUserSpecifiedRemoteArtifactRepositories);

			for (Map.Entry<URI, Map<String, Object>> processedUserSpecifiedRemoteArtifactRepositoryEntry : processedUserSpecifiedRemoteArtifactRepositories
					.entrySet()) {

				Map<String, Object> configurationProperties = processedUserSpecifiedRemoteArtifactRepositoryEntry
						.getValue();
				configurationProperties.putIfAbsent(REMOTE_ARTIFACT_REPOSITORY_NAME,
						DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME);
				configurationProperties.putIfAbsent(LOCAL_ARTIFACT_REPOSITORY_PATH, defaultM2RepositoryPath.toString());

				ArtifactRepository userSpecifiedRemoteArtifactRepository = artifactRepositoryFactory.createRepository(
						processedUserSpecifiedRemoteArtifactRepositoryEntry.getKey(), configurationProperties);

				artifactRepositories.put(String.valueOf(configurationProperties.get(REMOTE_ARTIFACT_REPOSITORY_NAME)),
						userSpecifiedRemoteArtifactRepository);
			}
		} else {
			artifactRepositories.put(DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME,
					defaultArtifactRepositories.get(DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME));
		}

		return artifactRepositories;
	}

	private Map<URI, Map<String, Object>> processUserSpecifiedRemoteArtifactRepositories(
			Map<Object, Object> userSpecifiedRemoteArtifactRepositories) {
		Map<URI, Map<String, Object>> processedUserSpecifiedRemoteArtifactRepositories = new HashMap<>();

		URI lastFoundURI = null;

		for (Map.Entry<Object, Object> userSpecifiedRemoteArtifactRepositoryEntry : userSpecifiedRemoteArtifactRepositories
				.entrySet()) {
			if (REMOTE_ARTIFACT_REPOSITORY_URI_VALUE
					.equals(String.valueOf(userSpecifiedRemoteArtifactRepositoryEntry.getValue()))) {
				lastFoundURI = URI.create(String.valueOf(userSpecifiedRemoteArtifactRepositoryEntry.getKey()));
				processedUserSpecifiedRemoteArtifactRepositories.put(lastFoundURI, new HashMap<>());

			} else if (lastFoundURI != null) {
				Map<String, Object> configurationProperties = processedUserSpecifiedRemoteArtifactRepositories
						.get(lastFoundURI);
				configurationProperties.put(String.valueOf(userSpecifiedRemoteArtifactRepositoryEntry.getKey()),
						userSpecifiedRemoteArtifactRepositoryEntry.getValue());
			}
		}

		return processedUserSpecifiedRemoteArtifactRepositories;
	}

	private Map<String, String> getDefaultFrameworkProperties(Path defaultFrameworkStorageDir) {
		return Map.of(Constants.FRAMEWORK_STORAGE, defaultFrameworkStorageDir.toString(),
				Constants.FRAMEWORK_STORAGE_CLEAN, FRAMEWORK_STORAGE_CLEAN_TESTONLY);
	}

	private Path createDefaultFrameworkStorageDir() throws IOException {
		return Files.createTempDirectory("osgi_");
	}

	private static boolean isJsonFile(Path p) {
		if ((p != null) && Files.isRegularFile(p)) {
			String fileName = p.getFileName().toString();
			return "json".equalsIgnoreCase(fileName.substring(fileName.lastIndexOf(".") + 1));
		}

		return false;
	}

	static class FeatureFile {
		@Parameters(arity = "1", paramLabel = "feature file path", description = "Specifies "
				+ "the location of a file containing the feature JSON. Feature files in "
				+ "this directory: ${COMPLETION-CANDIDATES}", converter = FeatureFileConverter.class, completionCandidates = FeatureFileCompleter.class)
		Path featureFilePathParameter;

		@Option(names = { "-f",
				"--feature-file" }, arity = "1", paramLabel = "feature file path", description = "Specifies "
						+ "the location of a file containing the feature JSON. If used "
						+ "then the <feature json> must be omitted. This provides the "
						+ "feature that must be launched. Feature files in this "
						+ "directory: ${COMPLETION-CANDIDATES}", order = -10, converter = FeatureFileConverter.class, completionCandidates = FeatureFileCompleter.class)
		Path featureFilePathOption;
	}

	static class FeatureFileCompleter implements Iterable<String> {
		@Override
		public Iterator<String> iterator() {
			try {
				// @formatter:off
				return Files.list(Paths.get("."))
						.filter(p -> isJsonFile(p))
						.map(p -> p.toString())
						.iterator();
				// @formatter:on
			} catch (IOException e) {
				System.err.println("Error listing feature files!");
				e.printStackTrace();
			}
			return Collections.emptyIterator();
		}
	}

	static class FeatureFileConverter implements ITypeConverter<Path> {

		@Override
		public Path convert(String value) throws Exception {
			Path result = Paths.get(value);

			if (!isJsonFile(result)) {
				throw new FeatureLauncherCliException("File not found! Provide path to existing feature file");
			}

			return result;
		}
	}
}
