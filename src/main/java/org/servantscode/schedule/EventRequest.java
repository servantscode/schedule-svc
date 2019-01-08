package org.servantscode.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class EventRequest {
    public enum RequestStatus { REQUESTED, APPROVED, DENIED };

    private int id;
    private Event event;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-ddTHH:mm:ss+Z")
    private Date requestDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-ddTHH:mm:ss+Z")
    private Date approvalDate;
    private int approverId;
    private RequestStatus status;

    // ----- Accessors -----
    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date requestDate) { this.requestDate = requestDate; }

    public Date getApprovalDate() { return approvalDate; }
    public void setApprovalDate(Date approvalDate) { this.approvalDate = approvalDate; }

    public int getApproverId() { return approverId; }
    public void setApproverId(int approverId) { this.approverId = approverId; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
}