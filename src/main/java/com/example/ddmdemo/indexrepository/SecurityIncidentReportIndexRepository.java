package com.example.ddmdemo.indexrepository;

import com.example.ddmdemo.modelIndex.SecurityIncidentReportIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityIncidentReportIndexRepository extends ElasticsearchRepository<SecurityIncidentReportIndex, String> {
}
