package no.idporten.eudiw.demo.verifier.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

public class RedisCache implements Cache {

    private static final Logger log = LoggerFactory.getLogger(RedisCache.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void set(String cacheKey, Object object, Duration duration) {
        try {
            redisTemplate.opsForValue().set(cacheKey, object, duration);
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Failed to set {} object in cache: {}", cacheKey, e.getMessage());
            throw e;
        }
    }

    @Override
    public Object get(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Failed to get {} object from cache: {}", cacheKey, e.getMessage());
            throw e;
        }
    }

    @Override
    public Object remove(String cacheKey) {
        try {
            return redisTemplate.opsForValue().getAndDelete(cacheKey);
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Failed to delete {} object from cache: {}", cacheKey, e.getMessage());
            throw e;
        }
    }
}
