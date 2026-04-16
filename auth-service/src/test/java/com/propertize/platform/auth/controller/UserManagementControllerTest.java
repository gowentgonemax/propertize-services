package com.propertize.platform.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertize.platform.auth.dto.CreateUserRequest;
import com.propertize.platform.auth.dto.UserInfoResponse;
import com.propertize.platform.auth.service.UserManagementService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagementController Tests")
class UserManagementControllerTest {

    @Mock
    private UserManagementService userManagementService;
    @InjectMocks
    private UserManagementController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserInfoResponse sampleUser() {
        return UserInfoResponse.builder()
                .id(1L)
                .username("jdoe")
                .email("jdoe@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .organizationId("10")
                .roles(Set.of("PROPERTY_MANAGER"))
                .enabled(true)
                .build();
    }

    // ── POST /users ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/users — createUser()")
    class CreateUser {

        @Test
        @DisplayName("Returns 201 with created user on success")
        void returns201OnSuccess() {
            CreateUserRequest req = new CreateUserRequest();
            req.setUsername("jdoe");
            req.setEmail("jdoe@example.com");
            req.setPassword("Pass1!");

            when(userManagementService.createUser(any())).thenReturn(sampleUser());

            ResponseEntity<UserInfoResponse> response = controller.createUser(req);

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(response.getBody().getUsername()).isEqualTo("jdoe");
        }

        @Test
        @DisplayName("Returns 409 when username/email already exists")
        void returns409OnConflict() {
            CreateUserRequest req = new CreateUserRequest();
            req.setUsername("existing");
            when(userManagementService.createUser(any()))
                    .thenThrow(new IllegalArgumentException("Username already exists"));

            ResponseEntity<UserInfoResponse> response = controller.createUser(req);

            assertThat(response.getStatusCode().value()).isEqualTo(409);
        }

        @Test
        @DisplayName("Returns 500 on unexpected service exception")
        void returns500OnException() {
            CreateUserRequest req = new CreateUserRequest();
            req.setUsername("user");
            when(userManagementService.createUser(any())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<UserInfoResponse> response = controller.createUser(req);

            assertThat(response.getStatusCode().value()).isEqualTo(500);
        }
    }

    // ── GET /users/{id} ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/users/{id} — getUserById()")
    class GetUserById {

        @Test
        @DisplayName("Returns 200 with user when found")
        void returns200WhenFound() {
            when(userManagementService.getUserById(1L)).thenReturn(sampleUser());

            ResponseEntity<UserInfoResponse> response = controller.getUserById(1L);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Returns 404 when user not found")
        void returns404WhenNotFound() {
            when(userManagementService.getUserById(999L)).thenThrow(new RuntimeException("Not found"));

            ResponseEntity<UserInfoResponse> response = controller.getUserById(999L);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }
    }

    // ── GET /users ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/users — getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("Returns 200 with paged response")
        void returns200WithPagedResult() {
            Page<UserInfoResponse> page = new PageImpl<>(List.of(sampleUser()),
                    PageRequest.of(0, 20), 1);
            when(userManagementService.getAllUsers(any(), any())).thenReturn(page);

            ResponseEntity<java.util.Map<String, Object>> response = controller.getAllUsers(0, 20, null);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsKey("data");
            assertThat(response.getBody()).containsKey("pagination");
        }

        @Test
        @DisplayName("Returns 500 on unexpected service error")
        void returns500OnError() {
            when(userManagementService.getAllUsers(any(), any())).thenThrow(new RuntimeException("DB down"));

            ResponseEntity<java.util.Map<String, Object>> response = controller.getAllUsers(0, 20, null);

            assertThat(response.getStatusCode().value()).isEqualTo(500);
        }
    }
}
