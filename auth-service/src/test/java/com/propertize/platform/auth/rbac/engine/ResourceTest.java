package com.propertize.platform.auth.rbac.engine;

import com.propertize.platform.auth.rbac.enums.CategoryEnum;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Resource enum — validates all 30+ resources,
 * key lookup, existence check, categories, and toString.
 */
@DisplayName("Resource Enum Tests")
class ResourceTest {

    // ==================== fromKey ====================

    @Nested
    @DisplayName("fromKey Tests")
    class FromKeyTests {

        @Test
        @DisplayName("Should resolve core business resources")
        void testCoreBusiness() {
            assertEquals(Resource.PROPERTY, Resource.fromKey("property"));
            assertEquals(Resource.LEASE, Resource.fromKey("lease"));
            assertEquals(Resource.TENANT, Resource.fromKey("tenant"));
        }

        @Test
        @DisplayName("Should resolve financial resources")
        void testFinancial() {
            assertEquals(Resource.PAYMENT, Resource.fromKey("payment"));
            assertEquals(Resource.INVOICE, Resource.fromKey("invoice"));
            assertEquals(Resource.LATE_FEE, Resource.fromKey("late_fee"));
        }

        @Test
        @DisplayName("Should resolve operational resources")
        void testOperational() {
            assertEquals(Resource.MAINTENANCE, Resource.fromKey("maintenance"));
            assertEquals(Resource.INSPECTION, Resource.fromKey("inspection"));
            assertEquals(Resource.VENDOR, Resource.fromKey("vendor"));
            assertEquals(Resource.ASSET, Resource.fromKey("asset"));
        }

        @Test
        @DisplayName("Should resolve system resources")
        void testSystem() {
            assertEquals(Resource.USER, Resource.fromKey("user"));
            assertEquals(Resource.ORGANIZATION, Resource.fromKey("organization"));
            assertEquals(Resource.ROLE, Resource.fromKey("role"));
            assertEquals(Resource.PLATFORM, Resource.fromKey("platform"));
            assertEquals(Resource.AUDIT_LOG, Resource.fromKey("audit_log"));
        }

        @Test
        @DisplayName("Should resolve communication resources")
        void testCommunication() {
            assertEquals(Resource.NOTIFICATION, Resource.fromKey("notification"));
            assertEquals(Resource.DOCUMENT, Resource.fromKey("document"));
        }

        @Test
        @DisplayName("Should resolve workflow resources")
        void testWorkflow() {
            assertEquals(Resource.RENTAL_APPLICATION, Resource.fromKey("rental_application"));
            assertEquals(Resource.WORKFLOW, Resource.fromKey("workflow"));
            assertEquals(Resource.ONBOARDING, Resource.fromKey("onboarding"));
        }

        @Test
        @DisplayName("Should resolve analytics resources")
        void testAnalytics() {
            assertEquals(Resource.REPORT, Resource.fromKey("report"));
            assertEquals(Resource.DASHBOARD, Resource.fromKey("dashboard"));
            assertEquals(Resource.MILESTONE, Resource.fromKey("milestone"));
        }

        @Test
        @DisplayName("Should throw for unknown resource key")
        void testUnknownKey() {
            assertThrows(IllegalArgumentException.class,
                    () -> Resource.fromKey("unknown_resource"));
        }
    }

    // ==================== exists ====================

    @Test
    @DisplayName("Should return true for known keys")
    void testExistsKnown() {
        assertTrue(Resource.exists("property"));
        assertTrue(Resource.exists("tenant"));
        assertTrue(Resource.exists("audit_log"));
        assertTrue(Resource.exists("dashboard"));
    }

    @Test
    @DisplayName("Should return false for unknown keys")
    void testExistsUnknown() {
        assertFalse(Resource.exists("spaceship"));
        assertFalse(Resource.exists(""));
    }

    // ==================== Categories ====================

    @Nested
    @DisplayName("Category Tests")
    class CategoryTests {

        @Test
        @DisplayName("Core business resources should have BUSINESS category")
        void testBusinessCategory() {
            assertEquals(CategoryEnum.BUSINESS, Resource.PROPERTY.getCategoryEnum());
            assertEquals(CategoryEnum.BUSINESS, Resource.LEASE.getCategoryEnum());
            assertEquals(CategoryEnum.BUSINESS, Resource.TENANT.getCategoryEnum());
        }

        @Test
        @DisplayName("Financial resources should have FINANCIAL category")
        void testFinancialCategory() {
            assertEquals(CategoryEnum.FINANCIAL, Resource.PAYMENT.getCategoryEnum());
            assertEquals(CategoryEnum.FINANCIAL, Resource.INVOICE.getCategoryEnum());
        }

        @Test
        @DisplayName("Operational resources should have OPERATIONS category")
        void testOperationsCategory() {
            assertEquals(CategoryEnum.OPERATIONS, Resource.MAINTENANCE.getCategoryEnum());
            assertEquals(CategoryEnum.OPERATIONS, Resource.VENDOR.getCategoryEnum());
        }

        @Test
        @DisplayName("System resources should have SYSTEM category")
        void testSystemCategory() {
            assertEquals(CategoryEnum.SYSTEM, Resource.USER.getCategoryEnum());
            assertEquals(CategoryEnum.SYSTEM, Resource.ORGANIZATION.getCategoryEnum());
            assertEquals(CategoryEnum.SYSTEM, Resource.PLATFORM.getCategoryEnum());
        }

        @Test
        @DisplayName("Analytics resources should have ANALYTICS category")
        void testAnalyticsCategory() {
            assertEquals(CategoryEnum.ANALYTICS, Resource.REPORT.getCategoryEnum());
            assertEquals(CategoryEnum.ANALYTICS, Resource.DASHBOARD.getCategoryEnum());
        }
    }

    // ==================== Properties ====================

    @Test
    @DisplayName("Should have key, description, and category")
    void testProperties() {
        Resource resource = Resource.PROPERTY;
        assertEquals("property", resource.getKey());
        assertEquals("Property management", resource.getDescription());
        assertEquals(CategoryEnum.BUSINESS, resource.getCategoryEnum());
    }

    // ==================== toString ====================

    @Test
    @DisplayName("toString should return key")
    void testToString() {
        assertEquals("property", Resource.PROPERTY.toString());
        assertEquals("rental_application", Resource.RENTAL_APPLICATION.toString());
        assertEquals("audit_log", Resource.AUDIT_LOG.toString());
    }

    // ==================== All values valid ====================

    @Test
    @DisplayName("Every resource should have a non-null key, description, and category")
    void testAllResourcesValid() {
        for (Resource resource : Resource.values()) {
            assertNotNull(resource.getKey(), resource.name() + " has null key");
            assertNotNull(resource.getDescription(), resource.name() + " has null description");
            assertNotNull(resource.getCategoryEnum(), resource.name() + " has null category");
            assertFalse(resource.getKey().isBlank(), resource.name() + " has blank key");
        }
    }
}
