package in.project.main;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import in.project.main.services.EnrollmentServiceTest;
import java.io.PrintWriter;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("Starting EnrollmentServiceTest execution...");
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClass(EnrollmentServiceTest.class))
            .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));
        System.out.println("Tests found: " + summary.getTestsFoundCount());
        System.out.println("Tests succeeded: " + summary.getTestsSucceededCount());
        System.out.println("Tests failed: " + summary.getTestsFailedCount());
        if (summary.getTotalFailureCount() > 0) {
            for (TestExecutionSummary.Failure failure : summary.getFailures()) {
                System.err.println("=== FAILURE in " + failure.getTestIdentifier().getDisplayName() + " ===");
                Throwable t = failure.getException();
                while (t != null) {
                    System.err.println("Cause: " + t.getClass().getName() + " -> " + t.getMessage());
                    t = t.getCause();
                }
                failure.getException().printStackTrace();
                break; // print just the first failure's root cause
            }
            System.exit(1);
        }
    }
}
