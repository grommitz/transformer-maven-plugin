package com.grommitz.maven.transformer.plugin;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
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
	public void should_find_persistence_xmls() throws Exception {
		PersistenceXmlMojo mojo = new PersistenceXmlMojo()
				.startingDir(startingDir).fromVersion("1.0-SNAPSHOT").toVersion("2.0-SNAPSHOT");
		mojo.execute();
		assertThat(mojo.getPersistenceXmls().size(), is(2));
	}
	
	private void setupTempDirectoryStructure() throws IOException {
		Files.createDirectories(startingDir);
		createFilesUnder(startingDir);

		Path module = startingDir.resolve("module");
		Files.createDirectories(module);
		createFilesUnder(module);
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
