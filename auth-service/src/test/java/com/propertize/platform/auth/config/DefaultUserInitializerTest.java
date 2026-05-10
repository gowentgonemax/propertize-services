package com.propertize.platform.auth.config;

import com.propertize.commons.enums.UserRoleEnum;
import com.propertize.platform.auth.entity.User;
import com.propertize.platform.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultUserInitializer Tests")
class DefaultUserInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments applicationArguments;

    private DefaultUserInitializer defaultUserInitializer;

    @Test
    @DisplayName("Removes legacy admin and creates superadmin when missing")
    void removesLegacyAdminAndCreatesSuperadmin() {
        defaultUserInitializer = new DefaultUserInitializer(userRepository, passwordEncoder, "password");

        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(new User()));
        when(userRepository.existsByUsername("superadmin")).thenReturn(false);
        when(userRepository.existsByEmail("superadmin@propertize.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");

        defaultUserInitializer.run(applicationArguments);

        verify(userRepository).delete(any(User.class));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User createdUser = userCaptor.getValue();
        assertThat(createdUser.getUsername()).isEqualTo("superadmin");
        assertThat(createdUser.getEmail()).isEqualTo("superadmin@propertize.com");
        assertThat(createdUser.getPassword()).isEqualTo("encoded-password");
        assertThat(createdUser.getRoles()).contains(UserRoleEnum.PLATFORM_OVERSIGHT);
    }
}
