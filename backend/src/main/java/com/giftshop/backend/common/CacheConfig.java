package com.giftshop.backend.common;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's cache abstraction, backed by Redis
 * (see spring.cache.redis.* in application.properties).
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
