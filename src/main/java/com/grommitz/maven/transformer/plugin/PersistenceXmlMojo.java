package com.grommitz.maven.transformer.plugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
import org.codehaus.plexus.util.FileUtils;

import com.google.common.base.Strings;
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
	private String fromVersion;
	private String toVersion;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (Strings.isNullOrEmpty(toVersion) || Strings.isNullOrEmpty(fromVersion)) {
			throw new MojoExecutionException("toVersion and fromVersion must be set");
		}
		findPersistenceXmls();
		persistenceXmls.stream().forEach(f -> getLog().info("Transforming " + f));
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
			new FileReplace().doIt(f);
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

	class FileReplace {
		
		List<String> lines = new ArrayList<String>();
		String line = null;

		public void doIt(Path persistenceXml) {
			try {
				File f1 = persistenceXml.toFile();
				FileReader fr = new FileReader(f1);
				BufferedReader br = new BufferedReader(fr);
				while ((line = br.readLine()) != null) {
					//System.out.println("line = " + line);
					if (line.matches("^\\s*<jar-file>.*</jar-file>\\s*$")) {
						getLog().info("matched line " + line);
						line = line.replace(fromVersion, toVersion);
					}
					lines.add(line);
				}
				fr.close();
				br.close();

				FileWriter fw = new FileWriter(f1);
				BufferedWriter out = new BufferedWriter(fw);
				for(String s : lines)
					out.write(s);
				out.flush();
				out.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

}
