package com.giftshop.backend.giftcard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheManager;

import com.giftshop.backend.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class GiftCardCacheIntegrationTest {

    @Autowired
    private GiftCardService giftCardService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void catalog_isCachedInRedis() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);

        Cache cache = cacheManager.getCache("giftCards");
        cache.clear();

        List<GiftCard> fromDatabase = giftCardService.getAll();

        assertThat(cache.get(SimpleKey.EMPTY)).isNotNull();
        assertThat(giftCardService.getAll()).isEqualTo(fromDatabase);
    }
}
