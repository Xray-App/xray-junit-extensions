package app.getxray.xray.junit.xray_json;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

public class XrayJsonReporter implements TestExecutionListener {

    private final BiConsumer<Throwable, Supplier<String>> logger;


	/**
	 * Create a {@code LoggingListener} which delegates to a
	 * {@link java.util.logging.Logger} using a log level of
	 * {@link Level#FINE FINE}.
	 *
	 * @see #forJavaUtilLogging(Level)
	 * @see #forBiConsumer(BiConsumer)
	 */
	public static XrayJsonReporter forJavaUtilLogging() {
		return forJavaUtilLogging(Level.WARNING);
	}

	/**
	 * Create a {@code XrayJsonReporter} which delegates to a
	 * {@link java.util.logging.Logger} using the supplied
	 * {@linkplain Level log level}.
	 *
	 * @param logLevel the log level to use; never {@code null}
	 * @see #forJavaUtilLogging()
	 * @see #forBiConsumer(BiConsumer)
	 */
	public static XrayJsonReporter forJavaUtilLogging(Level logLevel) {
		Preconditions.notNull(logLevel, "logLevel must not be null");
		Logger logger = Logger.getLogger(XrayJsonReporter.class.getName());
		return new XrayJsonReporter((t, messageSupplier) -> logger.log(logLevel, t, messageSupplier));
	}

	/**
	 * Create a {@code LoggingListener} which delegates to the supplied
	 * {@link BiConsumer} for consumption of logging messages.
	 *
	 * <p>The {@code BiConsumer's} arguments are a {@link Throwable} (potentially
	 * {@code null}) and a {@link Supplier} (never {@code null}) for the log
	 * message.
	 *
	 * @param logger a logger implemented as a {@code BiConsumer};
	 * never {@code null}
	 *
	 * @see #forJavaUtilLogging()
	 * @see #forJavaUtilLogging(Level)
	 */
	public static XrayJsonReporter forBiConsumer(BiConsumer<Throwable, Supplier<String>> logger) {
		return new XrayJsonReporter(logger);
	}


	private XrayJsonReporter(BiConsumer<Throwable, Supplier<String>> logger) {
		this.logger = Preconditions.notNull(logger, "logger must not be null");
	}


    public XrayJsonReporter() {
		this((t, messageSupplier) -> Logger.getLogger(XrayJsonReporter.class.getName()).log(Level.FINE, t, messageSupplier));
    }


    @Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		log("TestPlan Execution Started: %s", testPlan);
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		log("TestPlan Execution Finished: %s", testPlan);
	}

	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		log("Dynamic Test Registered: %s - %s - %s - %s", testIdentifier.getDisplayName(), testIdentifier.getUniqueId(), testIdentifier.getLegacyReportingName(), testIdentifier.getParentId());
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		log("Execution Started: %s - %s", testIdentifier.getDisplayName(), testIdentifier.getUniqueId());
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		log("Execution Skipped: %s - %s - %s", testIdentifier.getDisplayName(), testIdentifier.getUniqueId(), reason);
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		logWithThrowable("Execution Finished: %s - %s - %s", testExecutionResult.getThrowable().orElse(null),
			testIdentifier.getDisplayName(), testIdentifier.getUniqueId(), testExecutionResult);
	}

	private void log(String message, Object... args) {
		logWithThrowable(message, null, args);
	}

	private void logWithThrowable(String message, Throwable t, Object... args) {
		System.out.println(String.format(message, args));
		this.logger.accept(t, () -> String.format(message, args));
	}
    
}
