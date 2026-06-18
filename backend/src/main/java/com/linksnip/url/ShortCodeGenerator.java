package com.linksnip.url;

import com.linksnip.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates short codes from a 62-char alphabet using a CSPRNG.
 * At length 7 the keyspace is 62^7 ≈ 3.5e12, so collisions are vanishingly
 * rare; we still retry on the off chance, bounded by max-retries.
 */
@Component
public class ShortCodeGenerator {

    private static final char[] ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private final SecureRandom random = new SecureRandom();
    private final UrlRepository urlRepository;
    private final int length;
    private final int maxRetries;

    public ShortCodeGenerator(UrlRepository urlRepository,
                              @Value("${app.shortcode.length}") int length,
                              @Value("${app.shortcode.max-retries}") int maxRetries) {
        this.urlRepository = urlRepository;
        this.length = length;
        this.maxRetries = maxRetries;
    }

    public String generateUnique() {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            String candidate = random(length);
            if (!urlRepository.existsByShortCode(candidate)) {
                return candidate;
            }
        }
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "Could not allocate a unique short code; please retry");
    }

    private String random(int len) {
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            buf[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(buf);
    }

    public boolean isValidCustomAlias(String alias) {
        if (alias == null || alias.length() < 3 || alias.length() > 16) {
            return false;
        }
        for (int i = 0; i < alias.length(); i++) {
            char c = alias.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
