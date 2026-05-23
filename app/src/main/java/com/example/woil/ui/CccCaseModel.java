
package com.example.woil.ui;

public class CccCaseModel {
    private final String caseId;
    private final String type;
    private final String title;
    private final String source;
    private final String severity;
    private final String status;
    private final String subtitle;
    private final String createdText;
    private final String linkedId;

    public CccCaseModel(String caseId, String type, String title, String source, String severity,
                        String status, String subtitle, String createdText, String linkedId) {
        this.caseId = caseId;
        this.type = type;
        this.title = title;
        this.source = source;
        this.severity = severity;
        this.status = status;
        this.subtitle = subtitle;
        this.createdText = createdText;
        this.linkedId = linkedId;
    }

    public String getCaseId() { return caseId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getSource() { return source; }
    public String getSeverity() { return severity; }
    public String getStatus() { return status; }
    public String getSubtitle() { return subtitle; }
    public String getCreatedText() { return createdText; }
    public String getLinkedId() { return linkedId; }
}
