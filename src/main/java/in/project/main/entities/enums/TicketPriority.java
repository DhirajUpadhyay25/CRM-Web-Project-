package in.project.main.entities.enums;

public enum TicketPriority {
    LOW("Low", "bg-slate-500/10 text-slate-400 border-slate-500/20"),
    MEDIUM("Medium", "bg-blue-500/10 text-blue-400 border-blue-500/20"),
    HIGH("High", "bg-amber-500/10 text-amber-400 border-amber-500/20"),
    URGENT("Urgent", "bg-rose-500/10 text-rose-400 border-rose-500/20");

    private final String displayName;
    private final String badgeClass;

    TicketPriority(String displayName, String badgeClass) {
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
