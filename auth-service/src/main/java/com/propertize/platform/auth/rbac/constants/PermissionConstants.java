package com.propertize.platform.auth.rbac.constants;

/**
 * Permission Constants - RBAC v5.0
 * Centralized permission strings matching rbac.yml configuration.
 * Single source of truth for all services.
 *
 * @version 5.0 - Centralized in auth-service
 */
public final class PermissionConstants {

    private PermissionConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final class User {
        public static final String CREATE = "user:create";
        public static final String READ = "user:read";
        public static final String UPDATE = "user:update";
        public static final String DELETE = "user:delete";
        public static final String LIST = "user:list";
        public static final String LIST_ALL = "user:list_all";
        public static final String INVITE = "user:invite";
        public static final String ACTIVATE = "user:activate";
        public static final String DEACTIVATE = "user:deactivate";
        public static final String RESET_PASSWORD = "user:reset_password";

        private User() {
        }
    }

    public static final class Organization {
        public static final String CREATE = "organization:create";
        public static final String READ = "organization:read";
        public static final String UPDATE = "organization:update";
        public static final String DELETE = "organization:delete";
        public static final String LIST = "organization:list";
        public static final String LIST_ALL = "organization:list_all";
        public static final String MANAGE = "organization:manage";
        public static final String CONFIGURE = "organization:configure";

        private Organization() {
        }
    }

    public static final class Property {
        public static final String CREATE = "property:create";
        public static final String READ = "property:read";
        public static final String UPDATE = "property:update";
        public static final String DELETE = "property:delete";
        public static final String LIST = "property:list";
        public static final String LIST_ALL = "property:list_all";
        public static final String VIEW_ALL = "property:view_all";
        public static final String MANAGE = "property:manage";
        public static final String CONFIGURE = "property:configure";

        private Property() {
        }
    }

    public static final class Lease {
        public static final String CREATE = "lease:create";
        public static final String READ = "lease:read";
        public static final String UPDATE = "lease:update";
        public static final String DELETE = "lease:delete";
        public static final String LIST = "lease:list";
        public static final String LIST_ALL = "lease:list_all";
        public static final String APPROVE = "lease:approve";
        public static final String TERMINATE = "lease:terminate";
        public static final String RENEW = "lease:renew";

        private Lease() {
        }
    }

    public static final class Tenant {
        public static final String CREATE = "tenant:create";
        public static final String READ = "tenant:read";
        public static final String UPDATE = "tenant:update";
        public static final String DELETE = "tenant:delete";
        public static final String LIST = "tenant:list";
        public static final String LIST_ALL = "tenant:list_all";
        public static final String SCREEN = "tenant:screen";
        public static final String APPROVE = "tenant:approve";

        private Tenant() {
        }
    }

    public static final class Payment {
        public static final String CREATE = "payment:create";
        public static final String READ = "payment:read";
        public static final String UPDATE = "payment:update";
        public static final String DELETE = "payment:delete";
        public static final String LIST = "payment:list";
        public static final String LIST_ALL = "payment:list_all";
        public static final String PROCESS = "payment:process";
        public static final String APPROVE = "payment:approve";
        public static final String RECONCILE = "payment:reconcile";
        public static final String REFUND = "payment:refund";

        private Payment() {
        }
    }

    public static final class Invoice {
        public static final String CREATE = "invoice:create";
        public static final String READ = "invoice:read";
        public static final String UPDATE = "invoice:update";
        public static final String DELETE = "invoice:delete";
        public static final String LIST = "invoice:list";
        public static final String SEND = "invoice:send";
        public static final String APPROVE = "invoice:approve";
        public static final String RECONCILE = "invoice:reconcile";

        private Invoice() {
        }
    }

    public static final class Maintenance {
        public static final String CREATE = "maintenance:create";
        public static final String READ = "maintenance:read";
        public static final String UPDATE = "maintenance:update";
        public static final String DELETE = "maintenance:delete";
        public static final String LIST = "maintenance:list";
        public static final String LIST_ALL = "maintenance:list_all";
        public static final String ASSIGN = "maintenance:assign";
        public static final String COMPLETE = "maintenance:complete";
        public static final String APPROVE = "maintenance:approve";
        public static final String SCHEDULE = "maintenance:schedule";

