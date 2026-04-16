package com.propertize.platform.auth.service;

import com.propertize.enums.UserRoleEnum;
import com.propertize.platform.auth.entity.User;
import com.propertize.platform.auth.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User buildActiveUser(Set<UserRoleEnum> roles) {
        User u = new User();
        u.setUsername("jdoe");
        u.setPassword("hashed");
        u.setEnabled(true);
        u.setAccountNonExpired(true);
        u.setCredentialsNonExpired(true);
        u.setAccountNonLocked(true);
        u.setRoles(roles);
        return u;
    }

    @Nested
    @DisplayName("loadUserByUsername()")
    class LoadUserByUsername {

        @Test
        @DisplayName("Loads user by username when found directly")
        void loadsByUsername() {
            User user = buildActiveUser(Set.of(UserRoleEnum.PROPERTY_MANAGER));
            when(userRepository.findByUsernameWithRoles("jdoe")).thenReturn(Optional.of(user));

            UserDetails details = customUserDetailsService.loadUserByUsername("jdoe");

            assertThat(details.getUsername()).isEqualTo("jdoe");
            assertThat(details.getPassword()).isEqualTo("hashed");
            assertThat(details.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Falls back to email lookup when username lookup returns empty")
        void fallsBackToEmailLookup() {
            User user = buildActiveUser(Set.of(UserRoleEnum.TENANT));
            when(userRepository.findByUsernameWithRoles("jane@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmailWithRoles("jane@example.com")).thenReturn(Optional.of(user));

            UserDetails details = customUserDetailsService.loadUserByUsername("jane@example.com");

            assertThat(details.getUsername()).isEqualTo("jdoe");
        }

        @Test
        @DisplayName("Throws UsernameNotFoundException when user not found at all")
        void throwsWhenNotFound() {
            when(userRepository.findByUsernameWithRoles("unknown")).thenReturn(Optional.empty());
            when(userRepository.findByEmailWithRoles("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Maps roles to ROLE_-prefixed GrantedAuthority")
        void mapsRolesToAuthorities() {
            User user = buildActiveUser(Set.of(UserRoleEnum.ORGANIZATION_ADMIN));
            when(userRepository.findByUsernameWithRoles("jdoe")).thenReturn(Optional.of(user));

            UserDetails details = customUserDetailsService.loadUserByUsername("jdoe");

            assertThat(details.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_ORGANIZATION_ADMIN");
        }

        @Test
        @DisplayName("Returns empty authorities when user has no roles")
        void handlesNullRoles() {
            User user = buildActiveUser(null);
            when(userRepository.findByUsernameWithRoles("norole")).thenReturn(Optional.of(user));

            UserDetails details = customUserDetailsService.loadUserByUsername("norole");

            assertThat(details.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("Propagates account flags: disabled account")
        void propagatesDisabledFlag() {
            User user = buildActiveUser(Set.of(UserRoleEnum.TENANT));
            user.setEnabled(false);
            when(userRepository.findByUsernameWithRoles("disabled")).thenReturn(Optional.of(user));

            UserDetails details = customUserDetailsService.loadUserByUsername("disabled");

            assertThat(details.isEnabled()).isFalse();
        }
    }
}
