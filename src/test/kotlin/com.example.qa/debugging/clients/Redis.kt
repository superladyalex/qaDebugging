package com.example.qa.debugging.clients

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands


class Redis(host: String, port: String, password:String) {

    var redisUri = RedisURI.Builder.redis(host, port.toInt())
        .withSsl(true)
        .withVerifyPeer(false)
        .withPassword(password)
        .build();

    private val redisClient: RedisClient = RedisClient.create(redisUri)
    private val connection: StatefulRedisConnection<String, String> = redisClient.connect()
    private val syncCommands: RedisCommands<String, String> = connection.sync()


    fun getKey(key: String): String? {
        return syncCommands.get(key);
    }

    fun setKey(key: String, value: String): String? {
        return syncCommands.set(key, value)
    }

    fun deleteKey(key: String): Long {
        return syncCommands.del(key)
    }

    fun shutdownRedis() {
        connection.close();
        redisClient.shutdown();
    }
}