        private Maintenance() {
        }
    }

    public static final class Application {
        public static final String CREATE = "application:create";
        public static final String READ = "application:read";
        public static final String UPDATE = "application:update";
        public static final String DELETE = "application:delete";
        public static final String LIST = "application:list";
        public static final String APPROVE = "application:approve";
        public static final String REJECT = "application:reject";
        public static final String REVIEW = "application:review";

        private Application() {
        }
    }

    public static final class Report {
        public static final String CREATE = "report:create";
        public static final String READ = "report:read";
        public static final String LIST = "report:list";
        public static final String EXPORT = "report:export";
        public static final String EXPORT_ALL = "report:export_all";
        public static final String GENERATE = "report:generate";
        public static final String SCHEDULE = "report:schedule";

        private Report() {
        }
    }

    public static final class Dashboard {
        public static final String READ = "dashboard:read";
        public static final String VIEW = "dashboard:view";
        public static final String READ_PLATFORM_DASHBOARD = "dashboard:read_platform_dashboard";
        public static final String READ_PORTFOLIO_DASHBOARD = "dashboard:read_portfolio_dashboard";
        public static final String READ_PROPERTY_DASHBOARD = "dashboard:read_property_dashboard";
        public static final String READ_MAINTENANCE_DASHBOARD = "dashboard:read_maintenance_dashboard";

        private Dashboard() {
        }
    }

    public static final class Document {
        public static final String CREATE = "document:create";
        public static final String READ = "document:read";
        public static final String UPDATE = "document:update";
        public static final String DELETE = "document:delete";
        public static final String LIST = "document:list";
        public static final String UPLOAD = "document:upload";
        public static final String DOWNLOAD = "document:download";
        public static final String SIGN = "document:sign";

        private Document() {
        }
    }

    public static final class Session {
        public static final String LIST = "session:list";
        public static final String LIST_ALL = "session:list_all";
        public static final String TERMINATE = "session:terminate";
        public static final String TERMINATE_ALL = "session:terminate_all";

        private Session() {
        }
    }

    public static final class System {
        public static final String CONFIGURE = "system:configure";
        public static final String MONITOR = "system:monitor";
        public static final String BACKUP = "system:backup";
        public static final String RESTORE = "system:restore";
        public static final String UPDATE = "system:update";

        private System() {
        }
    }

    public static final class AuditLog {
        public static final String READ = "audit_log:read";
        public static final String EXPORT = "audit_log:export";

        private AuditLog() {
        }
    }

    public static final class Vendor {
        public static final String CREATE = "vendor:create";
        public static final String READ = "vendor:read";
        public static final String UPDATE = "vendor:update";
        public static final String DELETE = "vendor:delete";
        public static final String LIST = "vendor:list";
        public static final String APPROVE = "vendor:approve";

        private Vendor() {
        }
    }

    public static final class Task {
        public static final String CREATE = "task:create";
        public static final String READ = "task:read";
        public static final String UPDATE = "task:update";
        public static final String DELETE = "task:delete";
        public static final String LIST = "task:list";
        public static final String ASSIGN = "task:assign";
        public static final String COMPLETE = "task:complete";

        private Task() {
        }
    }

    public static final class Notification {
        public static final String CREATE = "notification:create";
        public static final String READ = "notification:read";
        public static final String SEND = "notification:send";
        public static final String LIST = "notification:list";
        public static final String CONFIGURE = "notification:configure";

        private Notification() {
        }
    }

    public static final class Screening {
        public static final String CREATE = "screening:create";
        public static final String READ = "screening:read";
        public static final String UPDATE = "screening:update";
        public static final String LIST = "screening:list";
        public static final String APPROVE = "screening:approve";
        public static final String INITIATE = "screening:initiate";

        private Screening() {
        }
    }

    public static final class Communication {
        public static final String SEND = "communication:send";
        public static final String READ = "communication:read";
        public static final String LIST = "communication:list";

        private Communication() {
        }
    }
}
