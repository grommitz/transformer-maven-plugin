package com.grommitz.maven.transformer.plugin;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;

/**
 * Unit tests for {@link PersistenceXmlMojo}
 *
 * @author Martin Charlesworth
 *
 */
public class PersistenceXmlMojoTest {

	private Path startingDir;
	
	@Before
	public void setUp() throws Exception {
		startingDir = Paths.get("/tmp").resolve("mojotest");
		setupTempDirectoryStructure();
	}
	
	@After
	public void tearDown() throws IOException {
		FileUtils.deleteDirectory(startingDir.toString());
	}
	
	@Test
	public void shouldFindPersistenceXmls() throws Exception {
		PersistenceXmlMojo mojo = new PersistenceXmlMojo()
				.startingDir(startingDir).oldVersion("1").newVersion("2");
		mojo.execute();
		assertThat(mojo.getPersistenceXmls().size(), is(3));
	}
	
	@Test
	public void shouldReplaceVersionNumberInPersistenceXmls() throws Exception {
		PersistenceXmlMojo mojo = new PersistenceXmlMojo()
				.startingDir(startingDir).oldVersion("1.0-SNAPSHOT").newVersion("2.0-SNAPSHOT");
		mojo.execute();
		String content = new String(Files.readAllBytes(mojo.getPersistenceXmls().get(0)), Charsets.UTF_8);
		assertThat(content.contains("2.0-SNAPSHOT"), is(true));
		assertThat(content.contains("1.0-SNAPSHOT"), is(false));
	}
	
	@Test ( expected = MojoFailureException.class )
	public void shouldThrowIfVersionNumberNotMatched() throws Exception {
		new PersistenceXmlMojo()
				.startingDir(startingDir)
				.oldVersion("bad.version")
				.newVersion("2.0-SNAPSHOT")
				.execute();
	}
	
	@Test
	public void shouldExcludeNamedModules() throws Exception {
		PersistenceXmlMojo mojo = new PersistenceXmlMojo().startingDir(startingDir)
				.oldVersion("1.0-SNAPSHOT").newVersion("2.0-SNAPSHOT")
				.excludeModule("module2");
		mojo.execute();
		assertThat(mojo.getPersistenceXmls().size(), is(2));
	}
	
	private void setupTempDirectoryStructure() throws IOException {
		Files.createDirectories(startingDir);
		createFilesUnder(startingDir);

		Path module1 = startingDir.resolve("module1");
		Files.createDirectories(module1);
		createFilesUnder(module1);
		
		Path module2 = startingDir.resolve("module2");
		Files.createDirectories(module2);
		createFilesUnder(module2);
	}
	
	private void createFilesUnder(Path projectRoot) throws IOException {
		Path metaInf = projectRoot.resolve("src/main/resources/META-INF");
		Files.createDirectories(metaInf);
		Path xml = metaInf.resolve("persistence.xml");
		Path txt = metaInf.resolve("persistence.txt");
		if (!Files.exists(xml)) { 
			Path p = Files.createFile(xml);
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(p.toFile()))) {
				writer.write("<xml>\n");
				writer.write(" <persistence-unit name=\"myPU\" transaction-type=\"RESOURCE_LOCAL\">\n");
				writer.write("  <jar-file>../path/to/file/myjar-1.0-SNAPSHOT.jar</jar-file>\n");
				writer.write(" </persistence-unit>\n");
				writer.write("</xml>\n");
			}
		}
		if (!Files.exists(txt)) Files.createFile(txt);

	}
}
