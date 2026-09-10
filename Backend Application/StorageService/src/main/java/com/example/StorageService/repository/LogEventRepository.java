package com.example.StorageService.repository;


import com.example.StorageService.model.LogEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogEventRepository extends MongoRepository<LogEvent, String> {
}