package com.propertize.platform.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for KeyRotationManager
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KeyRotationManager Tests")
class KeyRotationManagerTest {

    @Mock
    private RsaKeyProvider rsaKeyProvider;

    private KeyRotationManager keyRotationManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        keyRotationManager = new KeyRotationManager(rsaKeyProvider);

        // Default configuration
        ReflectionTestUtils.setField(keyRotationManager, "rotationEnabled", true);
        ReflectionTestUtils.setField(keyRotationManager, "rotationIntervalDays", 90);
        ReflectionTestUtils.setField(keyRotationManager, "gracePeriodHours", 24);
        ReflectionTestUtils.setField(keyRotationManager, "backupCount", 5);
        ReflectionTestUtils.setField(keyRotationManager, "keySize", 2048);

        Path keysDir = tempDir.resolve("keys");
        Path backupDir = tempDir.resolve("backup");

        ReflectionTestUtils.setField(keyRotationManager, "publicKeyPath", keysDir.resolve("public_key.pem").toString());
        ReflectionTestUtils.setField(keyRotationManager, "privateKeyPath", keysDir.resolve("private_key.pem").toString());
        ReflectionTestUtils.setField(keyRotationManager, "backupPath", backupDir.toString());
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize when rotation is enabled")
        void shouldInitializeWhenEnabled() throws Exception {
            KeyPair keyPair = generateKeyPair();
            when(rsaKeyProvider.getPublicKey()).thenReturn(keyPair.getPublic());

            keyRotationManager.init();

            List<PublicKey> validKeys = keyRotationManager.getValidPublicKeys();
            assertThat(validKeys).hasSize(1);
            assertThat(validKeys.get(0)).isEqualTo(keyPair.getPublic());
        }

