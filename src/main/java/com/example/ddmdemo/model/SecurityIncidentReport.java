package com.example.ddmdemo.model;

import com.example.ddmdemo.model.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "security_incident_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityIncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String employeeName;

    @Column(nullable = false)
    private String securityOrganizationName;

    @Column(nullable = false)
    private String affectedOrganizationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeverityLevel severityLevel;

    @Column(nullable = false)
    private String affectedOrganizationAddress;

    @Lob
    private String reportContent;
}