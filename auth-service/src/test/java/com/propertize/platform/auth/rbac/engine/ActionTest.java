package com.propertize.platform.auth.rbac.engine;

import com.propertize.platform.auth.rbac.enums.ScopeEnum;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Action enum — validates all 50+ actions,
 * key lookup, existence check, scope classification, and toString.
 */
@DisplayName("Action Enum Tests")
class ActionTest {

    // ==================== fromKey ====================

    @Nested
    @DisplayName("fromKey Tests")
    class FromKeyTests {

        @Test
        @DisplayName("Should resolve basic CRUD actions")
        void testBasicCrud() {
            assertEquals(Action.CREATE, Action.fromKey("create"));
            assertEquals(Action.READ, Action.fromKey("read"));
            assertEquals(Action.UPDATE, Action.fromKey("update"));
            assertEquals(Action.DELETE, Action.fromKey("delete"));
        }

        @Test
        @DisplayName("Should resolve workflow actions")
        void testWorkflowActions() {
            assertEquals(Action.SUBMIT, Action.fromKey("submit"));
            assertEquals(Action.APPROVE, Action.fromKey("approve"));
            assertEquals(Action.REJECT, Action.fromKey("reject"));
            assertEquals(Action.ASSIGN, Action.fromKey("assign"));
        }

        @Test
        @DisplayName("Should resolve financial actions")
        void testFinancialActions() {
            assertEquals(Action.PROCESS, Action.fromKey("process"));
            assertEquals(Action.REFUND, Action.fromKey("refund"));
            assertEquals(Action.PAY, Action.fromKey("pay"));
        }

        @Test
        @DisplayName("Should resolve dashboard-specific actions")
        void testDashboardActions() {
            assertEquals(Action.READ_PLATFORM_DASHBOARD, Action.fromKey("read_platform_dashboard"));
            assertEquals(Action.READ_FINANCIAL_DASHBOARD, Action.fromKey("read_financial_dashboard"));
        }

        @Test
        @DisplayName("Should resolve global admin actions")
        void testGlobalActions() {
            assertEquals(Action.LIST_ALL, Action.fromKey("list_all"));
            assertEquals(Action.VIEW_ALL, Action.fromKey("view_all"));
            assertEquals(Action.TERMINATE_ALL, Action.fromKey("terminate_all"));
        }

        @Test
        @DisplayName("Should throw for unknown action key")
        void testUnknownKey() {
            assertThrows(IllegalArgumentException.class,
                    () -> Action.fromKey("nonexistent"));
        }
    }

    // ==================== exists ====================

    @Test
    @DisplayName("Should return true for known keys")
    void testExistsKnown() {
        assertTrue(Action.exists("create"));
        assertTrue(Action.exists("read"));
        assertTrue(Action.exists("manage"));
        assertTrue(Action.exists("read_platform_dashboard"));
    }

    @Test
    @DisplayName("Should return false for unknown keys")
    void testExistsUnknown() {
        assertFalse(Action.exists("fly"));
        assertFalse(Action.exists(""));
    }

    // ==================== Scope Classification ====================

    @Nested
    @DisplayName("Scope Classification Tests")
    class ScopeTests {

        @Test
        @DisplayName("READ should be read-only")
        void testReadOnly() {
            assertTrue(Action.READ.isReadOnly());
            assertTrue(Action.LIST.isReadOnly());
            assertTrue(Action.EXPORT.isReadOnly());
            assertTrue(Action.ANALYZE.isReadOnly());
            assertTrue(Action.DOWNLOAD.isReadOnly());
            assertFalse(Action.CREATE.isReadOnly());
            assertFalse(Action.DELETE.isReadOnly());
        }

        @Test
        @DisplayName("Dashboard actions should be read-only")
        void testDashboardReadOnly() {
            assertTrue(Action.READ_PLATFORM_DASHBOARD.isReadOnly());
            assertTrue(Action.READ_FINANCIAL_DASHBOARD.isReadOnly());
        }

        @Test
        @DisplayName("WRITE/ADMIN should be write")
        void testIsWrite() {
            assertTrue(Action.CREATE.isWrite());
            assertTrue(Action.UPDATE.isWrite());
            assertTrue(Action.DELETE.isWrite());
            assertTrue(Action.MANAGE.isWrite()); // ADMIN scope → isWrite true
            assertTrue(Action.CONFIGURE.isWrite());
            assertFalse(Action.READ.isWrite());
        }

        @Test
        @DisplayName("ADMIN scope should be admin")
        void testIsAdmin() {
            assertTrue(Action.MANAGE.isAdmin());
            assertTrue(Action.CONFIGURE.isAdmin());
            assertTrue(Action.MONITOR.isAdmin());
            assertTrue(Action.TRANSFER.isAdmin());
            assertFalse(Action.READ.isAdmin());
            assertFalse(Action.CREATE.isAdmin());
        }
    }

    // ==================== Properties ====================

    @Test
    @DisplayName("Should have key, description, and scope")
    void testProperties() {
        Action action = Action.CREATE;
        assertEquals("create", action.getKey());
        assertEquals("Create new resource", action.getDescription());
        assertEquals(ScopeEnum.WRITE, action.getScopeEnum());
    }

    // ==================== toString ====================

    @Test
    @DisplayName("toString should return key")
    void testToString() {
        assertEquals("create", Action.CREATE.toString());
        assertEquals("read_platform_dashboard", Action.READ_PLATFORM_DASHBOARD.toString());
    }

    // ==================== All values have keys ====================

    @Test
    @DisplayName("Every action should have a non-null key, description, and scope")
    void testAllActionsValid() {
        for (Action action : Action.values()) {
            assertNotNull(action.getKey(), action.name() + " has null key");
            assertNotNull(action.getDescription(), action.name() + " has null description");
            assertNotNull(action.getScopeEnum(), action.name() + " has null scope");
            assertFalse(action.getKey().isBlank(), action.name() + " has blank key");
        }
    }
}
