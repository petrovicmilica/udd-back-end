package com.example.ddmdemo.service.impl;

import co.elastic.clients.elasticsearch._types.KnnQuery;
import com.example.ddmdemo.util.VectorizationUtil;
import com.example.ddmdemo.dto.DocumentSearchRequest;
import com.example.ddmdemo.dto.SecurityIncidentReportResponse;
import com.example.ddmdemo.model.SecurityIncidentReport;
import com.example.ddmdemo.model.enums.SeverityLevel;
import com.example.ddmdemo.modelIndex.SecurityIncidentReportIndex;
import com.example.ddmdemo.respository.SecurityIncidentReportRepository;
import com.example.ddmdemo.service.interfaces.DocumentSearchService;
import com.example.ddmdemo.service.interfaces.GeocodingService;
import joptsimple.internal.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;

import java.util.Optional;
import java.util.UUID;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import org.elasticsearch.common.unit.Fuzziness;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentSearchServiceImpl implements DocumentSearchService {
    private final ElasticsearchOperations elasticsearchOperations;
    private final SecurityIncidentReportRepository securityIncidentReportRepository;
    private final GeocodingService geocodingService;

    @Value("${ors.api.key}")
    private String orsApiKey;

    private static final String ORS_PROFILE = "foot-walking";

    private static final Set<String> KEYWORD_FIELDS = Set.of(
            "severity"
    );

    private static final List<String> TEXT_FIELDS = List.of(
            "employee_name",
            "security_organization_name",
            "affected_organization_name",
            "content"
    );

    private static final List<String> SEARCH_FIELDS = List.of(
            "employee_full_name^2",
            "security_organization_name",
            "affected_organization_name",
            "content",
            "severity"
    );

    @Override
    public List<SecurityIncidentReportResponse> search(DocumentSearchRequest keywords, String searchType) {
        if ("geolocation".equals(searchType)) {
            return geoSearch(keywords.searchKeywords(), keywords.radius() != null ? keywords.radius() : 0);
        }
        if ("knn".equals(searchType)) {
            return knnSearch(keywords.searchKeywords());
        }
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
                .withQuery(buildSimpleSearchQuery(keywords.searchKeywords(), keywords.booleanQuery(), keywords.radius(), searchType))
                .withHighlightQuery(new HighlightQuery(new Highlight(params, highlightFields), SecurityIncidentReportIndex.class));

        return runQuery(searchQueryBuilder.build());
    }

    public List<SecurityIncidentReportResponse> knnSearch(List<String> keywords) {
        try {
            String text = Strings.join(keywords, " ");

            float[] embedding = VectorizationUtil.getEmbedding(text);

            List<Float> vectorList = new ArrayList<>();
            for (float f : embedding) {
                vectorList.add(f);
            }

            KnnQuery knnQuery = new KnnQuery.Builder()
                    .field("vectorizedContent")
                    .queryVector(vectorList)
                    .numCandidates(100)
                    .k(10)
                    .boost(10.0f)
                    .build();

            NativeQuery searchQuery = NativeQuery.builder()
                    .withKnnQuery(knnQuery)
                    .withMaxResults(5)
                    .withSearchType(null)
                    .build();

            var searchHits = elasticsearchOperations.search(searchQuery, SecurityIncidentReportIndex.class);

            List<SecurityIncidentReportResponse> dtos = new ArrayList<>();
            for (var hit : searchHits) {
                SecurityIncidentReportIndex entity = hit.getContent();
                Optional<SecurityIncidentReport> incidentOpt = securityIncidentReportRepository.findById(Integer.valueOf(entity.getDatabaseId()));
                if (incidentOpt.isPresent()) {
                    SecurityIncidentReport incident = incidentOpt.get();

                    SecurityIncidentReportResponse dto = SecurityIncidentReportResponse.builder()
                            .withEmployeeName(entity.getEmployeeName())
                            .withSecurityOrganizationName(entity.getSecurityOrganizationName())
                            .withAffectedOrganizationName(entity.getAffectedOrganizationName())
                            .withSeverityLevel(SeverityLevel.valueOf(entity.getSeverity()))
                            .withAffectedOrganizationAddress(incident.getAffectedOrganizationAddress())
                            .withReportContent(entity.getContent())
                            .build();

                    dtos.add(dto);
                }
            }

            return dtos;

        } catch (Exception e) {
            log.error("KNN search failed", e);
            return List.of();
        }
    }


    private Query buildSimpleSearchQuery(List<String> tokens, String booleanQuery, Integer radius, String typeOfSearch){
        String trimmed = booleanQuery != null ? booleanQuery.trim() : "";
        boolean hasRaw = !trimmed.isBlank();
        boolean hasTokens = tokens != null && !tokens.isEmpty();
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
            case "boolean":
                if (!hasRaw && !hasTokens) {
                    return QueryBuilders.matchAll(m -> m);
                }
                String raw = hasRaw ? trimmed : String.join(" ", tokens);

                BooleanQueryParser dsl = new BooleanQueryParser(KEYWORD_FIELDS, TEXT_FIELDS, SEARCH_FIELDS);

                return dsl.parseToQuery(raw);
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

    public List<SecurityIncidentReportResponse> geoSearch(List<String> keywords, int radiusMeters) {
        final String location = String.join(" ", keywords);
        System.out.println("[geoSearch] location=\"{}\"" + location);

        try {
            double[] geoPoint = geocodingService.getCoordinates(location);
            double srcLat = geoPoint[0];
            double srcLon = geoPoint[1];
            System.out.println("[geoSearch] Start coordinates lat=" + srcLat + " lon=" + srcLon);

            NativeQuery query = NativeQuery.builder()
                    .withQuery(Query.of(q -> q.matchAll(m -> m)))
                    .withMaxResults(1000)
                    .build();

            var searchHits = elasticsearchOperations.search(query, SecurityIncidentReportIndex.class,
                    IndexCoordinates.of("security_incident_report_index"));
            System.out.println("search hits: " + searchHits.getSearchHits().size());

            List<SecurityIncidentReportResponse> dtos = new ArrayList<>();

            for (var hit : searchHits) {
                SecurityIncidentReportIndex incident = hit.getContent();

                try {
                    String[] latLon = incident.getLocation().split(",");
                    double dstLat = Double.parseDouble(latLon[0].trim());
                    double dstLon = Double.parseDouble(latLon[1].trim());
                    System.out.println("indeks neki, koordinate: " + dstLat + ", " + dstLon);

                    double distanceMeters = getNetworkDistanceMeters(srcLat, srcLon, dstLat, dstLon);
                    System.out.println("distanca u metrima za taj neki indeks: " + distanceMeters);
                    if (distanceMeters <= radiusMeters) {
                        Optional<SecurityIncidentReport> incidentReportOpt =
                                securityIncidentReportRepository.findById(Integer.valueOf(incident.getDatabaseId()));

                        if (incidentReportOpt.isPresent()) {
                            SecurityIncidentReport reportEntity = incidentReportOpt.get();
                            SecurityIncidentReportResponse dto = SecurityIncidentReportResponse.builder()
                                    .withEmployeeName(incident.getEmployeeName())
                                    .withSecurityOrganizationName(incident.getSecurityOrganizationName())
                                    .withAffectedOrganizationName(incident.getAffectedOrganizationName())
                                    .withSeverityLevel(SeverityLevel.valueOf(incident.getSeverity()))
                                    .withAffectedOrganizationAddress("<em class=\"highlight\">" + reportEntity.getAffectedOrganizationAddress() + "</em>")
                                    .withReportContent(incident.getContent())
                                    .build();
                            dtos.add(dto);
                        }
                    }
                } catch (Exception e) {
                    log.error("[geoSearch] Error processing doc {}: {}", incident.getDatabaseId(), e.getMessage());
                }
            }

            log.info("[geoSearch] Matched {} documents within {} meters", dtos.size(), radiusMeters);
            return dtos;

        } catch (Exception e) {
            log.error("[geoSearch] ERROR: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private double getNetworkDistanceMeters(double srcLat, double srcLon, double dstLat, double dstLon) throws Exception {
        String url = String.format(
                "https://api.openrouteservice.org/v2/directions/%s?api_key=%s&start=%f,%f&end=%f,%f",
                ORS_PROFILE, orsApiKey, srcLon, srcLat, dstLon, dstLat
        );

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/geo+json, application/json")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("ORS directions failed: HTTP " + response.statusCode());
        }

        JSONObject json = new JSONObject(response.body());
        double distance = json.getJSONArray("features")
                .getJSONObject(0)
                .getJSONObject("properties")
                .getJSONArray("segments")
                .getJSONObject(0)
                .getDouble("distance");

        return distance;
    }
}
