package com.example.ddmdemo.dto;

import com.example.ddmdemo.model.enums.SeverityLevel;

public record SecurityIncidentReportResponse(
        String employeeName,
        String securityOrganizationName,
        String affectedOrganizationName,
        SeverityLevel severityLevel,
        String affectedOrganizationAddress,
        String reportContent
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String employeeName;
        private String securityOrganizationName;
        private String affectedOrganizationName;
        private SeverityLevel severityLevel;
        private String affectedOrganizationAddress;
        private String reportContent;

        public Builder withEmployeeName(String employeeName) {
            this.employeeName = employeeName;
            return this;
        }

        public Builder withSecurityOrganizationName(String securityOrganizationName) {
            this.securityOrganizationName = securityOrganizationName;
            return this;
        }

        public Builder withAffectedOrganizationName(String affectedOrganizationName) {
            this.affectedOrganizationName = affectedOrganizationName;
            return this;
        }

        public Builder withSeverityLevel(SeverityLevel severityLevel) {
            this.severityLevel = severityLevel;
            return this;
        }

        public Builder withAffectedOrganizationAddress(String affectedOrganizationAddress) {
            this.affectedOrganizationAddress = affectedOrganizationAddress;
            return this;
        }

        public Builder withReportContent(String reportContent) {
            this.reportContent = reportContent;
            return this;
        }

        public SecurityIncidentReportResponse build() { return new SecurityIncidentReportResponse(employeeName, securityOrganizationName, affectedOrganizationName, severityLevel, affectedOrganizationAddress, reportContent); }
    }
}
