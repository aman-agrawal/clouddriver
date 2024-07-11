Spinnaker Cloud Provider Service
------------------------------------
[![Build Status](https://github.com/spinnaker/clouddriver/workflows/Branch%20Build/badge.svg)](https://github.com/spinnaker/clouddriver/actions)

This service is the main integration point for Spinnaker cloud providers like AWS, GCE, CloudFoundry, Azure etc.

### Developing with Intellij

To configure this repo as an Intellij project, run `./gradlew idea` in the root directory.

Some of the modules make use of [Lombok](https://projectlombok.org/), which will compile correctly on its own. However, for Intellij to make sense of the Lombok annotations, you'll need to install the [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok-plugin) as well as [check 'enable' under annotation processing](https://www.jetbrains.com/help/idea/configuring-annotation-processing.html#3).

### Debugging

To start the JVM in debug mode, set the Java system property `DEBUG=true`:
```
./gradlew -DDEBUG=true
```

The JVM will then listen for a debugger to be attached on port 7102.  The JVM will _not_ wait for
the debugger to be attached before starting Clouddriver; the relevant JVM arguments can be seen and
modified as needed in `build.gradle`.

$${\color{lightblue} Recent \space commits:}$$ 

              CommitID                   |   Author      | Commit Message          | Commit Date
----------------------------------------------------------------------------------------------------



e458cd21087f54b0a9ece3dbe967edfafe98d001 | Yugandharkumar | Create commits-preserve.yml | 2023-08-09 


986aceb68240b2fb93bd923d78235133a619e1fb | Yugandharkumar | adding tzdata-java | 2023-08-25 


a81f01042740e1d03b403c8f66b274fcd17370df | Yugandharkumar | Rename Dockerfile-sleep to Dockerfile | 2024-05-20 
