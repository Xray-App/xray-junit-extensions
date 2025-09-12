package app.getxray.xray.junit.customjunitxml;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;


/**
 * A simple JUnit Platform {@link TestExecutionListener} implementation
 * that captures the {@link TestPlan} provided at the start of execution.
 *
 * The JUnit Platform calls {@link #testPlanExecutionStarted(TestPlan)} once,
 * right before any tests are executed, passing in the complete structure
 * of discovered tests (containers and test cases).
 *
 * This listener stores that {@code TestPlan} so it can be retrieved later
 * via {@link #getPlan()} and used in assertions, for example to look up
 * {@link org.junit.platform.launcher.TestIdentifier} instances when testing
 * metadata extraction logic.
 */
class CapturingTestExecutionListener implements TestExecutionListener {
    private TestPlan plan;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        this.plan = testPlan;
    }

    public TestPlan getPlan() {
        return plan;
    }
}
