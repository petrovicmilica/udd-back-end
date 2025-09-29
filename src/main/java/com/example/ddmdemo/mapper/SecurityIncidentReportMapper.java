package com.example.ddmdemo.mapper;

import com.example.ddmdemo.dto.SecurityIncidentReportResponse;
import com.example.ddmdemo.model.SecurityIncidentReport;
import org.springframework.stereotype.Component;

@Component
public class SecurityIncidentReportMapper {
    public static SecurityIncidentReportResponse toResponse(SecurityIncidentReport entity) {
        return SecurityIncidentReportResponse.builder()
                .withEmployeeName(entity.getEmployeeName())
                .withSecurityOrganizationName(entity.getSecurityOrganizationName())
                .withAffectedOrganizationName(entity.getAffectedOrganizationName())
                .withSeverityLevel(entity.getSeverityLevel())
                .withAffectedOrganizationAddress(entity.getAffectedOrganizationAddress())
                .withReportContent(entity.getReportContent())
                .build();
    }

    public static SecurityIncidentReport toEntity(SecurityIncidentReportResponse dto) {
        return SecurityIncidentReport.builder()
                .employeeName(dto.employeeName())
                .securityOrganizationName(dto.securityOrganizationName())
                .affectedOrganizationName(dto.affectedOrganizationName())
                .severityLevel(dto.severityLevel())
                .affectedOrganizationAddress(dto.affectedOrganizationAddress())
                .reportContent(dto.reportContent())
                .build();
    }
}