package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.AISummary;
import org.example.backend.repository.AISummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

//TODO: implement smarter solution not just "an ai summary"
@Service
@RequiredArgsConstructor
@Slf4j
public class AISummaryService {

    private final AISummaryRepository aiSummaryRepository;

    /**
     * Saves or updates an AI summary (overwrites existing current summary)
     */
    @Transactional
    public AISummary saveAISummary(AISummary aiSummary) {
        log.info("Saving new AI summary");

        // Delete old summaries to keep only the most recent
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        aiSummaryRepository.deleteOldSummaries(cutoff);

        return aiSummaryRepository.save(aiSummary);
    }

    /**
     * Gets the most recent AI summary
     */
    public Optional<AISummary> getCurrentSummary() {
        return aiSummaryRepository.findTopByOrderByTimestampDesc();
    }

    /**
     * Gets recent summaries from the last 24 hours
     */
    public List<AISummary> getRecentSummaries() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return aiSummaryRepository.findByTimestampAfterOrderByTimestampDesc(since);
    }

    /**
     * Cleanup old summaries
     */
    @Transactional
    public void cleanupOldSummaries() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        aiSummaryRepository.deleteOldSummaries(cutoff);
        log.info("Cleaned up AI summaries older than 7 days");
    }
}