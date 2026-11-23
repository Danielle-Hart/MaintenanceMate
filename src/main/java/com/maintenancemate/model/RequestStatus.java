package com.maintenancemate.model;

/**
 * Lifecycle states for a maintenance request.
 * Stored in the database by name (see {@link MaintenanceRequest#getStatus()}).
 */
public enum RequestStatus {

    OPEN("Open", "bg-secondary"),
    IN_PROGRESS("In Progress", "bg-warning text-dark"),
    COMPLETED("Completed", "bg-success");

    private final String label;
    private final String badgeClass;

    RequestStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    /** Human-readable name shown in the UI. */
    public String getLabel() {
        return label;
    }

    /** Bootstrap badge classes used to color-code the status. */
    public String getBadgeClass() {
        return badgeClass;
    }
}
