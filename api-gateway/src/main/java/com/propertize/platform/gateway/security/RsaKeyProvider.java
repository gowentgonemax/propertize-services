package com.propertize.platform.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA Key Provider for Production JWT Verification
 *
 * Supports:
 * - Loading RSA public key from file (production recommended)
 * - Loading RSA private key for service-to-service token generation
 * - Auto-generating keys for development (NOT recommended for production)
 *
 * Key paths:
 * - Public key: ${jwt.rsa.public-key-path} or config/keys/public_key.pem
 * - Private key: ${jwt.rsa.private-key-path} or config/keys/private_key.pem
 */
@Slf4j
@Component
public class RsaKeyProvider {

    private static final String DEFAULT_PUBLIC_KEY_PATH = "config/keys/public_key.pem";
    private static final String DEFAULT_PRIVATE_KEY_PATH = "config/keys/private_key.pem";
    private static final String PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_KEY_FOOTER = "-----END PUBLIC KEY-----";
    private static final String PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_KEY_FOOTER = "-----END PRIVATE KEY-----";

    @Value("${jwt.rsa.public-key-path:}")
    private String publicKeyPath;

    @Value("${jwt.rsa.private-key-path:}")
    private String privateKeyPath;

    @Value("${jwt.rsa.auto-generate:false}")
    private boolean autoGenerate;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private boolean rsaEnabled = false;

    @PostConstruct
    public void init() {
        try {
            loadKeys();
        } catch (Exception e) {
            log.warn("RSA keys not available: {}. JWT verification will use HMAC fallback.", e.getMessage());
            if (autoGenerate && isDevProfile()) {
                generateDevKeys();
            }
        }
    }

    private void loadKeys() throws Exception {
        // Load public key
        String pubKeyPathToUse = publicKeyPath != null && !publicKeyPath.isEmpty()
            ? publicKeyPath : DEFAULT_PUBLIC_KEY_PATH;

        Path pubPath = Paths.get(pubKeyPathToUse);
        if (Files.exists(pubPath)) {
            String pubContent = Files.readString(pubPath, StandardCharsets.UTF_8);
            publicKey = parsePublicKey(pubContent);
            log.info("✅ Loaded RSA public key from: {}", pubKeyPathToUse);
        } else {
            throw new IOException("Public key file not found: " + pubKeyPathToUse);
        }

        // Load private key (optional, only needed for service-to-service tokens)
        String privKeyPathToUse = privateKeyPath != null && !privateKeyPath.isEmpty()
            ? privateKeyPath : DEFAULT_PRIVATE_KEY_PATH;

        Path privPath = Paths.get(privKeyPathToUse);
        if (Files.exists(privPath)) {
            String privContent = Files.readString(privPath, StandardCharsets.UTF_8);
            privateKey = parsePrivateKey(privContent);
            log.info("✅ Loaded RSA private key from: {}", privKeyPathToUse);
        } else {
            log.info("Private key not loaded (not required for gateway verification)");
        }

        rsaEnabled = true;
    }

    private PublicKey parsePublicKey(String content) throws Exception {
        String base64Key = extractKeyContent(content, PUBLIC_KEY_HEADER, PUBLIC_KEY_FOOTER);
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private PrivateKey parsePrivateKey(String content) throws Exception {
        String base64Key = extractKeyContent(content, PRIVATE_KEY_HEADER, PRIVATE_KEY_FOOTER);
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private String extractKeyContent(String content, String header, String footer) {
        if (content.contains(header) && content.contains(footer)) {
            int start = content.indexOf(header) + header.length();
            int end = content.indexOf(footer);
            if (start >= 0 && end > start) {
                return content.substring(start, end).replaceAll("\\s", "");
            }
        }
        return content.replaceAll("\\s", "");
    }

    private void generateDevKeys() {
        log.warn("⚠️ Auto-generating RSA keys for development. DO NOT USE IN PRODUCTION!");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
            rsaEnabled = true;

            log.info("Generated ephemeral RSA key pair for development");
        } catch (Exception e) {
            log.error("Failed to generate RSA keys: {}", e.getMessage());
        }
    }

    private boolean isDevProfile() {
        return activeProfile == null ||
               activeProfile.contains("dev") ||
               activeProfile.contains("local") ||
               activeProfile.equals("default");
    }

    public boolean isRsaEnabled() {
        return rsaEnabled;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public boolean hasPrivateKey() {
        return privateKey != null;
    }
}
