package in.project.main.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Publishes a single flag, devToolsEnabled, to every view.
 *
 * DataSeederController and DataMigrationController are annotated @Profile("dev"), so their
 * routes simply do not exist outside the dev profile. The admin templates still rendered the
 * buttons that target those routes, which meant a production build showed two live-looking
 * controls that could only ever produce a 404.
 *
 * This advice is itself @Profile("dev"), so outside dev the bean is absent, the attribute is
 * never added, and th:if="${devToolsEnabled}" evaluates false - the buttons disappear along
 * with the endpoints they call. Hiding them is presentation only; the endpoints are gated by
 * the profile and by /admin/** in SecurityConfig, not by the absence of a button.
 */
@ControllerAdvice
@Profile("dev")
public class DevToolsModelAdvice {

    @ModelAttribute("devToolsEnabled")
    public boolean devToolsEnabled() {
        return true;
    }
}