        @Test
        @DisplayName("Should not initialize when rotation is disabled")
        void shouldNotInitializeWhenDisabled() {
            ReflectionTestUtils.setField(keyRotationManager, "rotationEnabled", false);

            keyRotationManager.init();

            verifyNoInteractions(rsaKeyProvider);
        }
    }

    @Nested
    @DisplayName("Key Rotation Tests")
    class KeyRotationTests {

        @Test
        @DisplayName("Should fail rotation when disabled")
        void shouldFailRotationWhenDisabled() {
            ReflectionTestUtils.setField(keyRotationManager, "rotationEnabled", false);

            KeyRotationManager.KeyRotationResult result = keyRotationManager.rotateKeys();

            assertThat(result.succeeded()).isFalse();
            assertThat(result.message()).contains("disabled");
        }

        @Test
        @DisplayName("Should rotate keys successfully")
        void shouldRotateKeysSuccessfully() throws Exception {
            // Setup initial key
            KeyPair initialKeyPair = generateKeyPair();
            when(rsaKeyProvider.getPublicKey()).thenReturn(initialKeyPair.getPublic());

            // Create initial key files
            Path keysDir = tempDir.resolve("keys");
            Files.createDirectories(keysDir);
            Files.writeString(keysDir.resolve("public_key.pem"), toPem(initialKeyPair.getPublic().getEncoded(), "PUBLIC KEY"));
            Files.writeString(keysDir.resolve("private_key.pem"), toPem(initialKeyPair.getPrivate().getEncoded(), "PRIVATE KEY"));

            // Mock rsaKeyProvider.init() to do nothing
            doNothing().when(rsaKeyProvider).init();

            // Initialize
            keyRotationManager.init();

            // Rotate keys
            KeyRotationManager.KeyRotationResult result = keyRotationManager.rotateKeys();

            assertThat(result.succeeded()).isTrue();

            // Verify backup was created
            Path backupDir = tempDir.resolve("backup");
            assertThat(Files.list(backupDir).count()).isGreaterThan(0);

            // Verify new key files exist
            assertThat(Files.exists(keysDir.resolve("public_key.pem"))).isTrue();
            assertThat(Files.exists(keysDir.resolve("private_key.pem"))).isTrue();
        }

        @Test
        @DisplayName("Should keep old key valid during grace period")
        void shouldKeepOldKeyValidDuringGracePeriod() throws Exception {
            KeyPair initialKeyPair = generateKeyPair();
            when(rsaKeyProvider.getPublicKey()).thenReturn(initialKeyPair.getPublic());

            // Create initial key files
            Path keysDir = tempDir.resolve("keys");
            Files.createDirectories(keysDir);
            Files.writeString(keysDir.resolve("public_key.pem"), toPem(initialKeyPair.getPublic().getEncoded(), "PUBLIC KEY"));
            Files.writeString(keysDir.resolve("private_key.pem"), toPem(initialKeyPair.getPrivate().getEncoded(), "PRIVATE KEY"));

            doNothing().when(rsaKeyProvider).init();

            keyRotationManager.init();
            keyRotationManager.rotateKeys();

            // Both old and new keys should be valid
            List<PublicKey> validKeys = keyRotationManager.getValidPublicKeys();
            assertThat(validKeys.size()).isGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Key Validity Tests")
    class KeyValidityTests {

        @Test
        @DisplayName("Should report key as valid when in valid list")
        void shouldReportKeyAsValid() throws Exception {
            KeyPair keyPair = generateKeyPair();
            when(rsaKeyProvider.getPublicKey()).thenReturn(keyPair.getPublic());

            keyRotationManager.init();

            assertThat(keyRotationManager.isKeyValid(keyPair.getPublic())).isTrue();
        }

        @Test
        @DisplayName("Should report unknown key as invalid")
        void shouldReportUnknownKeyAsInvalid() throws Exception {
            KeyPair keyPair1 = generateKeyPair();
            KeyPair keyPair2 = generateKeyPair();

            when(rsaKeyProvider.getPublicKey()).thenReturn(keyPair1.getPublic());

            keyRotationManager.init();

            assertThat(keyRotationManager.isKeyValid(keyPair2.getPublic())).isFalse();
        }
    }

    @Nested
    @DisplayName("Status Tests")
    class StatusTests {

        @Test
        @DisplayName("Should return correct status")
        void shouldReturnCorrectStatus() throws Exception {
            KeyPair keyPair = generateKeyPair();
            when(rsaKeyProvider.getPublicKey()).thenReturn(keyPair.getPublic());

            keyRotationManager.init();

            KeyRotationManager.KeyRotationStatus status = keyRotationManager.getStatus();

            assertThat(status.rotationEnabled()).isTrue();
            assertThat(status.rotationIntervalDays()).isEqualTo(90);
            assertThat(status.gracePeriodHours()).isEqualTo(24);
            assertThat(status.validKeyCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should report disabled status when rotation is off")
        void shouldReportDisabledStatus() {
            ReflectionTestUtils.setField(keyRotationManager, "rotationEnabled", false);

            KeyRotationManager.KeyRotationStatus status = keyRotationManager.getStatus();

            assertThat(status.rotationEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Backup Management Tests")
    class BackupManagementTests {

        @Test
        @DisplayName("Should limit backup count")
        void shouldLimitBackupCount() throws Exception {
            ReflectionTestUtils.setField(keyRotationManager, "backupCount", 2);

            KeyPair keyPair = generateKeyPair();
            when(rsaKeyProvider.getPublicKey()).thenReturn(keyPair.getPublic());
            doNothing().when(rsaKeyProvider).init();

            // Create initial key files
            Path keysDir = tempDir.resolve("keys");
            Files.createDirectories(keysDir);
            Files.writeString(keysDir.resolve("public_key.pem"), toPem(keyPair.getPublic().getEncoded(), "PUBLIC KEY"));
            Files.writeString(keysDir.resolve("private_key.pem"), toPem(keyPair.getPrivate().getEncoded(), "PRIVATE KEY"));

            keyRotationManager.init();

            // Rotate multiple times
            for (int i = 0; i < 4; i++) {
                keyRotationManager.rotateKeys();
                Thread.sleep(100); // Ensure different timestamps
            }

            // Check backup count
            Path backupDir = tempDir.resolve("backup");
            long publicKeyBackups = Files.list(backupDir)
                .filter(p -> p.getFileName().toString().startsWith("public_key_"))
                .count();

            assertThat(publicKeyBackups).isLessThanOrEqualTo(2);
        }
    }

    // Helper methods
    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String toPem(byte[] encoded, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN ").append(type).append("-----\n");
        sb.append(Base64.getEncoder().encodeToString(encoded));
        sb.append("\n-----END ").append(type).append("-----");
        return sb.toString();
    }
}
