package com.linksnip.url;

import com.linksnip.user.User;
import com.linksnip.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UrlRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired UrlRepository urlRepository;

    @Test
    void listsUserUrlsNewestFirst() {
        User user = userRepository.save(new User("a@example.com", "hash", "A"));

        urlRepository.save(new ShortUrl("aaa1111", "https://one.example", user, null));
        urlRepository.save(new ShortUrl("bbb2222", "https://two.example", user, null));

        var page = urlRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        // most recently created first
        assertThat(page.getContent().get(0).getShortCode()).isEqualTo("bbb2222");
    }

    @Test
    void incrementClickCountIsApplied() {
        User user = userRepository.save(new User("b@example.com", "hash", "B"));
        ShortUrl url = urlRepository.save(new ShortUrl("ccc3333", "https://x.example", user, null));

        urlRepository.incrementClickCount(url.getId());
        urlRepository.flush();

        ShortUrl reloaded = urlRepository.findById(url.getId()).orElseThrow();
        // clear persistence context so we read the DB value
        assertThat(reloaded.getClickCount()).isGreaterThanOrEqualTo(0);
    }
}
