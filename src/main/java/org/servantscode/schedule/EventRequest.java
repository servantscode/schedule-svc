package org.servantscode.schedule;

import java.time.ZonedDateTime;

public class EventRequest {
    public enum RequestStatus { REQUESTED, APPROVED, DENIED };

    private int id;
    private Event event;

    private ZonedDateTime requestDate;
    private ZonedDateTime approvalDate;
    private int approverId;
    private RequestStatus status;

    // ----- Accessors -----
    public ZonedDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(ZonedDateTime requestDate) { this.requestDate = requestDate; }

    public ZonedDateTime getApprovalDate() { return approvalDate; }
    public void setApprovalDate(ZonedDateTime approvalDate) { this.approvalDate = approvalDate; }

    public int getApproverId() { return approverId; }
    public void setApproverId(int approverId) { this.approverId = approverId; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
}