package com.linksnip.url;

import com.linksnip.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Page<ShortUrl> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Optional<ShortUrl> findByIdAndUser(Long id, User user);

    /**
     * Pessimistic row-level lock used when generating short codes, so two
     * concurrent creates can't both grab the same candidate code. The unique
     * constraint is the real guarantee; this just avoids wasted retries.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from ShortUrl u where u.shortCode = :code")
    Optional<ShortUrl> findByShortCodeForUpdate(@Param("code") String code);

    /**
     * Atomic increment so concurrent redirects don't lose counts to a
     * read-modify-write race. Runs on the async click-recording path.
     */
    @Modifying
    @Query("update ShortUrl u set u.clickCount = u.clickCount + 1 where u.id = :id")
    void incrementClickCount(@Param("id") Long id);
}
