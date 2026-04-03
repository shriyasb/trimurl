package com.trimurl.repository;

import com.trimurl.model.UrlDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

/**
 * Repository interface for URL documents in MongoDB.
 * Provides CRUD operations and custom query methods.
 */
public interface UrlRepository extends MongoRepository<UrlDocument, String> {
    Optional<UrlDocument> findByShortCode(String shortCode);
}