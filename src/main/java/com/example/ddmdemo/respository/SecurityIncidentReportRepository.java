package com.example.ddmdemo.respository;

import com.example.ddmdemo.model.SecurityIncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityIncidentReportRepository extends JpaRepository<SecurityIncidentReport, Integer> {
}
