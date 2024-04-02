package app.getxray.xray.junit.xray_json;

public class XrayJsonReporterConfig  {
 

    /**
     * User id to assign as the reporter of the Test Execution issue. Defaults to the user implicitly
     * connected to the REST API authentication.
     */
    private String user = "";

    /**
     * Text to set as the description field on the Test Execution issue. Default is "test automation results".
     */
    private String summary = "test automation results";
    
    public static final String DEFAULT_TESTEXECUTION_SUMMARY = "test automation results";

    /**
     * Text to set as the description field on the Test Execution issue. Default is empty.
     */
    private String description = "";

    /**
     * Key of Jira projected where Test Execution, and eventually Test issues, will be created in. 
     */
    private String projectKey = "";

    /**
     * FixVersion (i.e. Jira project version) to assign to the Test Execution and implicitly its results. Optional.
     */
    private String version = "";

    /**
     * Revision (e.g., build number, code revision) to assign to the Test Execution and implicitly its results. Optional.
     */
    private String revision = "";

    /**
     * Test Execution to update/overwrite, if specified. If not specified a new Test Execution
     * will be created.
     */
    private String testExecutionKey = "";

    /**
     * Test Plan to link the Test Execution, and the corresponding results, to. Optional.
     */
    private String testPlanKey = "";

    /**
     * Test Environment to assign to the Test Execution and its results.
     * Usually, if present, has one value (e.g., browser vendor).
     * More than one value may be provided using comma as delimeter. Optional.
     */
    private String testEnvironments = "";

    /**
     * Indicates that "Manual" (i.e., step based tests) should be used during autoprovisioning
     * of data-driven tests. Defaults to true.
     */
    private boolean useManualTestsForDatadrivenTests = true;

    /**
     * Indicates that "Manual" (i.e., step based tests) should be used during autoprovisioning
     * of "regular, non data-driven, tests. If false, "Generic" (i.e., unstructured) tests will
     * be used. Defaults to false.
     */ 
    private boolean useManualTestsForRegularTests = false;


    /**
     * Indicates whether the Xray JSON format is for Xray Cloud or not. Defaults to true.
     */
    private boolean xrayCloud = true;

    /**
     * Filename of report file for storing the test results in Xray JSON format.
     * Defaults to "xray-report.json".
     */
    private String reportFilename = "xray-report.json";


    public void setXrayCloud(boolean xrayCloud) {
        this.xrayCloud = xrayCloud;
    }

    public boolean isXrayCloud() {
        return xrayCloud;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getTestExecutionKey() {
        return testExecutionKey;
    }

    public void setTestExecutionKey(String testExecutionKey) {
        this.testExecutionKey = testExecutionKey;
    }

    public String getTestPlanKey() {
        return testPlanKey;
    }

    public void setTestPlanKey(String testPlanKey) {
        this.testPlanKey = testPlanKey;
    }

    public String getTestEnvironments() {
        return testEnvironments;
    }

    public void setTestEnvironments(String testEnvironments) {
        this.testEnvironments = testEnvironments;
    }

    public String getReportFilename() {
        return reportFilename;
    }

    public void setReportFilename(String reportFilename) {
        this.reportFilename = reportFilename;
    }

    public void setUseManualTestsForDatadrivenTests(boolean useManualTestsForDatadrivenTests) {
        this.useManualTestsForDatadrivenTests = useManualTestsForDatadrivenTests;
    }

    public boolean isUseManualTestsForDatadrivenTests() {
        return useManualTestsForDatadrivenTests;
    }

    public void setUseManualTestsForRegularTests(boolean useManualTestsForRegularTests) {
        this.useManualTestsForRegularTests = useManualTestsForRegularTests;
    }

    public boolean isUseManualTestsForRegularTests() {
        return useManualTestsForRegularTests;
    }
}
