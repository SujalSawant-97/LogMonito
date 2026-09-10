package com.example.StorageService.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.example.StorageService.model.MetricEvent;

@Repository
public interface MetricEventRepository extends MongoRepository<MetricEvent, String> {
}
