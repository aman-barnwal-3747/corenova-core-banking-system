package com.corenova.bank.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;
import java.util.Map;

/**
 * ================================================================
 *  CacheConfig – Named Redis Cache Configuration
 *  PDF §15 Advanced Features: Redis Caching
 *
 *  Cache TTL strategy per banking domain:
 *  ┌────────────────────────┬────────────┬───────────────────────┐
 *  │ Cache Name             │ TTL        │ Reason                │
 *  ├────────────────────────┼────────────┼───────────────────────┤
 *  │ accounts               │ 10 minutes │ Balance changes often │
 *  │ customers              │ 30 minutes │ Profile rarely changes│
 *  │ dashboardData          │  5 minutes │ Live KPI refresh      │
 *  │ quickStats             │  2 minutes │ Header bar counters   │
 *  │ dashboardStats         │ 10 minutes │ Aggregate queries     │
 *  │ beneficiaries          │ 60 minutes │ Static reference data │
 *  │ loanPortfolio          │ 15 minutes │ Loan book summary     │
 *  │ auditStats             │ 30 minutes │ Compliance counts     │
 *  └────────────────────────┴────────────┴───────────────────────┘
 *
 *  Serialization: JSON (human-readable in Redis CLI, debuggable)
 *  Eviction: LRU policy set in Redis config (maxmemory-policy)
 *  In production: use Redis Sentinel or Cluster for HA.
 * ================================================================
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Builds a RedisCacheManager with per-cache TTL configuration.
     * Each cache name maps to a specific RedisCacheConfiguration.
     *
     * Serializer choice: GenericJackson2JsonRedisSerializer
     *  • Stores values as readable JSON (not Java serialized bytes)
     *  • Compatible across app restarts (no ClassCastException)
     *  • Supports polymorphic types via @class metadata
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory factory) {

        // ── Default config (fallback for unnamed caches) ─────────
        RedisCacheConfiguration defaultConfig = buildConfig(Duration.ofMinutes(10));

        // ── Per-cache TTL overrides ───────────────────────────────
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            "accounts",      buildConfig(Duration.ofMinutes(10)),
            "customers",     buildConfig(Duration.ofMinutes(30)),
            "dashboardData", buildConfig(Duration.ofMinutes(5)),
            "quickStats",    buildConfig(Duration.ofMinutes(2)),
            "dashboardStats",buildConfig(Duration.ofMinutes(10)),
            "beneficiaries", buildConfig(Duration.ofMinutes(60)),
            "loanPortfolio", buildConfig(Duration.ofMinutes(15)),
            "auditStats",    buildConfig(Duration.ofMinutes(30))
        );

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            // Do not create caches on-the-fly that aren't in the map
            .disableCreateOnMissingCache()
            .build();
    }

    /**
     * Builds a RedisCacheConfiguration with JSON serialization and
     * configurable TTL. Null values are NOT cached (prevents
     * caching "not found" states which can cause stale data bugs).
     */
    private RedisCacheConfiguration buildConfig(Duration ttl) {

        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(mapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer)
                )
                .disableCachingNullValues();
    }
    }

