package com.javastar920905.spider.util;

import com.alibaba.fastjson.JSONArray;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.alibaba.fastjson.JSONObject;

import static redis.clients.jedis.Protocol.CHARSET;

/**
 * Created by ouzhx on 2017/7/7. 可以查看参考文档了解redisTemplate操作对象的接口
 * http://docs.spring.io/spring-data/redis/docs/1.8.4.RELEASE/reference/html/#redis:template
 */
public class RedisOpsUtil {
  private static final String KEY_CACHE_PREFIX = "spider:";
  /**
   * 职位列表页要缓存的key
   */
  public static final String KEY_JOB51_POSITION = "spider:job51:position";
  public static final String KEY_JOB51_POSITION_LINK = "spider:job51:position:link";
  public static final String KEY_JOB51_COMPANY_LINK = "spider:job51:position:companylink";
  /**
   * 职位详情key
   */
  public static final String KEY_JOB51_POSITION_DETAIL = "spider:job51:position:detail";
  /**
   * 公司详情key
   */
  public static final String KEY_JOB51_COMPANY_DETAIL = "spider:job51:company:detail";
  private static final String PAGE_NUM = "pageNum";


  // <editor-fold desc="使用spring redisTemplate 操作redis 遇到个问题操作时乱码,所以改用RedisConnection">
  // 遇到个问题操作时乱码,所以改用RedisConnection
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
  // </editor-fold>

  // <editor-fold desc="使用jedis 客户端操作redis">
  // 使用jedis 客户端操作
  public static RedisConnection getRedisConnection() {
    return SpringContextUtil.getApplicationContext().getBean(JedisConnectionFactory.class)
        .getConnection();
  }


  public static void closeRedisConnection(RedisConnection connection) {
    if (connection != null) {
      connection.close();
    }
  }
  // </editor-fold>

  // <editor-fold desc="字符串方式 操作redis">
  /**
   * 字符串方式 操作redis
   * 
   * @param redisKey redis 中的key
   * @param field 字段名
   * @param value redis可以保存任意类型的值,这统一为json对象
   */
  public static void saveToRedis(byte[] redisKey, String field, JSONObject value) {
    RedisConnection connection = null;
    try {
      connection = getRedisConnection();
      connection.expire(redisKey, 60 * 60 * 4);// s为单位, 4h过期
      connection.hSet(redisKey, field.getBytes(CHARSET), value.toJSONString().getBytes(CHARSET));
    } catch (Exception e) {
      //
    } finally {
      closeRedisConnection(connection);
    }
  }

  /**
   * 字符串方式 操作redis
   * 
   * @param redisKey redis 中的key
   * @param field 字段名
   * @return
   */
  public static JSONObject getFromRedis(byte[] redisKey, String field) {
    RedisConnection connection = null;
    try {
      connection = getRedisConnection();
      if (StringUtil.isNotBlank(field)) {
        byte[] bytes = connection.hGet(redisKey, field.getBytes(CHARSET));
        if (bytes != null) {
          return JSONObject.parseObject(new String(bytes, CHARSET));
        }
      }
    } catch (Exception e) {
      //
    } finally {
      closeRedisConnection(connection);
    }
    return null;
  }
  // </editor-fold>

  // <editor-fold desc="设置搜索列表缓存">
  // 将搜索条件生成hash 做key
  public static byte[] dealRedisCachekey(JSONObject searchJson) {
    return (KEY_CACHE_PREFIX + searchJson.hashCode()).getBytes();
  }

  /**
   * 判断搜索条件是否被缓存 ouzhx on 2017/7/4
   * 
   * @param searchVo 搜索条件
   * @param pageNum 当前页码
   * @return 已经缓存搜索结果
   */
  public static JSONObject getCachedSearchList(Object searchVo, Integer pageNum) {
    RedisConnection connection = null;
    JSONObject CachedCrossPlatSearchList;
    try {
      connection = getRedisConnection();
      if (connection == null) {
        return null;
      }
      CachedCrossPlatSearchList = null;
      JSONObject searchJson = JSONUtil.parseObjectToJSONObject(searchVo, null);
      searchJson.put(PAGE_NUM, pageNum);

      // 检查redis中 是否存在searchKey
      byte[] searchKey = dealRedisCachekey(searchJson);
      byte[] cacheList = connection.get(searchKey);
      if (cacheList != null) {
        CachedCrossPlatSearchList = JSONObject.parseObject(new String(cacheList));
      }
    } finally {
      closeRedisConnection(connection);
    }
    return CachedCrossPlatSearchList;
  }

  /**
   * 设置搜索缓存
   *
   * @param searchVo 搜索条件
   * @param pageNum 当前页码
   * @param resultJson 搜索结果
   */
  public static void setCachedSearchList(Object searchVo, Integer pageNum, JSONObject resultJson) {
    RedisConnection connection = null;
    try {
      connection = getRedisConnection();
      if (resultJson == null || connection == null) {
        return;
      }
      JSONObject searchJson = JSONUtil.parseObjectToJSONObject(searchVo, null);
      searchJson.put(PAGE_NUM, pageNum);

      // redis,存放一个新的key searchField
      byte[] searchKey = dealRedisCachekey(searchJson);
      byte[] resultValue = resultJson.toString().getBytes();

      long expire = 60 * 60 * 4;
      connection.setEx(searchKey, expire, resultValue);
    } finally {
      closeRedisConnection(connection);
    }
  }
  // </editor-fold>
}
