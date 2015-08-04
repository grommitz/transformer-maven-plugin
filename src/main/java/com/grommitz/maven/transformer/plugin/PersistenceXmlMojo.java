package com.grommitz.maven.transformer.plugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;

/**
 * 
 * @author Martin Charlesworth
 *
 */
@Mojo(name = "persistencexml") //, defaultPhase = LifecyclePhase.INSTALL)
public class PersistenceXmlMojo extends AbstractMojo {

	public void execute() throws MojoExecutionException, MojoFailureException {
		List<Path> files = findPersistenceXmls();
		files.stream().forEach(f -> System.out.println("Transforming " + f));
		files.stream().forEach(f -> getLog().info("Transforming " + f));
	}

	private List<Path> findPersistenceXmls() {
		List<Path> results = new ArrayList<>();
		results.add(Paths.get("de2-core/src/main/resources/META-INF/persistence.xml"));
		results.add(Paths.get("de2-reporting/src/main/resources/META-INF/persistence.xml"));
		return results;
	}
	
}
