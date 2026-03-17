package com.propertize.platform.auth.rbac.engine;

import com.propertize.platform.auth.rbac.enums.ScopeEnum;
import lombok.Getter;

/**
 * Centralized registry of all actions in the system.
 * Maps to actions defined in rbac.yml resources.
 *
 * @version 2.0 - Centralized in auth-service
 */
@Getter
public enum Action {

    // BASIC CRUD
    CREATE("create", "Create new resource", ScopeEnum.WRITE),
    READ("read", "Read/view resource", ScopeEnum.READ),
    UPDATE("update", "Update existing resource", ScopeEnum.WRITE),
    DELETE("delete", "Delete resource", ScopeEnum.WRITE),

    // MANAGEMENT
    MANAGE("manage", "Full management access", ScopeEnum.ADMIN),
    LIST("list", "List resources", ScopeEnum.READ),
    EXPORT("export", "Export data", ScopeEnum.READ),
    TRACK("track", "Track/monitor", ScopeEnum.READ),
    ARCHIVE("archive", "Archive resource", ScopeEnum.WRITE),
    TRANSFER("transfer", "Transfer ownership", ScopeEnum.ADMIN),

    // WORKFLOW
    SUBMIT("submit", "Submit for review", ScopeEnum.WRITE),
    APPROVE("approve", "Approve request", ScopeEnum.WORKFLOW),
    REJECT("reject", "Reject request", ScopeEnum.WORKFLOW),
    REVIEW("review", "Review resource", ScopeEnum.WORKFLOW),
    ASSIGN("assign", "Assign to user", ScopeEnum.WORKFLOW),
    COMPLETE("complete", "Mark complete", ScopeEnum.WORKFLOW),
    SCHEDULE("schedule", "Schedule task", ScopeEnum.WORKFLOW),
    TERMINATE("terminate", "Terminate", ScopeEnum.ADMIN),
    RENEW("renew", "Renew", ScopeEnum.WORKFLOW),

    // ACCESS CONTROL
    ACTIVATE("activate", "Activate", ScopeEnum.ADMIN),
    SUSPEND("suspend", "Suspend", ScopeEnum.ADMIN),
    INVITE("invite", "Invite user", ScopeEnum.ADMIN),

    // FINANCIAL
    PROCESS("process", "Process transaction", ScopeEnum.FINANCIAL),
    REFUND("refund", "Issue refund", ScopeEnum.FINANCIAL),
    RECONCILE("reconcile", "Reconcile accounts", ScopeEnum.FINANCIAL),
    PAY("pay", "Make payment", ScopeEnum.FINANCIAL),

    // COMMUNICATION
    SEND("send", "Send message", ScopeEnum.WRITE),
    SHARE("share", "Share resource", ScopeEnum.WRITE),
    COMMUNICATE("communicate", "Communicate", ScopeEnum.WRITE),
    UPLOAD("upload", "Upload file", ScopeEnum.WRITE),
    DOWNLOAD("download", "Download file", ScopeEnum.READ),
    SIGN("sign", "Sign document", ScopeEnum.WRITE),

    // SYSTEM
    CONFIGURE("configure", "Configure settings", ScopeEnum.ADMIN),
    MONITOR("monitor", "Monitor system", ScopeEnum.ADMIN),
    BACKUP("backup", "Backup data", ScopeEnum.ADMIN),
    RESTORE("restore", "Restore data", ScopeEnum.ADMIN),
    TROUBLESHOOT("troubleshoot", "Troubleshoot", ScopeEnum.ADMIN),
    ANALYZE("analyze", "Analyze data", ScopeEnum.READ),

    // SPECIAL
    SCREEN("screen", "Background screening", ScopeEnum.WORKFLOW),
    RATE("rate", "Rate/review", ScopeEnum.WRITE),
    CONTRACT("contract", "Create contract", ScopeEnum.WORKFLOW),
    DEPRECIATE("depreciate", "Calculate depreciation", ScopeEnum.FINANCIAL),
    CUSTOMIZE("customize", "Customize", ScopeEnum.WRITE),
    GENERATE("generate", "Generate report", ScopeEnum.READ),

    // DASHBOARD-SPECIFIC
    READ_PLATFORM_DASHBOARD("read_platform_dashboard", "View platform-wide dashboard", ScopeEnum.DASHBOARD),
    READ_PORTFOLIO_DASHBOARD("read_portfolio_dashboard", "View property portfolio dashboard", ScopeEnum.DASHBOARD),
    READ_FINANCIAL_DASHBOARD("read_financial_dashboard", "View financial analytics dashboard", ScopeEnum.DASHBOARD),
    READ_MAINTENANCE_DASHBOARD("read_maintenance_dashboard", "View maintenance operations dashboard",
            ScopeEnum.DASHBOARD),
    READ_OVERSIGHT_DASHBOARD("read_oversight_dashboard", "View team oversight dashboard", ScopeEnum.DASHBOARD),
    READ_LEASING_DASHBOARD("read_leasing_dashboard", "View leasing activities dashboard", ScopeEnum.DASHBOARD),
    READ_MANAGER("read_manager", "View Manager dashboard", ScopeEnum.DASHBOARD),

    // GLOBAL (superadmin)
    LIST_ALL("list_all", "List all (cross-org)", ScopeEnum.ADMIN),
    VIEW_ALL("view_all", "View all (cross-org)", ScopeEnum.ADMIN),
    EXPORT_ALL("export_all", "Export all data", ScopeEnum.ADMIN),
    TERMINATE_ALL("terminate_all", "Terminate all sessions", ScopeEnum.ADMIN);

    private final String key;
    private final String description;
    private final ScopeEnum scopeEnum;

    Action(String key, String description, ScopeEnum scopeEnum) {
        this.key = key;
        this.description = description;
        this.scopeEnum = scopeEnum;
    }

    public static Action fromKey(String key) {
        for (Action action : values()) {
            if (action.key.equals(key)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown action key: " + key);
    }

    public static boolean exists(String key) {
        for (Action action : values()) {
            if (action.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean isReadOnly() {
        return scopeEnum == ScopeEnum.READ || scopeEnum == ScopeEnum.DASHBOARD;
    }

    public boolean isWrite() {
        return scopeEnum == ScopeEnum.WRITE || scopeEnum == ScopeEnum.ADMIN;
    }

    public boolean isAdmin() {
        return scopeEnum == ScopeEnum.ADMIN;
    }

    @Override
    public String toString() {
        return key;
    }
}
