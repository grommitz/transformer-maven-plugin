# transformer-maven-plugin

To aid updating the persistence.xml files automatically during a release I have written a maven plugin. 

## Using the transformer plugin

Standalone usage:

	mvn com.martincharlesworth:transformer-maven-plugin:persistencexml

or add this to the pom:

	<plugin>
		<artifactId>transformer-maven-plugin</artifactId>
		<groupId>com.martincharlesworth</groupId>
		<version>1.0-SNAPSHOT</version>
	</plugin>

then you can use the shortened version of the command line:

	mvn transformer:persistencexml



