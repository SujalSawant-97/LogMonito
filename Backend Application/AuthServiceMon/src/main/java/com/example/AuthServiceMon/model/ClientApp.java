package com.example.AuthServiceMon.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "client_applications")
public class ClientApp {

    @Id
    private String id;

    // The ID of the developer who owns this app on your platform
    private String userId;

    private String appName;

    // The secret key the SDK sends in the headers. We index it for lightning-fast lookups.
    @Indexed(unique = true)
    private String apiKey;

    private Instant createdAt;

    // A kill-switch. If a user stops paying for your SaaS, you set this to false.
    private boolean isActive = true;
}
