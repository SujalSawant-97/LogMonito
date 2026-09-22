package com.example.AnalyzerService.repository;

import com.example.AnalyzerService.model.LogDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LogRepository extends MongoRepository<LogDocument, String> {

    List<LogDocument> findByMetadataUserIdAndMetadataServiceNameAndLogLevelOrderByTimestampDesc(
            String userId, String serviceName, String logLevel, Pageable pageable);

    List<LogDocument> findByMetadataUserIdAndMetadataServiceNameAndTimestampAfterOrderByTimestampDesc(
            String userId, String serviceName, Instant timestamp);
}
