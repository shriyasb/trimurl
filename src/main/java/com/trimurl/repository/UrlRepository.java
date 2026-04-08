package com.trimurl.repository;

import com.trimurl.model.UrlDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface UrlRepository extends MongoRepository<UrlDocument, String> {
    Optional<UrlDocument> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
    List<UrlDocument> findByUserId(String userId);
    List<UrlDocument> findTop5ByUserIdOrderByTotalClicksDesc(String userId);
    long countByUserId(String userId);
    List<UrlDocument> findAllByUserId(String userId);
}
