package com.linksnip.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

/**
 * Cache TTLs mirror the original Django design:
 *   - url::{shortCode}            24h   (hottest path — every redirect)
 *   - analytics::{userId}:{urlId}  5m   (eventually consistent is fine)
 *   - topUrls::{userId}           10m
 *
 * Cache names are referenced from @Cacheable in the service layer.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    public static final String CACHE_URL = "url";
    public static final String CACHE_ANALYTICS = "analytics";
    public static final String CACHE_TOP_URLS = "topUrls";

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    /**
     * Dedicated ObjectMapper for cache values. We cache final records
     * (RedirectTarget, AnalyticsResponse, ...) and a List, so we need
     * polymorphic type info written into the JSON; without it the values
     * deserialize back to LinkedHashMap and blow up on a cache hit.
     *
     * Default typing is restricted by an allow-list to our own package and the
     * java.* types we actually store, so it isn't a general gadget sink.
     */
    private ObjectMapper cacheObjectMapper() {
        BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.linksnip.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.lang.")
                .build();

        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTyping(validator, ObjectMapper.DefaultTyping.EVERYTHING,
                        JsonTypeInfo.As.PROPERTY)
                .build();
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisSerializationContext.SerializationPair<Object> json =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(cacheObjectMapper()));

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(json)
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCache = Map.of(
                CACHE_URL, base.entryTtl(Duration.ofHours(24)),
                CACHE_ANALYTICS, base.entryTtl(Duration.ofMinutes(5)),
                CACHE_TOP_URLS, base.entryTtl(Duration.ofMinutes(10))
        );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
