package com.userservice.infrastructure.cache.Configuration;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.userservice.infrastructure.cache.vo.CacheType;

@EnableCaching
@Configuration
public class CacheConfiguration {

	@Bean
	public CacheManager cacheManager() {

		CaffeineCacheManager cacheManager = new CaffeineCacheManager();

		cacheManager.registerCustomCache(
			CacheType.VERIFICATION_TRY.name(), Caffeine.newBuilder()
				.initialCapacity(100)
				.maximumSize(1000)
				.expireAfterAccess(Duration.ofMinutes(1))
				.recordStats()
				.build()
		);

		return cacheManager;
	}
}
