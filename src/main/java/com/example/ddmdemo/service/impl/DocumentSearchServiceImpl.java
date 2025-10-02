package com.example.ddmdemo.service.impl;

import com.example.ddmdemo.dto.DocumentSearchRequest;
import com.example.ddmdemo.dto.SecurityIncidentReportResponse;
import com.example.ddmdemo.model.SecurityIncidentReport;
import com.example.ddmdemo.model.enums.SeverityLevel;
import com.example.ddmdemo.modelIndex.SecurityIncidentReportIndex;
import com.example.ddmdemo.respository.SecurityIncidentReportRepository;
import com.example.ddmdemo.service.interfaces.DocumentSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.util.*;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import org.elasticsearch.common.unit.Fuzziness;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentSearchServiceImpl implements DocumentSearchService {
    private final ElasticsearchOperations elasticsearchOperations;
    private final SecurityIncidentReportRepository securityIncidentReportRepository;

    @Override
    public List<SecurityIncidentReportResponse> search(DocumentSearchRequest keywords, String searchType) {
        List<HighlightField> highlightFields = new ArrayList<>();

        highlightFields.add(new HighlightField("employee_name"));
        highlightFields.add(new HighlightField("security_organization_name"));
        highlightFields.add(new HighlightField("affected_organization_name"));
        highlightFields.add(new HighlightField("severity"));
        highlightFields.add(new HighlightField("content"));

        HighlightParameters params = HighlightParameters.builder()
                .withPreTags("<em class=\"highlight\">")
                .withPostTags("</em>")
                .withRequireFieldMatch(false)
                .build();

        NativeQueryBuilder searchQueryBuilder = new NativeQueryBuilder()
                .withQuery(buildSimpleSearchQuery(keywords.searchKeywords(), searchType))
                .withHighlightQuery(new HighlightQuery(new Highlight(params, highlightFields), SecurityIncidentReportIndex.class));

        return runQuery(searchQueryBuilder.build());
    }

    private Query buildSimpleSearchQuery(List<String> tokens, String typeOfSearch){
        switch(typeOfSearch){
            case "simple":
                return BoolQuery.of(q -> q.should(mb -> mb.bool(b -> {
                            tokens.forEach(token -> {
                                b.should(sb -> sb.match(m -> m.field("employee_name").fuzziness(Fuzziness.AUTO.asString()).query(token)));
                                b.should(sb -> sb.term(m -> m.field("severity").value(token.toUpperCase())));
                                b.should(sb -> sb.match(m -> m.field("security_organization_name").fuzziness(Fuzziness.AUTO.asString()).query(token)));
                                b.should(sb -> sb.match(m -> m.field("attacked_organization_name").fuzziness(Fuzziness.AUTO.asString()).query(token)));
                                b.should(sb -> sb.match(m -> m.field("content").fuzziness(Fuzziness.AUTO.asString()).query(token)));
                            });
                            return b;
                        })
                ))._toQuery();
            default:
                return null;
        }
    }

    private List<SecurityIncidentReportResponse> runQuery(NativeQuery searchQuery) {
        var searchHits = elasticsearchOperations.search(
                searchQuery,
                SecurityIncidentReportIndex.class,
                IndexCoordinates.of("security_incident_report_index")
        );

        List<SecurityIncidentReportResponse> result = new ArrayList<>();

        for (var hit : searchHits) {
            SecurityIncidentReportIndex entity = hit.getContent();

            Optional<SecurityIncidentReport> incidentReport =
                    securityIncidentReportRepository.findById(Integer.valueOf(entity.getDatabaseId()));

            if (incidentReport.isEmpty()) continue;

            SecurityIncidentReport reportEntity = incidentReport.get();

            SecurityIncidentReportResponse dto = SecurityIncidentReportResponse.builder()
                    .withEmployeeName(entity.getEmployeeName())
                    .withSecurityOrganizationName(entity.getSecurityOrganizationName())
                    .withAffectedOrganizationName(entity.getAffectedOrganizationName())
                    .withSeverityLevel(SeverityLevel.valueOf(entity.getSeverity()))
                    .withAffectedOrganizationAddress(reportEntity.getAffectedOrganizationAddress())
                    .withReportContent(entity.getContent())
                    .build();

            var highlights = hit.getHighlightFields();
            if (highlights != null && !highlights.isEmpty()) {
                dto = replaceValueForField(dto, highlights);
            }

            result.add(dto);
        }

        return result;
    }

    private SecurityIncidentReportResponse replaceValueForField(
            SecurityIncidentReportResponse dto,
            Map<String, List<String>> highlights) {

        String employeeName = dto.employeeName();
        String securityOrganizationName = dto.securityOrganizationName();
        String affectedOrganizationName = dto.affectedOrganizationName();
        SeverityLevel severityLevel = dto.severityLevel();
        String affectedOrganizationAddress = dto.affectedOrganizationAddress();
        String reportContent = dto.reportContent();

        for (var entry : highlights.entrySet()) {
            String value = String.join(" ", entry.getValue());

            switch (entry.getKey()) {
                case "employeeName":
                    employeeName = value;
                    break;
                case "securityOrganizationName":
                    securityOrganizationName = value;
                    break;
                case "affectedOrganizationName":
                    affectedOrganizationName = value;
                    break;
                case "content":
                    reportContent = value;
                    break;
                case "severityLevel":
                    try {
                        severityLevel = SeverityLevel.valueOf(value.toUpperCase());
                    } catch (Exception e) {
                        log.warn("Invalid severity highlight: {}", value);
                    }
                    break;
                default:
                    break;
            }
        }

        return new SecurityIncidentReportResponse(
                employeeName,
                securityOrganizationName,
                affectedOrganizationName,
                severityLevel,
                affectedOrganizationAddress,
                reportContent
        );
    }
}
