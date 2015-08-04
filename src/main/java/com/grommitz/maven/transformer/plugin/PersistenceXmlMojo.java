package com.grommitz.maven.transformer.plugin;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.CharSink;
import com.google.common.io.Files;

/**
 * 
 * Mojo which finds all persistence.xmls in the current project (and in modules if it
 * is a multi-module project) and updates the version numbers of the jar dependencies in 
 * them. Assumptions:<br>
 * * project uses standard maven directory layout<br>
 * * xml files are utf-8 encoded
 * 
 * @author Martin Charlesworth
 *
 */
@Mojo(name = "persistencexml")
public class PersistenceXmlMojo extends AbstractMojo {

	private List<Path> persistenceXmls = new ArrayList<>();
	private Path startingDir;
	private final List<String> directoriesToExplore = 
			Lists.newArrayList("src", "main", "test", "resources", "META-INF");
	@Parameter(property = "projectVersion", defaultValue = "${project.version}")
	private String projectVersion;
	@Parameter(property = "oldVersion")
	private String oldVersion;
	@Parameter(property = "newVersion")
	private String newVersion;
	@Parameter
	private List<String> excludeModules;
	
	@Component
	private Prompter prompter;
	
	@Component
	private MavenProject project;
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		startingDir = project.getBasedir().toPath();
		getLog().info("execute starting at " + startingDir.toString());
		
		if (project.getPackaging().equals("pom")) {
			getLog().info("Not processing parent pom");
			return;
		}
		if (excludeModules == null) {
			excludeModules = Lists.newArrayList();
			getLog().info("Including all modules");
		}
		excludeModules.stream().forEach(m -> getLog().info("Excluding " + m));
		
		findPersistenceXmls(project.getBasedir().toPath());
		if (persistenceXmls.isEmpty()) {
			throw new MojoFailureException("No persistence.xmls found under " + startingDir.toString());
		}

		try {
			while ( Strings.isNullOrEmpty(oldVersion) ) {
				oldVersion = prompter.prompt("Old version: ", projectVersion);
			}
			while ( Strings.isNullOrEmpty(newVersion) ) {
				newVersion = prompter.prompt("New version: ");
			}
		} catch (PrompterException e) {
			throw new MojoFailureException(e.getMessage());
		}
		transformPersistenceXmls();
		getLog().info("DONE");
	}

	public PersistenceXmlMojo oldVersion(String oldVer) {
		this.oldVersion = oldVer;
		return this;
	}

	public PersistenceXmlMojo newVersion(String newVer) {
		this.newVersion = newVer;
		return this;
	}

	private void transformPersistenceXmls() throws MojoFailureException {
		for (Path f : persistenceXmls) {
			getLog().info("Transforming " + f);
			new SearchAndReplace(f).replace(oldVersion, newVersion);
		};
	}

	public PersistenceXmlMojo startingDir(Path p) {
		this.startingDir = p;
		return this;
	}

	public PersistenceXmlMojo excludeModule(String moduleToExclude) {
		if (excludeModules == null) {
			excludeModules = Lists.newArrayList();
		}
		excludeModules.add(moduleToExclude);
		return this;
	}

	public List<Path> getPersistenceXmls() {
		return persistenceXmls;
	}

	private void findPersistenceXmls(Path root) {
		try {
			java.nio.file.Files.walkFileTree(root, new PersistenceXmlFinder());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class PersistenceXmlFinder extends SimpleFileVisitor<Path> {

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			String name = dir.getFileName().toString();
			if (java.nio.file.Files.isDirectory(dir)) {
				if (excludeModules.contains(name)) {
					getLog().info(name + " is an excluded module");
					return FileVisitResult.SKIP_SUBTREE;
				}
				if (dir.equals(startingDir)) {
					return FileVisitResult.CONTINUE;
				}
				if (dir.getParent().equals(startingDir)) {
					return FileVisitResult.CONTINUE;
				}
				if (directoriesToExplore.contains(name)) {
					return FileVisitResult.CONTINUE;
				}
				return FileVisitResult.SKIP_SUBTREE;
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			String name = file.getFileName().toString();
			if (name.equals("persistence.xml")) {
				persistenceXmls.add(file);
			}
			return FileVisitResult.CONTINUE;
		}
	}

	private class SearchAndReplace {

		private final Path persistenceXml;
		private boolean matched;
		
		public SearchAndReplace(Path persistenceXml) {
			this.persistenceXml = persistenceXml;
		}

		public void replace(String old, String replacement) throws MojoFailureException {
			getLog().info("Processing " + persistenceXml.toString());
			try {
				List<String> lines = Files.readLines(persistenceXml.toFile(), Charsets.UTF_8);
				lines = lines.stream().map(line -> {
					if (line.matches("^\\s*<jar-file>.*" + old + ".*</jar-file>\\s*$")) {
						getLog().info("matched line " + line.trim());
						line = line.replace(old, replacement);
						getLog().info("replaced with " + line.trim());
						matched = true;
					}
					return line;
				}).collect(Collectors.toList());

				if (!matched) {
					throw new MojoFailureException("Could not match any <jar-file> entries for version " + old);
				}
				
				CharSink sink = Files.asCharSink(persistenceXml.toFile(), Charsets.UTF_8);
				sink.writeLines(lines, System.getProperty("line.separator"));
			} catch (IOException e) {
				throw new MojoFailureException(e.getMessage());
			}

		}
	}

}
