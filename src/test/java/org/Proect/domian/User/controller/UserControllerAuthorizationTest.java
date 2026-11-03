package org.Proect.domian.User.controller;

import org.Proect.config.SecurityConfig;
import org.Proect.domian.User.dto.UserResponse;
import org.Proect.domian.User.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void allowsAdminToCreateUser() throws Exception {
        when(userService.registerUser(any())).thenReturn(new UserResponse());

        mockMvc.perform(post("/api/v1/users")
                        .header("Origin", "http://127.0.0.1:3000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUserRequest()))
                .andExpect(status().isCreated());

        verify(userService).registerUser(any());
    }

    @Test
    @WithMockUser(roles = "SECURITY")
    void deniesSecurityRoleFromCreatingUser() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Origin", "http://127.0.0.1:3000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUserRequest()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    private static String validUserRequest() {
        return """
                {
                  "roleId":1,
                  "zoneId":1,
                  "firstName":"Test",
                  "lastName":"User",
                  "middleName":"",
                  "numberPhone":"+70000000000",
                  "email":"test@example.com",
                  "position":"Engineer",
                  "address":"Test address",
                  "birthday":"1990-01-02T00:00:00",
                  "stage":"2020-01-01T00:00:00",
                  "login":"testuser",
                  "password":"test-password-123"
                }
                """;
    }
}
