package com.propertize.platform.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * RSA Key Rotation Manager

 * Handles the lifecycle of RSA keys including:
 * - Key rotation (scheduled and manual)
 * - Key backup before rotation
 * - Support for multiple valid keys during rotation window
 * - Grace period for old keys

 * Key Rotation Strategy:
 * 1. Generate new key pair
 * 2. Keep old public key valid for grace period
 * 3. Switch to new private key for signing
 * 4. After grace period, retire old keys
 */
@Slf4j
@Component
public class KeyRotationManager {

    @Value("${jwt.rsa.key-rotation.enabled:false}")
    private boolean rotationEnabled;

    @Value("${jwt.rsa.key-rotation.interval-days:90}")
    private int rotationIntervalDays;

    @Value("${jwt.rsa.key-rotation.grace-period-hours:24}")
    private int gracePeriodHours;

    @Value("${jwt.rsa.key-rotation.backup-count:5}")
    private int backupCount;

    @Value("${jwt.rsa.public-key-path:config/keys/public_key.pem}")
    private String publicKeyPath;

    @Value("${jwt.rsa.private-key-path:config/keys/private_key.pem}")
    private String privateKeyPath;

    @Value("${jwt.rsa.key-rotation.backup-path:config/keys/backup}")
    private String backupPath;

    @Value("${jwt.rsa.key-rotation.key-size:2048}")
    private int keySize;

    private final RsaKeyProvider rsaKeyProvider;

    // Keep track of valid public keys during rotation
    private final CopyOnWriteArrayList<ValidKey> validPublicKeys = new CopyOnWriteArrayList<>();

