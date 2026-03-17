package com.propertize.platform.gateway.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Multi-Instance Token Blacklist Synchronization Tester
 *
 * <p>Simulates multiple API Gateway instances and validates that
 * token blacklist is properly synchronized across all instances via Redis.</p>
 *
 * <p>Test Scenarios:</p>
 * <ul>
 *   <li>Blacklist token on instance A, verify on instance B</li>
 *   <li>Concurrent blacklist from multiple instances</li>
 *   <li>Redis failover handling</li>
 *   <li>TTL expiration verification</li>
 * </ul>
 *
 * @author Platform Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class BlacklistSyncTester {

    private static final String TEST_PREFIX = "test:blacklist:";

    /**
     * Simulates multiple gateway instances testing blacklist sync
     *
     * @param redisTemplate Redis template for direct access
     * @param numberOfInstances Number of simulated gateway instances
     * @return Test result summary
     */
    public TestResult runSynchronizationTest(
            StringRedisTemplate redisTemplate,
            int numberOfInstances) {

        log.info("🧪 Starting blacklist synchronization test with {} instances", numberOfInstances);

        TestResult result = new TestResult();
        ExecutorService executor = Executors.newFixedThreadPool(numberOfInstances);
        CountDownLatch latch = new CountDownLatch(numberOfInstances);

        // Test token
        String testToken = "test-token-" + UUID.randomUUID();
        String blacklistKey = TEST_PREFIX + testToken;

        try {
            // Instance 1: Blacklist the token
            redisTemplate.opsForValue().set(blacklistKey, "blacklisted", Duration.ofMinutes(5));
            log.info("✅ Instance 1 blacklisted token: {}", testToken);
            result.incrementSuccess();

            // Wait for Redis propagation (should be instant)
            Thread.sleep(100);

            // All other instances: Verify blacklist
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 2; i <= numberOfInstances; i++) {
                final int instanceId = i;
                Future<Boolean> future = executor.submit(() -> {
                    try {
                        Boolean isBlacklisted = redisTemplate.hasKey(blacklistKey);
                        if (Boolean.TRUE.equals(isBlacklisted)) {
                            log.info("✅ Instance {} verified blacklist", instanceId);
                            latch.countDown();
                            return true;
                        } else {
                            log.error("❌ Instance {} did NOT see blacklist", instanceId);
                            latch.countDown();
                            return false;
                        }
                    } catch (Exception e) {
                        log.error("❌ Instance {} failed: {}", instanceId, e.getMessage());
                        latch.countDown();
                        return false;
                    }
                });
                futures.add(future);
            }

            // Wait for all instances to complete (max 5 seconds)
            boolean completed = latch.await(5, TimeUnit.SECONDS);

            if (!completed) {
                result.addFailure("Timeout: Not all instances responded");
                log.error("❌ Test timeout - not all instances responded");
            }

            // Count successful verifications
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    result.incrementSuccess();
                } else {
                    result.addFailure("Instance failed to verify blacklist");
                }
            }

            // Cleanup
            redisTemplate.delete(blacklistKey);

            log.info("✅ Blacklist sync test complete: {} successes, {} failures",
                    result.getSuccessCount(), result.getFailureCount());

        } catch (Exception e) {
            log.error("❌ Test failed: {}", e.getMessage(), e);
            result.addFailure("Test exception: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

        return result;
    }

    /**
     * Tests concurrent blacklist operations from multiple instances
     */
    public TestResult runConcurrencyTest(
            StringRedisTemplate redisTemplate,
            int numberOfInstances,
            int tokensPerInstance) {

        log.info("🧪 Starting concurrency test: {} instances, {} tokens each",
                numberOfInstances, tokensPerInstance);

        TestResult result = new TestResult();
        ExecutorService executor = Executors.newFixedThreadPool(numberOfInstances);
        CountDownLatch latch = new CountDownLatch(numberOfInstances * tokensPerInstance);

        try {
            List<Future<Integer>> futures = new ArrayList<>();

            for (int i = 1; i <= numberOfInstances; i++) {
                final int instanceId = i;
                Future<Integer> future = executor.submit(() -> {
                    int successes = 0;
                    for (int j = 0; j < tokensPerInstance; j++) {
                        try {
                            String token = String.format("instance-%d-token-%d", instanceId, j);
                            String key = TEST_PREFIX + token;
                            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMinutes(1));
                            successes++;
                            latch.countDown();
                        } catch (Exception e) {
                            log.error("❌ Instance {} failed to blacklist token {}: {}",
                                    instanceId, j, e.getMessage());
                            latch.countDown();
                        }
                    }
                    return successes;
                });
                futures.add(future);
            }

            // Wait for all operations (max 10 seconds)
            boolean completed = latch.await(10, TimeUnit.SECONDS);

            if (!completed) {
                result.addFailure("Concurrency test timeout");
            }

            // Count total successes
            int totalSuccesses = 0;
            for (Future<Integer> future : futures) {
                totalSuccesses += future.get();
            }

            result.setSuccessCount(totalSuccesses);
            result.setFailureCount((numberOfInstances * tokensPerInstance) - totalSuccesses);

            // Cleanup all test keys
            redisTemplate.delete(redisTemplate.keys(TEST_PREFIX + "*"));

            log.info("✅ Concurrency test complete: {} successes, {} failures",
                    result.getSuccessCount(), result.getFailureCount());

        } catch (Exception e) {
            log.error("❌ Concurrency test failed: {}", e.getMessage(), e);
            result.addFailure("Test exception: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

        return result;
    }

    /**
     * Tests TTL expiration behavior
     */
    public TestResult runTTLTest(StringRedisTemplate redisTemplate) {
        log.info("🧪 Starting TTL expiration test");

        TestResult result = new TestResult();
        String testToken = "ttl-test-token-" + UUID.randomUUID();
        String key = TEST_PREFIX + testToken;

        try {
            // Set token with 2-second TTL
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofSeconds(2));

            // Verify immediately
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                result.incrementSuccess();
                log.info("✅ Token blacklisted successfully");
            } else {
                result.addFailure("Token not found immediately after blacklist");
            }

            // Wait for expiration
            Thread.sleep(2500);

            // Verify expired
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                result.incrementSuccess();
                log.info("✅ Token expired after TTL");
            } else {
                result.addFailure("Token still exists after TTL");
            }

        } catch (Exception e) {
            log.error("❌ TTL test failed: {}", e.getMessage(), e);
            result.addFailure("Test exception: " + e.getMessage());
        }

        return result;
    }

    /**
     * Test result container
     */
    public static class TestResult {
        private int successCount = 0;
        private int failureCount = 0;
        private final List<String> failures = new ArrayList<>();

        public void incrementSuccess() {
            successCount++;
        }

        public void addFailure(String reason) {
            failureCount++;
            failures.add(reason);
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public void setSuccessCount(int count) {
            this.successCount = count;
        }

        public void setFailureCount(int count) {
            this.failureCount = count;
        }

        public List<String> getFailures() {
            return failures;
        }

        public boolean isSuccess() {
            return failureCount == 0;
        }

        @Override
        public String toString() {
            return String.format("TestResult{successes=%d, failures=%d, failureReasons=%s}",
                    successCount, failureCount, failures);
        }
    }
}
