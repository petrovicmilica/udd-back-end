package com.example.ddmdemo.service.interfaces;

import com.example.ddmdemo.dto.SecurityIncidentReportRequest;
import com.example.ddmdemo.dto.SecurityIncidentReportResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface SecurityIncidentReportService {
    SecurityIncidentReportResponse parsePdf(MultipartFile file);
    SecurityIncidentReportResponse confirmAndSave(MultipartFile file, SecurityIncidentReportRequest metadata);
}
