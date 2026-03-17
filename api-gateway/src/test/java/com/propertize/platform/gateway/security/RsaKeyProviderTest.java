package com.propertize.platform.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit Tests for RsaKeyProvider
 */
@DisplayName("RsaKeyProvider Tests")
class RsaKeyProviderTest {

    private RsaKeyProvider rsaKeyProvider;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        rsaKeyProvider = new RsaKeyProvider();
        ReflectionTestUtils.setField(rsaKeyProvider, "autoGenerate", false);
        ReflectionTestUtils.setField(rsaKeyProvider, "activeProfile", "test");
    }

    @Nested
    @DisplayName("Key Loading Tests")
    class KeyLoadingTests {

        @Test
        @DisplayName("Should load RSA keys from files")
        void shouldLoadRsaKeysFromFiles() throws Exception {
            // Generate keys and save to temp files
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            // Create PEM files
            Path publicKeyPath = tempDir.resolve("public_key.pem");
            Path privateKeyPath = tempDir.resolve("private_key.pem");

            Files.writeString(publicKeyPath, toPem(keyPair.getPublic().getEncoded(), "PUBLIC KEY"));
            Files.writeString(privateKeyPath, toPem(keyPair.getPrivate().getEncoded(), "PRIVATE KEY"));

            // Configure provider
            ReflectionTestUtils.setField(rsaKeyProvider, "publicKeyPath", publicKeyPath.toString());
            ReflectionTestUtils.setField(rsaKeyProvider, "privateKeyPath", privateKeyPath.toString());

            // Initialize
            rsaKeyProvider.init();

            // Verify
            assertThat(rsaKeyProvider.isRsaEnabled()).isTrue();
            assertThat(rsaKeyProvider.getPublicKey()).isNotNull();
            assertThat(rsaKeyProvider.getPrivateKey()).isNotNull();
            assertThat(rsaKeyProvider.hasPrivateKey()).isTrue();
        }

        @Test
        @DisplayName("Should load only public key when private key is missing")
        void shouldLoadOnlyPublicKey() throws Exception {
            // Generate keys and save only public key
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            Path publicKeyPath = tempDir.resolve("public_key.pem");
            Files.writeString(publicKeyPath, toPem(keyPair.getPublic().getEncoded(), "PUBLIC KEY"));

            // Configure provider (no private key path)
            ReflectionTestUtils.setField(rsaKeyProvider, "publicKeyPath", publicKeyPath.toString());
            ReflectionTestUtils.setField(rsaKeyProvider, "privateKeyPath", tempDir.resolve("nonexistent.pem").toString());

            // Initialize
            rsaKeyProvider.init();

            // Verify
            assertThat(rsaKeyProvider.isRsaEnabled()).isTrue();
            assertThat(rsaKeyProvider.getPublicKey()).isNotNull();
            assertThat(rsaKeyProvider.getPrivateKey()).isNull();
            assertThat(rsaKeyProvider.hasPrivateKey()).isFalse();
        }

        @Test
        @DisplayName("Should not enable RSA when public key file is missing")
        void shouldNotEnableRsaWhenPublicKeyMissing() {
            ReflectionTestUtils.setField(rsaKeyProvider, "publicKeyPath", "/nonexistent/path/public_key.pem");
            ReflectionTestUtils.setField(rsaKeyProvider, "privateKeyPath", "/nonexistent/path/private_key.pem");

            rsaKeyProvider.init();

            assertThat(rsaKeyProvider.isRsaEnabled()).isFalse();
            assertThat(rsaKeyProvider.getPublicKey()).isNull();
        }
    }

    @Nested
    @DisplayName("Auto-Generate Tests")
    class AutoGenerateTests {

        @Test
        @DisplayName("Should auto-generate keys in dev profile")
        void shouldAutoGenerateKeysInDev() {
            ReflectionTestUtils.setField(rsaKeyProvider, "publicKeyPath", "/nonexistent/path/public_key.pem");
            ReflectionTestUtils.setField(rsaKeyProvider, "autoGenerate", true);
            ReflectionTestUtils.setField(rsaKeyProvider, "activeProfile", "dev");

            rsaKeyProvider.init();

            assertThat(rsaKeyProvider.isRsaEnabled()).isTrue();
            assertThat(rsaKeyProvider.getPublicKey()).isNotNull();
            assertThat(rsaKeyProvider.getPrivateKey()).isNotNull();
        }

        @Test
        @DisplayName("Should auto-generate keys in local profile")
        void shouldAutoGenerateKeysInLocal() {
            ReflectionTestUtils.setField(rsaKeyProvider, "publicKeyPath", "/nonexistent/path/public_key.pem");
            ReflectionTestUtils.setField(rsaKeyProvider, "autoGenerate", true);
            ReflectionTestUtils.setField(rsaKeyProvider, "activeProfile", "local");

            rsaKeyProvider.init();

            assertThat(rsaKeyProvider.isRsaEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should NOT auto-generate keys in prod profile")
        void shouldNotAutoGenerateKeysInProd() {
            ReflectionTestUtils.setField(rsaKeyProvider, "publicKeyPath", "/nonexistent/path/public_key.pem");
            ReflectionTestUtils.setField(rsaKeyProvider, "autoGenerate", true);
            ReflectionTestUtils.setField(rsaKeyProvider, "activeProfile", "prod");

            rsaKeyProvider.init();

            assertThat(rsaKeyProvider.isRsaEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("PEM Format Tests")
    class PemFormatTests {

        @Test
        @DisplayName("Should parse PEM with header/footer")
        void shouldParsePemWithHeaderFooter() throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            String pemContent = "-----BEGIN PUBLIC KEY-----\n" +
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()) +
                "\n-----END PUBLIC KEY-----";

            Path publicKeyPath = tempDir.resolve("public_key.pem");
            Files.writeString(publicKeyPath, pemContent);

            ReflectionTestUtils.setField(rsaKeyProvider, "publicKeyPath", publicKeyPath.toString());
            rsaKeyProvider.init();

            assertThat(rsaKeyProvider.isRsaEnabled()).isTrue();
            assertThat(rsaKeyProvider.getPublicKey()).isNotNull();
        }

        @Test
        @DisplayName("Should parse PEM with line breaks")
        void shouldParsePemWithLineBreaks() throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            String base64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            StringBuilder pemContent = new StringBuilder();
            pemContent.append("-----BEGIN PUBLIC KEY-----\n");
            for (int i = 0; i < base64.length(); i += 64) {
                pemContent.append(base64, i, Math.min(i + 64, base64.length()));
                pemContent.append("\n");
            }
            pemContent.append("-----END PUBLIC KEY-----");

            Path publicKeyPath = tempDir.resolve("public_key.pem");
            Files.writeString(publicKeyPath, pemContent.toString());

            ReflectionTestUtils.setField(rsaKeyProvider, "publicKeyPath", publicKeyPath.toString());
            rsaKeyProvider.init();

            assertThat(rsaKeyProvider.isRsaEnabled()).isTrue();
        }
    }

    // Helper method
    private String toPem(byte[] encoded, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN ").append(type).append("-----\n");
        sb.append(Base64.getEncoder().encodeToString(encoded));
        sb.append("\n-----END ").append(type).append("-----");
        return sb.toString();
    }
}
