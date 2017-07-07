package com.javastar920905.spider.util;

import com.alibaba.fastjson.JSONArray;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by ouzhx on 2017/7/7. 可以查看参考文档了解redisTemplate操作对象的接口
 * http://docs.spring.io/spring-data/redis/docs/1.8.4.RELEASE/reference/html/#redis:template
 */
public class RedisOpsUtil {
  public static final String KEY_JOB51_POSITION = "spider:job51:position";

  public static RedisTemplate redisTemplate = SpringContextUtil.getBean(RedisTemplate.class);
  // 字符串操作
  public static ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
  // list操作
  public static ListOperations<String, String> listOps = redisTemplate.opsForList();

  // hash 操作
  public static HashOperations<String, String, String> hashOpsString = redisTemplate.opsForHash();
  public static HashOperations<String, String, JSONArray> hashOpsJsonArray =
      redisTemplate.opsForHash();
  public static HashOperations<byte[], byte[], byte[]> hashOpsObject = redisTemplate.opsForHash();
  public static HashOperations<String, String, JSONObject> hashOpsJsonObject =
      redisTemplate.opsForHash();

  public static RedisConnection getRedisConnection() {
    return SpringContextUtil.getApplicationContext().getBean(JedisConnectionFactory.class)
        .getConnection();
  }


  public static void closeRedisConnection(RedisConnection connection) {
    if (connection != null) {
      connection.close();
    }
  }
}
