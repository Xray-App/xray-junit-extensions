# xray-junit-extensions

[![build workflow](https://github.com/Xray-App/xray-junit-extensions/actions/workflows/CI.yml/badge.svg)](https://github.com/Xray-App/xray-junit-extensions/actions/workflows/CI.yml)
[![Known Vulnerabilities](https://snyk.io/test/github/Xray-App/xray-junit-extensions/badge.svg)](https://snyk.io/test/github/Xray-App/xray-junit-extensions)
![code coverage](
https://raw.githubusercontent.com/Xray-App/xray-junit-extensions/main/.github/badges/jacoco.svg)
[![license](https://img.shields.io/badge/License-EPL%202.0-green.svg)](https://opensource.org/licenses/EPL-2.0)
[![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/Xray-App/community)
[![Maven Central Version](https://img.shields.io/maven-central/v/app.getxray/xray-junit-extensions)](https://central.sonatype.com/artifact/app.getxray/xray-junit-extensions/)

This repo contains several improvements for [JUnit](https://junit.org/junit5/) that allow you to take better advantage of JUnit 5 (jupiter engine) whenever using it together with [Xray Test Management](https://getxray.app).
This code is provided as-is; you're free to use it and modify it at your will (see license ahead).

This is a preliminary release so it is subject to changes, at any time.

## Overview

Results from automated scripts implemented as `@Test` methods can be tracked in test management tools to provide insights about quality aspects targeted by those scripts and their impacts.
Therefore, it's important to attach some relevant information during the execution of the tests, so it can be shared and analyzed later on in the test management tool (e.g. [Xray Test Management](https://getxray.app)).

This project is highly based on previous work by the JUnit team. The idea is to be able to produce a custom JUnit XML report containing additional information that Xray can take advantage of.
This way, testers can automate the test script and at the same time provide information such as the covered requirement, right from the test automation code. Additional information may be provided, either through new annotations or by injecting a custom reporter as argument to the test method, using a specific extension.

### Features

- track started and finished date timestamps for each test
- link a test method to an existing Test issue or use auto-provisioning
- cover a "requirement" (i.e. an issue in Jira) from a test method
- specify additional fields for the auto-provisioned Test issues (e.g. summary, description, labels)
- attach screenshots or any other file as evidence to the Test Run, right from within the test method
- add comments ot the Test Run, right from within the test method
- set the values for Test Run custom fields, right from within the test method

### Main classes

The project consists of:

- **EnhancedLegacyXmlReportGeneratingListener**: a custom TestExecutionListener implementation that is responsible for generating a custom JUnit XML with additional properties Xray can take advantage of
- **@XrayTest**, **@Requirement**: new, optional annotations to provide additional information whenever writing the automated test methods
- **XrayTestReporterParameterResolver**: a new, optional JUnit 5 extension that provides the means to report additional information to Xray, inside the test method flow

## Installing

These extensions are available as an artifact available on (Maven) Central Repository, which is configured by default in your Maven instalation.

Add the following dependency to your pom.xml:

```xml
        <dependency>
          <groupId>app.getxray</groupId>
          <artifactId>xray-junit-extensions</artifactId>
          <version>0.10.0</version>
          <scope>test</scope>
        </dependency>
```

### Configuration

If you want, you may configure certain aspects of this extension. The defaults should be fine, otherwise please create a properties file as `src/test/resources/xray-junit-extensions.properties`, and define some settings.

- `report_filename`: the name of the report, without ending .xml suffix. Default is "TEST-junit-jupiter.xml"
- `report_directory`: the directory where to generate the report, in relative or absolute format. Default is "target"
- `add_timestamp_to_report_filename`: add a timestamp based suffix to the report. Default is "false".
- `report_only_annotated_tests`: only include tests annotated with @XrayTest or @Requirement. Default is "false".
- `reports_per_class`: generate JUnit XML reports per test class instead of a single report with all results; if true, `report_filename`, and `add_timestamp_to_report_filename` are ignored. Default is "false".
- `test_metadata_reader`: override the default logic responsible for reading meta-information about test methods.

Example:

```bash
report_filename=custom-report-junit
report_directory=reports
add_timestamp_to_report_filename=true
report_only_annotated_tests=false
reports_per_class=false
test_metadata_reader=com.example.CustomTestMetadataReader
```

## How to use

In order to generate the enhanced, customized JUnit XML report we need to register the **EnhancedLegacyXmlReportGeneratingListener** listener. This can be done in [several ways](https://junit.org/junit5/docs/current/user-guide/#launcher-api-listeners-custom):

- discovered automatically at runtime based on the contents of a file (e.g `src/test/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener`) 

```
app.getxray.xray.junit.customjunitxml.EnhancedLegacyXmlReportGeneratingListener
```

- programmaticaly

```java
LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder
                                                .request().selectors(...).filters(...).build();
Launcher launcher = LauncherFactory.create();
launcher.discover(request)
EnhancedLegacyXmlReportGeneratingListener listener = new EnhancedLegacyXmlReportGeneratingListener( Paths.get("/tmp/reports/"),new PrintWriter(System.out));
launcher.registerTestExecutionListeners(listener);
launcher.execute(request);
```

Registering the listener is mandatory.
In order to take advantage of the capabilities of this new listener, new annotations can be used. There's also a custom ParameterResolver **XrayTestReporterParameterResolver**, which allows users to inject a **XrayTestReporter** object as argument on the test methods. This will be useful when it's needed to report/attach some additional information during the test lifecycle.


### New annotations

Two new annotations (`@XrayTest`, `@Requirement`) can be used.
The annotations are optional and cannot be use more than once per test method.

#### @XrayTest

Test methods don't need to be annotated with `@XrayTest` unless you want to take advantage of the following enhancements.

You may use the **@XrayTest** annotation to:

- enforce mapping of result to specific, existing Test identified by issue key, using the **key** attribute
- enforce mapping of result to specific, existing Test issue identified by the Generic Test Definition, using the **id** attribute
- enforce the summary on the corresponding Test issue, during auto-provisioning, using the **summary** attribute
- enforce the description on the corresponding Test issue, during auto-provisioning, using the **description** attribute

_Examples:_

1. enforce the given test and corresponding result to be reported against the existing Test issue having the key  CALC-1000 (i.e. create a Test Run in Xray for the Test with issue key CALC-1000)  

```java
    @Test
    @XrayTest(key = "CALC-1000")
    public void CanAddNumbers()
```

2. enforce the "Summary" and the "Description" fields on the corresponding Test issue. The test will be auto-provisioned in Xray, if it doesn't exist yet, based on the class name and method name implementing the test  

```java
    @Test
    @XrayTest(summary = "addition of two numbers", description = "tests the sum of two positive integers")
    public void CanAddNumbers()
```

#### @Requirement

You may use the **Requirement** annotation, before your test methods in order to identify the covered requirement(s).
It's possible to identify one covered issue (e.g. requirement) or more, even though it's a good practice to cover just one.

_Examples:_


1. use this test to cover a requirement/user story identified by the issue key CALC-1234 (i.e create a issue link "tests" between the Test issue and the identified requirement);  

```java
    @Test
    @Requirement("CALC-1234")
    public void CanAddNumbers()
```

2. use this test to cover two requirements/user stories identified by the issue keys CALC-1234, CALC-1235 (i.e create a issue link "tests" between the Test issue and the identified requirements).

```java
    @Test
    @Requirement({"CALC-1234", "CALC-1235"})
    public void CanAddNumbers()
```


### New Extension

A new JUnit 5 compatible Extension **XrayTestReporterParameterResolver** can be used, so we can inject a **XrayTestReporter** object as argument in the test methods.

It allows to:

- add one or more comments to the Test Run; if used multiple times, comments are appended;
- define the value of some Test Run's custom field;
- attach some file(s) as global evidence on the Test Run.

_Examples:_

1. add overall comments to the results (i.e. the Test Run), set the value of some Test Run custom field, and attach one (or more) evidence.

```java
@ExtendWith(XrayTestReporterParameterResolver.class)
public class XrayEnabledTestExamples {

...
    @Test
    public void someTest(XrayTestReporter xrayReporter) {
        xrayReporter.addComment("hello");
        xrayReporter.setTestRunCustomField("myTRcustomfield", "field1_value");
        // you can also set a multiple-value based TR custom field
        String [] someArr = {"Porto", "Lisbon"};
        xrayReporter.setTestRunCustomField("multiplevalueTRcustomfield", someArr);
        xrayReporter.addTestRunEvidence("/tmp/screenshot.png");
        xrayReporter.addComment("world");
    }
```

### Customizing how test metadata is read

When generating the report, it's allowed to customize the way the test method information is read.
By default, test information such as id, key, summary, description and requirements are read directly from @XrayTest and @Requirements annotations.
This behavior can be overridden by the user when he wants to change the way these meta-information are generated, or when he wants to use their own annotations to describe the tests.

To do this, you need to create a public class with a no-argument constructor that implements the `app.getxray.xray.junit.customjunitxml.XrayTestMetadataReader` interface (or extend `app.getxray.xray.junit.customjunitxml.DefaultXrayTestMetadataReader` class).
Then must add `test_metadata_reader` entry with the class name to the `xray-junit-extensions.properties` file.


#### Example: Custom test metadata reader to read Jira key from custom @JiraKey annotation

_JiraKey.java_
```java
package com.example;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface JiraKey {
    String value();
}
```

_CustomTestMetadataReader.java_
```java
package com.example;

import app.getxray.xray.junit.customjunitxml.DefaultXrayTestMetadataReader;
import org.junit.platform.launcher.TestIdentifier;

import java.util.Optional;

public class CustomTestMetadataReader extends DefaultXrayTestMetadataReader {

    @Override
    public Optional<String> getKey(TestIdentifier testIdentifier) {
        return getTestMethodAnnotation(testIdentifier, JiraKey.class)
                .map(JiraKey::value)
                .filter(s -> !s.isEmpty());
    }
}
```

_xray-junit-extensions.properties_
```
test_metadata_reader=com.example.CustomTestMetadataReader
```

_SimpleTest.java_
```java
package com.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimpleTest {

    @Test
    @JiraKey("CALC-123")
    @DisplayName("simple test")
    void simpleTest() {
        // ...
    }
}
```

## Other features and limitations

### Name of Tests

The summary of the Test issue will be set based on these rules (the first that applies):

* based on the summary attribute of @XrayTest annotation (e.g. `@XrayTest(summary = "xxx")`); 
* based on the `@DisplayName` annotation, or the display name of dynamically created tests from a TestFactory;
* based on the test's method name.

> [!TIP]
> This behavior can be changed by defining a custom test metadata reader.

### Parameterized and repeated tests

For the time being, and similar to what happened with legacy JUnit XML reports produces with JUnit 4, parameterized tests (i.e. annotated with `@ParameterizedTest`) will be mapped to similar `<testcase>` elements in the JUnit XML report.
The same happens with repeated tests (i.e annotated with `@RepeatedTest`).

## Background

With JUnit 4 and surefire, legacy JUnit XML reports can be produced. These are used by many tools, including CI, and test management tools (e.g. Xray).
However, the format produced by JUnit 4 is limited and cannot be extended.

Junit 5 has a more flexible architecture. Even though JUnit XML format as not evolved meanwhile, it's possible to use Extensions and Test Execution Listeners to implement our own, tailored custom reporter.

JUnit 5 provides a [legacy XML reporter for the jupiter engine](https://junit.org/junit5/docs/current/api/org.junit.platform.reporting/org/junit/platform/reporting/legacy/xml/LegacyXmlReportGeneratingListener.html) as a listener, which was used as basis for this implementation.


## TO DOs

...

## FAQ

1. Can this be used with JUnit 4?
No. If you're using JUnit 4 you can still generate a "standard" JUnit XML report but you'll miss the capabilities provided by this project. It's recommended to use JUnit 5 as JUnit 4 is an older project and much more limited.

2. Is this format compatible with Jenkins and other tools?
Probably. As there is no official JUnit XML schema, it's hard to say that in advance. However, the new information being embed on the custom JUnit XML report is done in such a way that shouldn't break other tools.

3. Why use a custom JUnit XML report instead of Xray JSON report?
For several reasons. One of them is that it was easier to take advantage of the existing code and adapt it.

4. Then enhanced JUnit XML report gets overwritten whenever I have different test classes...
Make sure you're using a recent version of `maven-surefire-plugin`, like 3.2.5; that will fix it!  If you're using a old version (e.g., 2.22.2), surefire will generate one JUnit testplan per class, and the reporter will be invoked multiple times, overwriting the previous report. For more info see [issue #47](https://github.com/Xray-App/xray-junit-extensions/issues/47)  and [this discussion](https://github.com/Xray-App/xray-junit-extensions/discussions/48).

5. I don't see the enhanced JUnit XML report or I see a report but without the Xray specific information.
You're probably using the legacy JUnit report and not the one generated by this plugin. Make sure you're using the `EnhancedLegacyXmlReportGeneratingListener`; see how to enable it, above in the "How to use" section.

## Contact

You may find me on [Twitter](https://twitter.com/darktelecom).
Any questions related with this code, please raise issues in this GitHub project. Feel free to contribute and submit PR's.
For Xray specific questions, please contact [Xray's support team](https://jira.getxray.app/servicedesk/customer/portal/2).

## References

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [How Xray processes JUnit XML reports](https://docs.getxray.app/display/XRAYCLOUD/Taking+advantage+of+JUnit+XML+reports)
- [JUnit 5 legacy XML reporter](https://junit.org/junit5/docs/current/api/org.junit.platform.reporting/org/junit/platform/reporting/legacy/xml/LegacyXmlReportGeneratingListener.html)

## LICENSE

Based on code from [JUnit5](https://github.com/junit-team/junit5/) project.

[Eclipse Public License - v 2.0](LICENSE)
