package in.project.main.api;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.project.main.services.AdminSearchService;

/**
 * REST controller for admin global search.
 * Returns categorized JSON results for the search overlay.
 */
@RestController
@RequestMapping("/admin/api")
public class AdminSearchApiController {

    @Autowired
    private AdminSearchService adminSearchService;

    @GetMapping("/search")
    public ResponseEntity<Map<String, List<Map<String, String>>>> search(
            @RequestParam(name = "q", defaultValue = "") String query) {
        Map<String, List<Map<String, String>>> results = adminSearchService.search(query);
        return ResponseEntity.ok(results);
    }
}
