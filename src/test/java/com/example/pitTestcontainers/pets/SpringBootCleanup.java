package com.example.pitTestcontainers.pets;

import java.io.Closeable;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Stefan Pennndorf
 */
public class SpringBootCleanup implements Extension, AfterAllCallback {

    private static final Namespace SPRING_NAMESPACE = Namespace.create(SpringExtension.class);
    private static final Namespace CLEANUP_NAMESPACE = Namespace.create(SpringBootCleanup.class);

    @Override
    public void afterAll(final ExtensionContext context) {
        final Store store = context.getRoot().getStore(SPRING_NAMESPACE);
        Class<?> testClass = context.getRequiredTestClass();
        final TestContextManager testContextManager = store.get(testClass, TestContextManager.class);

        final Store cleanupStore = context.getRoot().getStore(CLEANUP_NAMESPACE);
        cleanupStore.getOrComputeIfAbsent(testClass, k -> closingTCM(testContextManager));
    }

    private Store.CloseableResource closingTCM(final TestContextManager testContextManager) {
        final TestContext testContext = testContextManager.getTestContext();
        return () -> {
            final ApplicationContext applicationContext = testContext.getApplicationContext();

            testContext.markApplicationContextDirty(DirtiesContext.HierarchyMode.EXHAUSTIVE);
            if (applicationContext instanceof Closeable) {
                ((Closeable) applicationContext).close();
            }
        };
    }
}
