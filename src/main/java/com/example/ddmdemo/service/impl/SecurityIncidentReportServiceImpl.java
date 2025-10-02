package com.example.ddmdemo.service.impl;

import com.example.ddmdemo.dto.SecurityIncidentReportRequest;
import com.example.ddmdemo.dto.SecurityIncidentReportResponse;
import com.example.ddmdemo.indexrepository.SecurityIncidentReportIndexRepository;
import com.example.ddmdemo.mapper.SecurityIncidentReportMapper;
import com.example.ddmdemo.model.SecurityIncidentReport;
import com.example.ddmdemo.model.enums.SeverityLevel;
import com.example.ddmdemo.modelIndex.SecurityIncidentReportIndex;
import com.example.ddmdemo.respository.SecurityIncidentReportRepository;
import com.example.ddmdemo.service.interfaces.GeocodingService;
import com.example.ddmdemo.service.interfaces.SecurityIncidentReportService;
import com.example.ddmdemo.util.VectorizationUtil;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityIncidentReportServiceImpl implements SecurityIncidentReportService {
    private final SecurityIncidentReportRepository securityIncidentReportRepository;
    private final SecurityIncidentReportIndexRepository securityIncidentReportIndexRepository;
    private final MinioClient minioClient;
    private final GeocodingService geocodingService;

    @Override
    public SecurityIncidentReportResponse parsePdf(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            String employeeName = extractField(text, "Employee:");
            String securityOrg = extractField(text, "Security Org:");
            String affectedOrg = extractField(text, "Affected Org:");
            SeverityLevel severity = extractSeverity(text, "Severity:");
            String affectedAddress = extractField(text, "Address:");
            String reportContent = extractField(text, "Report Content:");

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

    @Override
    @Transactional
    public SecurityIncidentReportResponse confirmAndSave(MultipartFile file,
                                                         SecurityIncidentReportRequest request) {
        try {
            String objectName = "incident-reports/" + UUID.randomUUID() + ".pdf";
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket("security-incidents")
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            SecurityIncidentReport entity = SecurityIncidentReportMapper.toEntity(request);
            entity.setFilePath(objectName);

            SecurityIncidentReport saved = securityIncidentReportRepository.save(entity);

            SecurityIncidentReportIndex index = new SecurityIncidentReportIndex();
            index.setEmployeeName(saved.getEmployeeName());
            index.setSecurityOrganizationName(saved.getSecurityOrganizationName());
            index.setAffectedOrganizationName(saved.getAffectedOrganizationName());
            index.setSeverity(saved.getSeverityLevel().toString());
            index.setDatabaseId(saved.getId().toString());
            index.setContent(saved.getReportContent() != null ? saved.getReportContent() : "");
            double[] coords = geocodingService.getCoordinates(saved.getAffectedOrganizationAddress());
            String geoPoint = coords[0] + "," + coords[1];
            System.out.println("Geolokacija indeksa: " + coords[0] + ", " + coords[1]);
            index.setLocation(geoPoint);
            String address = saved.getAffectedOrganizationAddress();
            String city = address;
            if (address != null && address.contains(",")) {
                String[] parts = address.split(",");
                city = parts[parts.length - 1].trim();
            }
            index.setCity(city);
            index.setVectorizedContent(getVectorizedContent(saved));

            securityIncidentReportIndexRepository.save(index);

            logToFile(saved, saved.getReportContent());

            return SecurityIncidentReportResponse.builder()
                    .withEmployeeName(saved.getEmployeeName())
                    .withSecurityOrganizationName(saved.getSecurityOrganizationName())
                    .withAffectedOrganizationName(saved.getAffectedOrganizationName())
                    .withSeverityLevel(saved.getSeverityLevel())
                    .withAffectedOrganizationAddress(saved.getAffectedOrganizationAddress())
                    .withReportContent(saved.getReportContent())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to confirm upload", e);
        }
    }

    private float[] getVectorizedContent(SecurityIncidentReport entity) {
        StringBuilder fullText = new StringBuilder();

        if (entity.getEmployeeName() != null) {
            fullText.append(entity.getEmployeeName()).append(" ");
        }
        if (entity.getSecurityOrganizationName() != null) {
            fullText.append(entity.getSecurityOrganizationName()).append(" ");
        }
        if (entity.getAffectedOrganizationName() != null) {
            fullText.append(entity.getAffectedOrganizationName()).append(" ");
        }
        if (entity.getSeverityLevel() != null) {
            fullText.append(entity.getSeverityLevel()).append(" ");
        }
        if (entity.getReportContent() != null) {
            fullText.append(entity.getReportContent()).append(" ");
        }
        if (entity.getAffectedOrganizationAddress() != null) {
            fullText.append(entity.getAffectedOrganizationAddress()).append(" ");
        }

        String textForEmbedding = fullText.toString().trim();

        float[] vectorizedContent;
        try {
            return vectorizedContent = VectorizationUtil.getEmbedding(textForEmbedding);
        } catch (Exception e) {
            throw new RuntimeException("Failed to vectorize content", e);
        }
    }

    private void logToFile(SecurityIncidentReport report, String content) {
        String address = "";
        String city = "";
        if (report.getAffectedOrganizationAddress() != null &&
                report.getAffectedOrganizationAddress().contains(",")) {
            String[] parts = report.getAffectedOrganizationAddress().split(",\\s*");
            if (parts.length > 0) address = parts[0];
            if (parts.length > 1) city = parts[1];
        }

        String log = String.format("[%s] SecurityIncidentReport saved: " +
                        "id=%s, employee=%s, securityOrg=%s, affectedOrg=%s, " +
                        "severity=%s, address=%s, city=%s, content=%s, file=%s%n",
                java.time.LocalDateTime.now(),
                report.getId(),
                report.getEmployeeName(),
                report.getSecurityOrganizationName(),
                report.getAffectedOrganizationName(),
                report.getSeverityLevel(),
                address,
                city,
                content != null ? content : "",
                report.getFilePath()
        );

        try {
            String projectRoot = System.getProperty("user.dir");
            Path logDir = Paths.get(projectRoot, "elk", "logstash", "logstash-ingest-data");
            Files.createDirectories(logDir);

            Path logFilePath = logDir.resolve("application.log");
            Files.write(logFilePath, (log + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
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

    private SeverityLevel extractSeverity(String text, String fieldName) {
        String severityValue = extractField(text, fieldName);
        if (severityValue.contains("Low")) return SeverityLevel.LOW;
        if (severityValue.contains("Medium")) return SeverityLevel.MEDIUM;
        if (severityValue.contains("High")) return SeverityLevel.HIGH;
        if (severityValue.contains("Critical")) return SeverityLevel.CRITICAL;
        return SeverityLevel.MEDIUM;
    }
}
