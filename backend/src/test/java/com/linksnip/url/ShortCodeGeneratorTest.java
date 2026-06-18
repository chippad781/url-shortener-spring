package com.linksnip.url;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortCodeGeneratorTest {

    @Mock UrlRepository urlRepository;

    @Test
    void generatesCodeOfConfiguredLengthFromAlphabet() {
        when(urlRepository.existsByShortCode(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        var generator = new ShortCodeGenerator(urlRepository, 7, 5);

        String code = generator.generateUnique();

        assertThat(code).hasSize(7);
        assertThat(code).matches("[A-Za-z0-9]+");
    }

    @Test
    void retriesOnCollisionThenSucceeds() {
        var generator = new ShortCodeGenerator(urlRepository, 7, 5);
        // first candidate "taken", subsequent free
        when(urlRepository.existsByShortCode(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true)
                .thenReturn(false);

        String code = generator.generateUnique();

        assertThat(code).hasSize(7);
    }

    @Test
    void aliasValidation() {
        var generator = new ShortCodeGenerator(urlRepository, 7, 5);
        assertThat(generator.isValidCustomAlias("abc")).isTrue();
        assertThat(generator.isValidCustomAlias("Valid123")).isTrue();
        assertThat(generator.isValidCustomAlias("no")).isFalse();        // too short
        assertThat(generator.isValidCustomAlias("has space")).isFalse(); // invalid char
        assertThat(generator.isValidCustomAlias("with-dash")).isFalse(); // invalid char
    }
}
