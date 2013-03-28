<section>

# Installation

## Update Site
This update site contains latest version of emfjson and jackson 1.8.5.

[http://ghillairet.github.com/p2](http://ghillairet.github.com/p2)

## Maven repository
A maven repository for emfjson is available, note that you should also include the denpendency and repository for jackson in
your pom file. Please refer to the [jackson](http://jackson.codehaus.org/) documentation for more information.

```xml
<repository>
	<id>emfjson-repository</id>
	<url>http://repository-ghillairet.forge.cloudbees.com/snapshot</url>
	<snapshots>
		<enabled>true</enabled>
		<updatePolicy>always</updatePolicy>
	</snapshots>
</repository>

<dependency>
	<groupId>org.eclipselabs</groupId>
	<artifactId>org.eclipselabs.emfjson</artifactId>
	<version>0.5.3-SNAPSHOT</version>
</dependency>
```

## From Source
You can install emfjson from source if you have Maven 3.0 by executing the following commands.
This will generate a p2 repository in the build/repository/target folder.

```
git clone git://github.com/ghillairet/emfjson.git
cd emfjson
mvn clean install
```

</section>
