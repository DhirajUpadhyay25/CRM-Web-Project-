package in.project.main.audit;

public class AuditRequestContextHolder {

    public static class AuditContext {
        private String requestId;
        private String clientIp;
        private String userAgent;
        private String sessionId;
        private String actorEmail;
        private String actorName;
        private String actorRole;

        public AuditContext() {}

        public AuditContext(String requestId, String clientIp, String userAgent, String sessionId) {
            this.requestId = requestId;
            this.clientIp = clientIp;
            this.userAgent = userAgent;
            this.sessionId = sessionId;
        }

        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getActorEmail() { return actorEmail; }
        public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }
        public String getActorName() { return actorName; }
        public void setActorName(String actorName) { this.actorName = actorName; }
        public String getActorRole() { return actorRole; }
        public void setActorRole(String actorRole) { this.actorRole = actorRole; }
    }

    private static final ThreadLocal<AuditContext> contextHolder = new ThreadLocal<>();

    public static void setContext(AuditContext context) {
        contextHolder.set(context);
    }

    public static AuditContext getContext() {
        AuditContext ctx = contextHolder.get();
        if (ctx == null) {
            ctx = new AuditContext();
            contextHolder.set(ctx);
        }
        return ctx;
    }

    public static String getRequestId() {
        return getContext().getRequestId();
    }

    public static String getClientIp() {
        return getContext().getClientIp();
    }

    public static String getUserAgent() {
        return getContext().getUserAgent();
    }

    public static void clear() {
        contextHolder.remove();
    }
}
