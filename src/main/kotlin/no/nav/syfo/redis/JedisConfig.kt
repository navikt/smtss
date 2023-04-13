package no.nav.syfo.redis

import redis.clients.jedis.JedisPoolConfig
import java.time.Duration

class JedisConfig : JedisPoolConfig() {
    init {
        testWhileIdle = true
        minEvictableIdleTime = Duration.ofMillis(300000)
        timeBetweenEvictionRuns = Duration.ofMillis(60_000)
        maxTotal = 20
        maxIdle = 20
        minIdle = 10
        blockWhenExhausted = true
    }
}
