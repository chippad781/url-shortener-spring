package com.linksnip.analytics;

import com.linksnip.url.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByUrl(ShortUrl url);

    long countByUrlAndClickedAtAfter(ShortUrl url, Instant since);

    /** Daily click buckets for the time-series chart. */
    @Query("""
            select cast(c.clickedAt as date) as day, count(c) as total
            from ClickEvent c
            where c.url = :url and c.clickedAt >= :since
            group by cast(c.clickedAt as date)
            order by cast(c.clickedAt as date)
            """)
    List<DailyClicks> countDailyClicks(@Param("url") ShortUrl url, @Param("since") Instant since);

    /** Projection used by the daily-buckets query. */
    interface DailyClicks {
        java.time.LocalDate getDay();
        long getTotal();
    }
}
