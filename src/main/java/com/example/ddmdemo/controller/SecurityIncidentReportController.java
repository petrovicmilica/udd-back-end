package com.example.ddmdemo.controller;

import com.example.ddmdemo.dto.SecurityIncidentReportResponse;
import com.example.ddmdemo.service.interfaces.SecurityIncidentReportService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/security-incident-report")
@RequiredArgsConstructor
public class SecurityIncidentReportController {
    private static final Logger LOGGER = LogManager.getLogger(SecurityIncidentReportController.class);
    private final SecurityIncidentReportService securityIncidentReportService;

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SecurityIncidentReportResponse> parseDocument(
            @RequestPart("file") MultipartFile file) {
        LOGGER.info("Received request to parse security incident report file");
        SecurityIncidentReportResponse reportResponse = securityIncidentReportService.parsePdf(file);
        LOGGER.info("Security incident report file parsed successfully");
        LOGGER.info("Report data: {}", reportResponse);
        return ResponseEntity.ok(reportResponse);
    }
}
