# xray-junit-extensions

[![build workflow](https://github.com/Xray-App/xray-junit-extensions/actions/workflows/CI.yml/badge.svg)](https://github.com/Xray-App/xray-junit-extensions/actions/workflows/CI.yml)
[![Known Vulnerabilities](https://snyk.io/test/github/Xray-App/xray-junit-extensions/badge.svg)](https://snyk.io/test/github/Xray-App/xray-junit-extensions)
![code coverage](
https://raw.githubusercontent.com/Xray-App/xray-junit-extensions/main/.github/badges/jacoco.svg)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/686a52b4d223469d894af90071ee93d2)](https://www.codacy.com/gh/Xray-App/xray-junit-extensions/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Xray-App/xray-junit-extensions&amp;utm_campaign=Badge_Grade)
[![license](https://img.shields.io/badge/License-EPL%202.0-green.svg)](https://opensource.org/licenses/EPL-2.0)
[![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/Xray-App/community)

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

Configure the maven repository in your pom.xml:

```xml
        <repository>
            <id>github</id>
            <url>https://maven.pkg.github.com/Xray-App/*</url>
            <snapshots>
              <enabled>true</enabled>
            </snapshots>
        </repository>
```

Add the following dependency to your pom.xml:

```xml
        <dependency>
          <groupId>com.idera.xray</groupId>
          <artifactId>xray-junit-extensions</artifactId>
          <version>0.4.0</version>
          <scope>test</scope>
        </dependency>
```

In your `.m2/settings.xml`, configure the authentication for the maven repository.

```xml
 <servers>
    <server>
      <id>github</id>
      <username>GITHUB_USERNAME</username>
      <password>GITHUB_PERSONAL_TOKEN</password>
    </server>
    ...
</servers>
```

## How to use

In order to generate the enhanced, customized JUnit XML report we need to register the **EnhancedLegacyXmlReportGeneratingListener** listener. This can be done in [several ways](https://junit.org/junit5/docs/current/user-guide/#launcher-api-listeners-custom):

- discovered automatically at runtime based on the contents of a file (e.g `src/test/META-INF/services/org.junit.platform.launcher.TestExecutionListener`) 

```
com.idera.xray.junit.customjunitxml.EnhancedLegacyXmlReportGeneratingListener
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

## Other features and limitations

### Name of Tests

The summary of the Test issue will be set based on these rules (the first that applies):

* based on the summary attribute of @XrayTest annotation (e.g. `@XrayTest(summary = "xxx")`); 
* based on the `@DisplayName` annotation, or the display name of dynamically created tests from a TestFactory;
* based on the test's method name.

### Parameterized and repeated tests

For the time being, and similar to what happened with legacy JUnit XML reports produces with JUnit 4, parameterized tests (i.e. annotated with `@ParameterizedTest`) will be mapped to similar `<testcase>` elements in the JUnit XML report.
The same happens with repeated tests (i.e annotated with `@RepeatedTest`).

## Background

With JUnit 4 and surefire, legacy JUnit XML reports can be produced. These are used by many tools, including CI, and test management tools (e.g. Xray).
However, the format produced by JUnit 4 is limited and cannot be extended.

Junit 5 has a more flexible architecture. Even though JUnit XML format as not evolved meanwhile, it's possible to use Extensions and Test Execution Listeners to implement our own, tailored custom reporter.

JUnit 5 provides a [legacy XML reporter for the jupiter engine](https://junit.org/junit5/docs/current/api/org.junit.platform.reporting/org/junit/platform/reporting/legacy/xml/LegacyXmlReportGeneratingListener.html) as a listener, which was used as basis for this implementation.


## TO DOs

- cleanup code
- add more tests

## FAQ

1. Can this be used with JUnit 4?
No. If you're using JUnit 4 you can still generate a "standard" JUnit XML report but you'll miss the capabilities provided by this project. It's recommended to use JUnit 5 as JUnit 4 is an older project and much more limited.

2. Is this format compatible with Jenkins and other tools?
Probably. As there is no official JUnit XML schema, it's hard to say that in advance. However, the new information being embed on the custom JUnit XML report is done in such a way that shouldn't break other tools.

3. Why use a custom JUnit XML report instead of Xray JSON report?
For several reasons. One of them is that it was easier to take advantage of the existing code and adapt it.


## Contact

You may find me on [Twitter](https://twitter.com/darktelecom).
Any questions related with this code, please raise issues in this GitHub project. Feel free to contribute and submit PR's.
For Xray specific questions, please contact [Xray's support team](https://jira.getxray.app/servicedesk/customer/portal/2).

## References

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [How Xray processes JUnit XML reports](https://docs.getxray.app/display/XRAYCLOUD/Taking+advantage+of+JUnit+XML+reports)
- [JUnit 5 legacy XML reporter](https://junit.org/junit5/docs/current/api/org.junit.platform.reporting/org/junit/platform/reporting/legacy/xml/LegacyXmlReportGeneratingListener.html)


## LICENSE

[Eclipse Public License - v 2.0](LICENSE)
