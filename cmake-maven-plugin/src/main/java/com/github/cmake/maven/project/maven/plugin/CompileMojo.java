/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.cmake.maven.project.maven.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * Goal which compiles project files generated by CMake.
 *
 * @author Gili Tzabari
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE)
public class CompileMojo extends CmakeMojo
{
	/**
	 * The build configuration (e.g. "Win32|Debug", "x64|Release").
	 */
	@Parameter
	private String config;
	/**
	 * The target to build.
	 */
	@Parameter
	private String target;
	/**
	 * The directory containing the project file.
	 */
	@Parameter(required = true)
	private File projectDirectory;

	/**
	 * Creates a new instance.
	 *
	 * @param project       an instance of {@code MavenProject}
	 * @param pluginManager an instance of {@code PluginManager}
	 * @param session       an instance of {@code MavenSession}
	 */
	@Inject
	public CompileMojo(MavenProject project, MavenSession session, BuildPluginManager pluginManager)
	{
		super(project, session, pluginManager);
	}

	@Override
	public void execute()
		throws MojoExecutionException
	{
		try
		{
			if (!projectDirectory.exists())
				throw new MojoExecutionException(projectDirectory.getAbsolutePath() + " does not exist");
			if (!projectDirectory.isDirectory())
				throw new MojoExecutionException(projectDirectory.getAbsolutePath() + " must be a directory");

			downloadBinariesIfNecessary();

			ProcessBuilder processBuilder = new ProcessBuilder();
			overrideEnvironmentVariables(processBuilder);

			String cmakePath = getBinaryPath("cmake", processBuilder).toString();
			processBuilder.command().add(cmakePath);

			Collections.addAll(processBuilder.command(), "--build", projectDirectory.getPath());
			if (target != null)
				Collections.addAll(processBuilder.command(), "--target", target);
			if (config != null)
				Collections.addAll(processBuilder.command(), "--config", config);
			addOptions(processBuilder);

			Log log = getLog();
			if (log.isDebugEnabled())
			{
				log.debug("projectDirectory: " + projectDirectory);
				log.debug("target: " + target);
				log.debug("config: " + config);
				log.debug("Environment: " + processBuilder.environment());
				log.debug("Command-line: " + processBuilder.command());
			}
			int returnCode = Mojos.waitFor(processBuilder, getLog());
			if (returnCode != 0)
				throw new MojoExecutionException("Return code: " + returnCode);
		}
		catch (InterruptedException | IOException e)
		{
			throw new MojoExecutionException("", e);
		}
	}
}
