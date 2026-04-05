package com.trimurl.repository;

import com.trimurl.model.UrlDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

/**
 * Repository for URL documents.
 */
public interface UrlRepository extends MongoRepository<UrlDocument, String> {
    Optional<UrlDocument> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
    List<UrlDocument> findByUserId(String userId);

    @Query(sort = "{ 'totalClicks' : -1 }")
    List<UrlDocument> findTop5ByUserIdOrderByTotalClicksDesc(String userId);

    long countByUserId(String userId);
}