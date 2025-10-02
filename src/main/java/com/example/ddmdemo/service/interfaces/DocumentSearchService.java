package com.example.ddmdemo.service.interfaces;

import com.example.ddmdemo.dto.DocumentSearchRequest;
import com.example.ddmdemo.dto.SecurityIncidentReportResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface DocumentSearchService {
    List<SecurityIncidentReportResponse> search(DocumentSearchRequest keywords, String searchType);
}
