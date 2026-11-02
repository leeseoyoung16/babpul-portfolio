package com.bappul.matching.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackages = "com.bappul.matching.lunchmatch.match")
public class RedisConfig {
}