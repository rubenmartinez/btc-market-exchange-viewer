package net.rubenmartinez.stpc.test.util;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copied from https://junit.org/junit5/docs/current/user-guide/#extensions-lifecycle-callbacks-timing-extension
 * Including just a log trace when test starts recursively using all parent contexts
 */
public class TestLoggingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TestLoggingExtension.class);
	
    private static final String START_TIME = "start time";

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        LOGGER.info("===> About to execute method [{}] ({})", getTestMethodName(context), getRecursiveDisplayName(context));
        getStore(context).put(START_TIME, System.currentTimeMillis());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        long startTime = getStore(context).remove(START_TIME, long.class);
        long duration = System.currentTimeMillis() - startTime;

        LOGGER.info("<=== Finished executing [{}] in {} ms, ({})", getTestMethodName(context), duration, getRecursiveDisplayName(context));
    }

    private static Store getStore(ExtensionContext context) {
        return context.getStore(Namespace.create(TestLoggingExtension.class, context.getRequiredTestMethod()));
    }
    
    private static String getRecursiveDisplayName(ExtensionContext context) {
    	// StringBuilder wouldn't be too good here anyway as we want to prepend
    	// The root displayName is not very interesting, as it is just the runner name
    	
    	return context.getParent().isPresent() ?
    			getRecursiveDisplayName(context.getParent().get()) + "/" + context.getDisplayName()
    			: "";
    }

    private static String getTestMethodName(ExtensionContext context) {
        Method testMethod = context.getRequiredTestMethod();
        return testMethod.getName();
    }
}