# JavaFx TCK Tools Rewrite App

This module contains a JavaFx Quarkus app that allows one to browse an EE10 TCK distribution source tree and convert the test classes from the legacy JavaTest framework into Arquillian/Junit5 tests.

## Building the app dependencies
Right now there are several dependencies that are not available in Maven Central.  These dependencies are built from source and installed into the local Maven repository.  The following dependencies are built from source:
* platform.tck: The TCK platform repo platform-tck main branch needs to be built to provide the test classes for the current TCK, 11.0.0-SNAPSHOT.
* jakartaee-tck-tools/tools/tck-rewrite-ant module needs to be built to provide the Ant parser for the TCK source tree.

### Building the platform-tck
```bash
git clone https://github.com/jakartaee/platform-tck/
cd platform-tck
mvn install
```

### Building jakartaee-tck-tools/tools/tck-rewrite-ant
```bash
git clone https://github.com/eclipse-ee4j/jakartaee-tck-tools/
cd jakartaee-tck-tools/tools/tck-rewrite-ant
mvn install
```

## Building the application
The application is in the same jakartaee-tck-tools repository you built the tck-rewrite-ant module from.  The application is in the tools/tck-rewrite-fx module, and it needs Java SE 21+ to build and run.

To build the application, run the following commands:

```bash
cd jakartaee-tck-tools/tools/tck-rewrite-fx
mvn package
```

## Running the application
```bash
mvn quarkus:run
```

There are two environment variables you will want to set to simplify usage of the app:
* TS_HOME: The root directory of the EE10 TCK you have downloaded.
* TESTS_REPO: The location of the current platform-tck repo.

You can set or change these at runtime using the application file menu, but it is easier to set them in the shell before starting the app.

