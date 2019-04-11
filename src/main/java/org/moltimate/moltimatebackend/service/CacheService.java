package org.moltimate.moltimatebackend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;
import org.moltimate.moltimatebackend.dto.Alignment.QueryAlignmentResponse;
import org.moltimate.moltimatebackend.repository.FailedAlignmentRespository;
import org.moltimate.moltimatebackend.repository.QueryAlignmentResponseRepository;
import org.moltimate.moltimatebackend.repository.QueryResponsetDataRepository;
import org.moltimate.moltimatebackend.repository.SuccessfulAlignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class CacheService {
    @Autowired
    private QueryAlignmentResponseRepository queryAlignmentResponseRepository;

    @Autowired
    private QueryResponsetDataRepository queryResponsetDataRepository;

    @Autowired
    private SuccessfulAlignmentRepository successfulAlignmentRepository;

    @Autowired
    private FailedAlignmentRespository failedAlignmentRespository;

    public Cache<String, QueryAlignmentResponse> cache = Caffeine.newBuilder()
            .maximumSize(1)
            .expireAfterWrite(Duration.ofDays(1))
            .removalListener((String key, QueryAlignmentResponse value, RemovalCause cause) -> {
                System.out.printf("Key %s was removed from cache\n", key);
                assert value != null;
                saveQueryAlignmentResponse(value);
            })
            .build();

    private void saveQueryAlignmentResponse(QueryAlignmentResponse queryAlignmentResponse) {
        queryAlignmentResponse.getEntries()
                .forEach(entry -> {
                    entry.getAlignments()
                            .forEach(success -> successfulAlignmentRepository.save(success));
                    entry.getFailedAlignments()
                            .forEach(failedAlignment -> failedAlignmentRespository.save(failedAlignment));
                    queryResponsetDataRepository.save(entry);
                });
        queryAlignmentResponseRepository.save(queryAlignmentResponse);
    }

    public QueryAlignmentResponse findQueryAlignmentResponse(String cacheKey) {
        return queryAlignmentResponseRepository.findByCacheKey(cacheKey);
    }
}