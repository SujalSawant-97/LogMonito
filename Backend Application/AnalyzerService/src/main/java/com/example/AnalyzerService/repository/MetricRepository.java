package com.example.AnalyzerService.repository;

import com.example.AnalyzerService.model.MetricDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MetricRepository extends MongoRepository<MetricDocument, String> {

    List<MetricDocument> findByMetadataUserIdAndMetadataServiceNameOrderByTimestampDesc(
            String userId, String serviceName, Pageable pageable);

    List<MetricDocument> findByMetadataUserIdAndMetadataServiceNameAndTimestampAfterOrderByTimestampDesc(
            String userId, String serviceName, Instant timestamp);
}
