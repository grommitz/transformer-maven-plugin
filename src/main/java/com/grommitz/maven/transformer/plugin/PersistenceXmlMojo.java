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
import org.apache.maven.plugins.annotations.Mojo;

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
	private Path startingDir = Paths.get("").toAbsolutePath();
	private final List<String> directoriesToExplore = 
			Lists.newArrayList("src", "main", "test", "resources", "META-INF");
	private String fromVersion;
	private String toVersion;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (Strings.isNullOrEmpty(toVersion) || Strings.isNullOrEmpty(fromVersion)) {
			throw new MojoExecutionException("toVersion and fromVersion must be set");
		}
		findPersistenceXmls();
		transformPersistenceXmls();
		getLog().info("DONE");
	}

	public PersistenceXmlMojo fromVersion(String fromVer) {
		this.fromVersion= fromVer;
		return this;
	}

	public PersistenceXmlMojo toVersion(String toVer) {
		this.toVersion= toVer;
		return this;
	}

	private void transformPersistenceXmls() {
		persistenceXmls.stream().forEach(f -> {
			getLog().info("Transforming " + f);
			try {
				new SearchAndReplace(f).replace(fromVersion, toVersion);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	public PersistenceXmlMojo startingDir(Path p) {
		this.startingDir = p;
		return this;
	}

	public List<Path> getPersistenceXmls() {
		return persistenceXmls;
	}

	private void findPersistenceXmls() {
		try {
			java.nio.file.Files.walkFileTree(startingDir, new PersistenceXmlFinder());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class PersistenceXmlFinder extends SimpleFileVisitor<Path> {

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (dir.equals(startingDir)) {
				return FileVisitResult.CONTINUE;
			}
			if (dir.getParent().equals(startingDir)) {
				return FileVisitResult.CONTINUE;
			}
			if (!java.nio.file.Files.isDirectory(dir)) {
				return FileVisitResult.CONTINUE;
			}
			String name = dir.getFileName().toString();
			if (directoriesToExplore.contains(name)) {
				return FileVisitResult.CONTINUE;
			}
			return FileVisitResult.SKIP_SUBTREE;
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

		public SearchAndReplace(Path persistenceXml) {
			this.persistenceXml = persistenceXml;
		}

		public void replace(String old, String replacement) throws IOException {
			List<String> lines = Files.readLines(persistenceXml.toFile(), Charsets.UTF_8);

			lines = lines.stream().map(line -> {
				if (line.matches("^\\s*<jar-file>.*</jar-file>\\s*$")) {
					getLog().info("matched line " + line);
					line = line.replace(old, replacement);
					getLog().info("replaced with " + line);
				}
				return line;
			}).collect(Collectors.toList());

			CharSink sink = Files.asCharSink(persistenceXml.toFile(), Charsets.UTF_8);
			sink.writeLines(lines, System.getProperty("line.separator"));
		}
	}

}
