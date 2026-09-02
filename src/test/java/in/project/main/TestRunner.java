package in.project.main;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import in.project.main.services.NotificationServiceTest;

public class TestRunner {
    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(NotificationServiceTest.class))
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        TestExecutionSummary summary = listener.getSummary();
        System.out.println("==========================================");
        System.out.println("TEST EXECUTION RESULTS");
        System.out.println("==========================================");
        System.out.println("Tests found:     " + summary.getTestsFoundCount());
        System.out.println("Tests succeeded: " + summary.getTestsSucceededCount());
        System.out.println("Tests failed:    " + summary.getTestsFailedCount());
        if (!summary.getFailures().isEmpty()) {
            System.out.println("------------------------------------------");
            System.out.println("FAILURES:");
            summary.getFailures().forEach(f -> {
                System.out.println(" - " + f.getTestIdentifier().getDisplayName() + ": " + f.getException().getMessage());
            });
            System.out.println("------------------------------------------");
        }
    }
}
