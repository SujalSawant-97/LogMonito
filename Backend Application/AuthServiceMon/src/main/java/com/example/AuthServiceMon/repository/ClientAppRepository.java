package com.example.AuthServiceMon.repository;


import com.example.AuthServiceMon.model.ClientApp;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientAppRepository extends MongoRepository<ClientApp, String> {

    // Finds the app by API key, but ONLY if the account is still active
    Optional<ClientApp> findByApiKeyAndIsActiveTrue(String apiKey);
}
