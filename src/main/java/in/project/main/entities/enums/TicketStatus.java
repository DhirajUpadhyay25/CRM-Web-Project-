package in.project.main.entities.enums;

public enum TicketStatus {
    OPEN("Open", "bg-amber-500/10 text-amber-500 border-amber-500/20"),
    IN_PROGRESS("In Progress", "bg-blue-500/10 text-blue-500 border-blue-500/20"),
    WAITING_FOR_USER("Waiting for User", "bg-purple-500/10 text-purple-500 border-purple-500/20"),
    RESOLVED("Resolved", "bg-emerald-500/10 text-emerald-500 border-emerald-500/20"),
    CLOSED("Closed", "bg-slate-500/10 text-slate-400 border-slate-500/20");

    private final String displayName;
    private final String badgeClass;

    TicketStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
