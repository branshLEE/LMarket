package com.lmarket.product.config;

import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@EnableConfigurationProperties(CacheProperties.class)
@EnableCaching
@Configuration
public class MyCacheConfig {

    @Bean
    RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties){

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        //config = config.entryTtl();
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        config = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringRedisSerializer));

        GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(genericJackson2JsonRedisSerializer));

        //将配置文件中的所有配置都生效
        CacheProperties.Redis cachePropertiesRedis = cacheProperties.getRedis();
        if(cachePropertiesRedis.getTimeToLive() != null){
            config = config.entryTtl(cachePropertiesRedis.getTimeToLive());
        }

        if(cachePropertiesRedis.getKeyPrefix() != null){
            config = config.prefixKeysWith(cachePropertiesRedis.getKeyPrefix());
        }

        if(!cachePropertiesRedis.isCacheNullValues()){
            config = config.disableCachingNullValues();
        }

        if(!cachePropertiesRedis.isUseKeyPrefix()){
            config = config.disableKeyPrefix();
        }

        return config;
    }
}