    public KeyRotationManager(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    @PostConstruct
    public void init() {
        if (rotationEnabled) {
            log.info("🔑 Key rotation is ENABLED - interval: {} days, grace period: {} hours",
                rotationIntervalDays, gracePeriodHours);

            // Add current key as valid
            if (rsaKeyProvider.getPublicKey() != null) {
                validPublicKeys.add(new ValidKey(
                    rsaKeyProvider.getPublicKey(),
                    LocalDateTime.now(),
                    null // No expiry for initial key until rotation
                ));
            }
        } else {
            log.info("🔑 Key rotation is DISABLED");
        }
    }

    /**
     * Scheduled key rotation check
     * Runs daily to check if keys need rotation
     */
    @Scheduled(cron = "${jwt.rsa.key-rotation.check-cron:0 0 2 * * *}") // 2 AM daily
    public void scheduledRotationCheck() {
        if (!rotationEnabled) {
            return;
        }

        log.info("🔑 Running scheduled key rotation check");

        try {
            if (shouldRotateKeys()) {
                log.info("🔄 Keys need rotation - initiating rotation process");
                rotateKeys();
            } else {
                log.debug("✅ Keys are still valid - no rotation needed");
            }

            // Cleanup expired keys
            cleanupExpiredKeys();
        } catch (Exception e) {
            log.error("❌ Key rotation check failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if keys should be rotated based on age
     */
    public boolean shouldRotateKeys() {
        Path publicKey = Paths.get(publicKeyPath);
        if (!Files.exists(publicKey)) {
            return false;
        }

        try {
            java.nio.file.attribute.BasicFileAttributes attrs =
                Files.readAttributes(publicKey, java.nio.file.attribute.BasicFileAttributes.class);

            java.time.Instant creationTime = attrs.creationTime().toInstant();
            java.time.Instant rotationThreshold = java.time.Instant.now()
                .minus(rotationIntervalDays, java.time.temporal.ChronoUnit.DAYS);

            return creationTime.isBefore(rotationThreshold);
        } catch (IOException e) {
            log.warn("Could not check key age: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Manually trigger key rotation
     */
    public KeyRotationResult rotateKeys() {
        if (!rotationEnabled) {
            return KeyRotationResult.ofFailure("Key rotation is disabled");
        }

        log.info("🔄 Starting key rotation process");

        try {
            // Step 1: Backup current keys
            backupCurrentKeys();

            // Step 2: Generate new key pair
            KeyPair newKeyPair = generateKeyPair();

            // Step 3: Add old public key to valid list with expiry
            PublicKey oldPublicKey = rsaKeyProvider.getPublicKey();
            if (oldPublicKey != null) {
                LocalDateTime expiry = LocalDateTime.now().plusHours(gracePeriodHours);
                validPublicKeys.add(new ValidKey(oldPublicKey, LocalDateTime.now(), expiry));
                log.info("📜 Old public key will remain valid until: {}", expiry);
            }

            // Step 4: Save new keys
            saveKeyPair(newKeyPair);

            // Step 5: Reload keys in provider
            rsaKeyProvider.init();

            // Step 6: Add new public key to valid list
            validPublicKeys.add(new ValidKey(
                newKeyPair.getPublic(),
                LocalDateTime.now(),
                null // Primary key, no expiry
            ));

            log.info("✅ Key rotation completed successfully");
            return KeyRotationResult.ofSuccess();

        } catch (Exception e) {
            log.error("❌ Key rotation failed: {}", e.getMessage(), e);
            return KeyRotationResult.ofFailure(e.getMessage());
        }
    }

    /**
     * Check if a public key is currently valid
     */
    public boolean isKeyValid(PublicKey key) {
        return validPublicKeys.stream()
            .filter(vk -> !vk.isExpired())
            .anyMatch(vk -> vk.key().equals(key));
    }

    /**
     * Get all currently valid public keys
     */
    public List<PublicKey> getValidPublicKeys() {
        cleanupExpiredKeys();
        return validPublicKeys.stream()
            .filter(vk -> !vk.isExpired())
            .map(ValidKey::key)
            .toList();
    }

    /**
     * Get key rotation status
     */
    public KeyRotationStatus getStatus() {
        Path publicKey = Paths.get(publicKeyPath);
        java.time.Instant keyCreationTime = null;
        java.time.Instant nextRotation = null;

        try {
            if (Files.exists(publicKey)) {
                java.nio.file.attribute.BasicFileAttributes attrs =
                    Files.readAttributes(publicKey, java.nio.file.attribute.BasicFileAttributes.class);
                keyCreationTime = attrs.creationTime().toInstant();
                nextRotation = keyCreationTime.plus(rotationIntervalDays, java.time.temporal.ChronoUnit.DAYS);
            }
        } catch (IOException e) {
            log.warn("Could not read key attributes: {}", e.getMessage());
        }

        return KeyRotationStatus.builder()
            .rotationEnabled(rotationEnabled)
            .rotationIntervalDays(rotationIntervalDays)
            .gracePeriodHours(gracePeriodHours)
            .keyCreationTime(keyCreationTime)
            .nextScheduledRotation(nextRotation)
            .validKeyCount(validPublicKeys.size())
            .build();
    }

    // ============================================
    // Private Helper Methods
    // ============================================

    private void backupCurrentKeys() throws IOException {
        Path backupDir = Paths.get(backupPath);
        Files.createDirectories(backupDir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        Path currentPublic = Paths.get(publicKeyPath);
        Path currentPrivate = Paths.get(privateKeyPath);

        if (Files.exists(currentPublic)) {
            Files.copy(currentPublic, backupDir.resolve("public_key_" + timestamp + ".pem"),
                StandardCopyOption.REPLACE_EXISTING);
        }

        if (Files.exists(currentPrivate)) {
            Files.copy(currentPrivate, backupDir.resolve("private_key_" + timestamp + ".pem"),
                StandardCopyOption.REPLACE_EXISTING);
        }

        // Cleanup old backups
        cleanupOldBackups();

        log.info("📦 Keys backed up with timestamp: {}", timestamp);
    }

    private void cleanupOldBackups() throws IOException {
        Path backupDir = Paths.get(backupPath);
        if (!Files.exists(backupDir)) return;

        List<Path> backupFiles;
        try (var stream = Files.list(backupDir)) {
            backupFiles = stream
                .filter(p -> p.getFileName().toString().startsWith("public_key_"))
                .sorted(Comparator.comparing(Path::getFileName).reversed())
                .toList();
        }

        // Keep only the configured number of backups
        if (backupFiles.size() > backupCount) {
            for (int i = backupCount; i < backupFiles.size(); i++) {
                Files.deleteIfExists(backupFiles.get(i));
                // Also delete corresponding private key
                String privateKeyName = backupFiles.get(i).getFileName().toString()
                    .replace("public_key_", "private_key_");
                Files.deleteIfExists(backupDir.resolve(privateKeyName));
            }
            log.info("🧹 Cleaned up {} old backup files", backupFiles.size() - backupCount);
        }
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(keySize);
        KeyPair keyPair = generator.generateKeyPair();
        log.info("🔐 Generated new RSA key pair (size: {} bits)", keySize);
        return keyPair;
    }

    private void saveKeyPair(KeyPair keyPair) throws IOException {
        // Ensure directories exist
        Files.createDirectories(Paths.get(publicKeyPath).getParent());
        Files.createDirectories(Paths.get(privateKeyPath).getParent());

        // Save public key
        String publicKeyPem = toPem(keyPair.getPublic().getEncoded(), "PUBLIC KEY");
        Files.writeString(Paths.get(publicKeyPath), publicKeyPem, StandardCharsets.UTF_8);

        // Save private key
        String privateKeyPem = toPem(keyPair.getPrivate().getEncoded(), "PRIVATE KEY");
        Files.writeString(Paths.get(privateKeyPath), privateKeyPem, StandardCharsets.UTF_8);

        log.info("💾 Saved new key pair to: {} and {}", publicKeyPath, privateKeyPath);
    }

    private String toPem(byte[] encoded, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN ").append(type).append("-----\n");

        String base64 = Base64.getEncoder().encodeToString(encoded);
        for (int i = 0; i < base64.length(); i += 64) {
            sb.append(base64, i, Math.min(i + 64, base64.length()));
            sb.append("\n");
        }

        sb.append("-----END ").append(type).append("-----");
        return sb.toString();
    }

    private void cleanupExpiredKeys() {
        int before = validPublicKeys.size();
        validPublicKeys.removeIf(ValidKey::isExpired);
        int removed = before - validPublicKeys.size();
        if (removed > 0) {
            log.info("🧹 Removed {} expired keys from valid key list", removed);
        }
    }

    // ============================================
    // Inner Classes
    // ============================================

    private record ValidKey(PublicKey key, LocalDateTime addedAt, LocalDateTime expiresAt) {

        boolean isExpired() {
                return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
            }
        }

    public record KeyRotationResult(boolean succeeded, String message) {
        public static KeyRotationResult ofSuccess() {
            return new KeyRotationResult(true, "Key rotation completed successfully");
        }

        public static KeyRotationResult ofFailure(String reason) {
            return new KeyRotationResult(false, reason);
        }
    }

    @lombok.Builder
    public record KeyRotationStatus(
        boolean rotationEnabled,
        int rotationIntervalDays,
        int gracePeriodHours,
        java.time.Instant keyCreationTime,
        java.time.Instant nextScheduledRotation,
        int validKeyCount
    ) {}
}
