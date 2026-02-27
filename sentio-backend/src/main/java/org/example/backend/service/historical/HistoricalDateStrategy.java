package org.example.backend.service.historical;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strategy component for selecting and grouping historical weather target dates.
 * Provides canonical comparison intervals and month-based grouping for API efficiency.
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class HistoricalDateStrategy {

    /**
     * Returns historical comparison dates relative to the current date.
     *
     * @return List of dates: 3 days, 2 weeks, 1 month, 3 months, and 1 year ago
     */
    public List<LocalDate> getHistoricalDates() {
        LocalDate now = LocalDate.now();
        return Arrays.asList(
                now.minusDays(3),
                now.minusWeeks(2),
                now.minusMonths(1),
                now.minusMonths(3),
                now.minusYears(1));
    }

    /**
     * Groups target dates by year-month key to optimize archive API range calls.
     *
     * @param dates Target dates to group
     * @return Map keyed by YYYY-MM containing grouped dates
     */
    public Map<String, List<LocalDate>> groupDatesByMonthYear(List<LocalDate> dates) {
        return dates.stream()
                .collect(Collectors.groupingBy(date -> date.getYear() + "-" + String.format("%02d", date.getMonthValue())));
    }
}