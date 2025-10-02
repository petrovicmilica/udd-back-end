package com.example.ddmdemo.controller;

import com.example.ddmdemo.dto.DocumentSearchRequest;
import com.example.ddmdemo.dto.SecurityIncidentReportRequest;
import com.example.ddmdemo.dto.SecurityIncidentReportResponse;
import com.example.ddmdemo.service.interfaces.DocumentSearchService;
import com.example.ddmdemo.service.interfaces.SecurityIncidentReportService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/security-incident-report")
@RequiredArgsConstructor
public class SecurityIncidentReportController {
    private static final Logger LOGGER = LogManager.getLogger(SecurityIncidentReportController.class);
    private final SecurityIncidentReportService securityIncidentReportService;
    private final DocumentSearchService documentSearchService;

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SecurityIncidentReportResponse> parseDocument(
            @RequestPart("file") MultipartFile file) {
        LOGGER.info("Received request to parse security incident report file");
        SecurityIncidentReportResponse reportResponse = securityIncidentReportService.parsePdf(file);
        LOGGER.info("Security incident report file parsed successfully");
        LOGGER.info("Report data: {}", reportResponse);
        return ResponseEntity.ok(reportResponse);
    }

    @PostMapping(value = "/upload/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SecurityIncidentReportResponse> confirmUpload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") SecurityIncidentReportRequest metadata
    ) {
        LOGGER.info("Confirming upload for incident report...");
        LOGGER.info("Security incident report to upload: {}", metadata);
        SecurityIncidentReportResponse response = securityIncidentReportService.confirmAndSave(file, metadata);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search/{searchType}")
    public ResponseEntity<List<SecurityIncidentReportResponse>> search(@RequestBody DocumentSearchRequest keywords, @PathVariable(name = "searchType") String searchType) {
        LOGGER.info("Received request to search documents with keywords {} and type {}", keywords, searchType);
        List<SecurityIncidentReportResponse> responses = documentSearchService.search(keywords, searchType);
        LOGGER.info("Responses: {}", responses);
        return ResponseEntity.ok(responses);
    }
}
