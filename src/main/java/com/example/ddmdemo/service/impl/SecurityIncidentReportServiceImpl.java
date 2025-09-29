package com.example.ddmdemo.service.impl;

import com.example.ddmdemo.dto.SecurityIncidentReportResponse;
import com.example.ddmdemo.model.enums.SeverityLevel;
import com.example.ddmdemo.service.interfaces.SecurityIncidentReportService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
public class SecurityIncidentReportServiceImpl implements SecurityIncidentReportService {
    @Override
    public SecurityIncidentReportResponse parsePdf(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            String employeeName = extractField(text, "Employee:");
            String securityOrg = extractField(text, "Security Org:");
            String affectedOrg = extractField(text, "Affected Org:");
            SeverityLevel severity = extractSeverity(text);
            String affectedAddress = extractField(text, "Address:");
            String reportContent = text;

            return SecurityIncidentReportResponse.builder()
                    .withEmployeeName(employeeName)
                    .withSecurityOrganizationName(securityOrg)
                    .withAffectedOrganizationName(affectedOrg)
                    .withSeverityLevel(severity)
                    .withAffectedOrganizationAddress(affectedAddress)
                    .withReportContent(reportContent)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PDF file", e);
        }
    }

    private String extractField(String text, String fieldName) {
        for (String line : text.split("\\r?\\n")) {
            if (line.startsWith(fieldName)) {
                return line.replace(fieldName, "").trim();
            }
        }
        return "";
    }

    private SeverityLevel extractSeverity(String text) {
        if (text.contains("Low")) return SeverityLevel.LOW;
        if (text.contains("Medium")) return SeverityLevel.MEDIUM;
        if (text.contains("High")) return SeverityLevel.HIGH;
        if (text.contains("Critical")) return SeverityLevel.CRITICAL;
        return SeverityLevel.MEDIUM;
    }
}
