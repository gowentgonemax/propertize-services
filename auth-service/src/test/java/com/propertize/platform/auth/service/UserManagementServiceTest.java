package com.propertize.platform.auth.service;

import com.propertize.platform.auth.dto.CreateUserRequest;
import com.propertize.platform.auth.dto.UserInfoResponse;
import com.propertize.platform.auth.entity.User;
import com.propertize.platform.auth.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserManagementService.createUser() — focuses on the
 * organizationType field that was previously dropped during user creation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserManagementService Tests")
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserManagementService userManagementService;

    private CreateUserRequest validRequest() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("jane.doe");
        req.setEmail("jane@example.com");
        req.setPassword("Password1!");
        req.setFirstName("Jane");
        req.setLastName("Doe");
        req.setOrganizationId("10");
        req.setOrganizationCode("ORG-TEN");
        return req;
    }

    @BeforeEach
    void setUp() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return u; // return the same user passed in (simulates save)
        });
    }

    @Nested
    @DisplayName("createUser organizationType Tests")
    class CreateUserOrgTypeTests {

        @Test
        @DisplayName("Should persist organizationType when provided")
        void testOrgTypePersisted() {
            CreateUserRequest req = validRequest();
            req.setOrganizationType("INDIVIDUAL_PROPERTY_OWNER");

            userManagementService.createUser(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals("INDIVIDUAL_PROPERTY_OWNER", captor.getValue().getOrganizationType());
        }

        @Test
        @DisplayName("Should persist null organizationType when not provided")
        void testNullOrgTypeNotOverridden() {
            CreateUserRequest req = validRequest();
            req.setOrganizationType(null);

            userManagementService.createUser(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertNull(captor.getValue().getOrganizationType());
        }

        @Test
        @DisplayName("Should persist PROPERTY_MANAGEMENT_COMPANY org type correctly")
        void testPmcOrgType() {
            CreateUserRequest req = validRequest();
            req.setOrganizationType("PROPERTY_MANAGEMENT_COMPANY");

            userManagementService.createUser(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals("PROPERTY_MANAGEMENT_COMPANY", captor.getValue().getOrganizationType());
        }
    }

    @Nested
    @DisplayName("createUser validation Tests")
    class CreateUserValidationTests {

        @Test
        @DisplayName("Should throw when username already exists")
        void testDuplicateUsername() {
            when(userRepository.findByUsername("jane.doe")).thenReturn(Optional.of(new User()));

            CreateUserRequest req = validRequest();
            assertThrows(IllegalArgumentException.class, () -> userManagementService.createUser(req));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when email already exists")
        void testDuplicateEmail() {
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(new User()));

            CreateUserRequest req = validRequest();
            assertThrows(IllegalArgumentException.class, () -> userManagementService.createUser(req));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should enable user by default when enabled field is null")
        void testDefaultEnabledIsTrue() {
            CreateUserRequest req = validRequest();
            req.setEnabled(null);

            userManagementService.createUser(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertTrue(captor.getValue().getEnabled());
        }

        @Test
        @DisplayName("Should use provided roles set")
        void testRolesPersisted() {
            CreateUserRequest req = validRequest();
            req.setRoles(Set.of(com.propertize.enums.UserRoleEnum.ORGANIZATION_ADMIN));

            userManagementService.createUser(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertFalse(captor.getValue().getRoles().isEmpty());
        }
    }
}
