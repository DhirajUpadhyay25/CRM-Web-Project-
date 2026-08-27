package in.project.main.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import in.project.main.EducationApplication;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest(classes = EducationApplication.class)
@AutoConfigureMockMvc
public class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testAdminDashboardRendersSuccessfully() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("admin@gmail.com").roles("ADMIN")))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/dashboard"));
    }
}
