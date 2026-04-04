package com.trimurl.repository;

import com.trimurl.model.UrlDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for URL documents in MongoDB.
 * Provides CRUD operations and custom query methods.
 */
public interface UrlRepository extends MongoRepository<UrlDocument, String> {
    Optional<UrlDocument> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Query(sort = "{ 'createdAt' : -1 }")
    List<UrlDocument> findTop5ByOrderByClickCountDesc();

    @Query(sort = "{ 'createdAt' : -1 }")
    List<UrlDocument> findTop10ByOrderByCreatedAtDesc();

    long count();
}