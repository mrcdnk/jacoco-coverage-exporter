# jacoco-coverage-exporter
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/mrcdnk/jacoco-coverage-exporter/blob/master/LICENSE)
[![Release version](https://img.shields.io/github/v/release/mrcdnk/jacoco-coverage-exporter)](https://github.com/mrcdnk/jacoco-coverage-exporter/releases)
[![Docker pulls](https://img.shields.io/docker/pulls/mrcdnk/jacoco-coverage-exporter)](https://hub.docker.com/r/mrcdnk/jacoco-coverage-exporter)

Prometheus Exporter for fetching runtime jacoco coverage data provided through jmx.


Why do I need this? In case you want to know how much of your code base is being tested by integrative tests! 

It is easy to fetch the coverage when executing unit tests running on the same machine as the tests, but if you have tests running from outside this usually becomes a bit more difficult.
This project aims to provide an easy setup for monitoring test coverage generated by integrative tests.


**Please be aware that all releases before 1.0.0 might contain breaking changes in order to finish the initial MVP.**

# Prometheus Spring Auto Config

The easiest way to add jacoco code coverage to your prometheus endpoint is to add the jacoco-coverage-exporter-spring maven dependency to your classpath.
This will by default try to load the class files from `/app/classes/` which is the default of JIB. To change this you can set the following property:

```Yaml
coverage:
  classesLocations: "<first Path to class files>,<second path to class files>"
```
In most cases you will add this property in the `JAVA_TOOL_OPTIONS` or `_JAVA_OPTIONS` as `-Dcoverage.classesLocations=<path1>;<path2>` if you only want to modify the deployment.

## Adding dependencies to the coverage data
JIB usually only puts the dependencies of the module building the docker file into `/app/classes/` which might not be all classes you want to collect coverage for.
For example maven multi-module projects most of the time will need to add the jars of their other modules to this list:

```Yaml
coverage:
  enableClassesCache: true
  classesLocations: "/app/classes/"
  includePatterns: "glob:**.jar,glob:**.class"
  excludePatterns: "<patterns to exclude>"
```

`coverage.classesLocations` is a list of directories containing classes or JAR files to be scanned for coverage data. Can also be jar files directly. This exists to avoid scanning the entire file system.

`coverage.includePatterns` is a list of Java PathMatcher expressions matching all files that should be included.

`coverage.excludePatterns` is a list of Java PathMatcher expressions matching all files that will be excluded from the coverage data.

`coverage.enableClassCache` if true the classesLocations will only be scanned the first time, afterwards the previous results will be reused.

All files inside the classesLocations will be collected. If there are include patterns the original list will be filtered on file names matching the patterns. In case there are also excludePatterns the files matching those patterns will be removed from the list.

By default, only `/app/classes/` will be searched and the inclusion list only contains `glob:**.class,glob:**.jar`. There are no exclude patterns in the default settings.

## Adding the jacoco java agent

Additionally, you will need to add the `-javaagent:/path/to/jacocoagent.jar=jmx=true,output=none` option to generate coverage data.
Make sure the `jacoco-agent.jar` you are using is the one classified with `-runtime.jar` as a suffix! (e.g. [org.jacoco.agent-0.8.12-runtime.jar](https://repo1.maven.org/maven2/org/jacoco/org.jacoco.agent/0.8.12/org.jacoco.agent-0.8.12-runtime.jar))


## About Metrics
The metrics follow the same format as described in [Prometheus Metrics](#prometheus-metrics)

You can add labels to the metrics like this:

```Yaml
coverage:
  prometheus:
    labels:
      <tagKey1>: <tagValue1>
```

To reset the coverage data the actuator endpoint `jacoco` might need to be exposed and enabled depending on your settings:

```Yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - jacoco
  endpoint:
    jacoco:
      enabled: true
```
If the Actuator endpoint is available you can `POST` a reset command in the following format:

```Json
{
  "reset": "true"
}
```

to the path `/actuator/jacoco`.

# Export Server

## Features
* **REST Api for JaCoCo Coverage** — Manage Coverage data for a Set of configured application
* **Prometheus metrics for JaCoCo Coverage** — Monitor Jacoco Coverage for each application

### REST Api
* `POST /v1/reset` resets all coverage agents connected to the exporter
* `POST /v1/reset` with body `{"applications": ["app"]}` will only reset the specified list of applications
* `GET /v1/coverage` currently returns a simple overview page showing general coverage data for all applications, easy to consume data is currently only provided through the metrics endpoint.

### Prometheus Metrics

* `GET /actuator/prometheus` will also return prometheus metrics each top level coverage-type: `branches, instructions, methods, classes, lines, complexity`
  * `jacoco_<coverage-type>_covered{application="app"}`
  * `jacoco_<coverage-type>_missed{application="app"}`
  * `jacoco_<coverage-type>_total{application="app"}`
  * `jacoco_scrape_duration_seconds` duration for **any** coverage provider scrape, this currently is not labeled with the application being scraped
(there is no pre-made Grafana Dashboard for this yet)

### Liveliness and readiness probes
Liveliness and readiness endpoint is at `/actuator/health`.

## Getting Started

You can either build a jar yourself or use the container build through jib available on [hub.docker.com](https://hub.docker.com/r/mrcdnk/jacoco-coverage-exporter). 

### Helm Chart

The Helm Chart will be provided in the future through GitHub Pages and https://artifacthub.io/

### Providing Class Files

To be able to generate the coverage report the exporter requires access to the class files of each application.
If you are deploying this exporter in Kubernetes you can consider copying the class from the application image inside of init containers.

Because this process can might require a large amount of init containers please let me know how you would like to load your class files. 
I am planning to add different kinds of class file providers in the future.

### Configurations

Before you can fetch the coverage through this exporter you will need to add the jacoco agent to your testee. 
This can be achieved by adding the `jacocoagent.jar` (https://central.sonatype.com/artifact/org.jacoco/org.jacoco.agent) to the Pod of each application and adding the following `JAVA_TOOL_OPTIONS`

```Java
-javaagent:/path/to/jacocoagent.jar=jmx=true 
-Dcom.sun.management.jmxremote.port=5001 
-Dcom.sun.management.jmxremote.ssl=false 
-Dcom.sun.management.jmxremote.authenticate=false
```

There currently is no support for an authenticated jmx connection.

By default, the folder `/config` is part of the classpath, please mount/load the configuration into this folder.
The exporter is a standard spring boot application, which means you should also be able to let it fetch its configuration from a spring cloud config server.

```YAML
coverage: 
    prometheus:
        labels:
            system: "some-system"   # adds a set of custom labels to all jacoco metrics
    collect:
        providers:                  # A list of all jmx application coverage providers
            - name: my-application
              host: my-application-host
              port: 5001            # jmx port of your application
              sourcesLocations:
                - "/path/to/my-application/classes"
```

### Usage

Before starting to measure test coverage for your integration tests it might be a good idea to call `POST /v1/reset` to remove all the coverage generated by the startup of the system.
This way you will only measure the coverage generated by your integration tests.

Now you can run your test and either observe the rising test coverage inside Grafana or wait for the tests to finish and fetch the final results from the `/v1/coverage` endpoint.
