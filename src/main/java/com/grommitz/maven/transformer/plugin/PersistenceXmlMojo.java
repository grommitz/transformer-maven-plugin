package com.grommitz.maven.transformer.plugin;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import com.google.common.collect.Lists;

/**
 * 
 * Mojo which finds all persistence.xmls in the current project (and in modules if it
 * is a multi-module project) and updates the version numbers of the jar dependencies in 
 * them.
 * 
 * @author Martin Charlesworth
 *
 */
@Mojo(name = "persistencexml") //, defaultPhase = LifecyclePhase.INSTALL)
public class PersistenceXmlMojo extends AbstractMojo {

	private List<Path> persistenceXmls = new ArrayList<>();
	private Path startingDir = Paths.get("").toAbsolutePath();
	private final List<String> directoriesToExplore = 
			Lists.newArrayList("src", "main", "test", "resources", "META-INF");

	public void execute() throws MojoExecutionException, MojoFailureException {
		findPersistenceXmls();
		persistenceXmls.stream().forEach(f -> getLog().info("Transforming " + f));
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
			Files.walkFileTree(startingDir, new PersistenceXmlFinder());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	class PersistenceXmlFinder extends SimpleFileVisitor<Path> {
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (dir.equals(startingDir)) {
				return FileVisitResult.CONTINUE;
			}
			if (dir.getParent().equals(startingDir)) {
				return FileVisitResult.CONTINUE;
			}
			if (!Files.isDirectory(dir)) {
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
	
}
