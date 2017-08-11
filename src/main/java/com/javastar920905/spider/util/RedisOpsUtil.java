package com.javastar920905.spider.util;

import static redis.clients.jedis.Protocol.CHARSET;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ouzhx on 2017/7/7.
 */
@Component public class RedisOpsUtil {
    public static String REDIS_HOST = "192.168.1.170";
    private static final String KEY_CACHE_PREFIX = "spider:";
    /**
     * 要扒取的增量url的key
     */
    public static final byte[] KEY_51JOB_LIST_URLS = "spider:list:urls:51job".getBytes();
    public static final byte[] KEY_ZHILIAN_LIST_URLS = "spider:list:urls:zhilian".getBytes();

    /**
     * 职位列表页要缓存的key
     */
    public static final byte[] KEY_JOB51_POSITION = "spider:job51:position".getBytes();
    public static final byte[] KEY_JOB51_POSITION_LINK = "spider:job51:link:position".getBytes();
    public static final byte[] KEY_JOB51_COMPANY_LINK = "spider:job51:link:company".getBytes();
    /**
     * 职位详情key
     */
    public static final byte[] KEY_JOB51_POSITION_DETAIL =
        "spider:job51:detail:position".getBytes();
    public static final byte[] KEY_ZHILIAN_POSITION_DETAIL =
        "spider:zhilian:detail:position".getBytes();
    /**
     * 公司详情key
     */
    public static final byte[] KEY_JOB51_COMPANY_DETAIL = "spider:job51:detail:company".getBytes();
    public static final byte[] KEY_ZHILIAN_COMPANY_DETAIL =
        "spider:zhilian:detail:company".getBytes();
    private static final String PAGE_NUM = "pageNum";

    /**
     * //获取前50条url
     * List<byte[]> positionUrlList =
     * connection.lRange(RedisOpsUtil.KEY_JOB51_POSITION_LINK, 0, 49);
     * List<String> urls  =RedisOpsUtil.getStringList(positionUrlList);
     *
     * @param collection
     * @return
     */
    public static List<String> getStringList(Collection<byte[]> collection) {
        List<String> list = new ArrayList<>();
        Iterator<byte[]> it = collection.iterator();
        it.forEachRemaining(item -> {
            list.add(new String(item));
        });
        return list;
    }


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
     * @param field    字段名
     * @param value    redis可以保存任意类型的值,这统一为json对象
     */
    public static void saveToRedis(byte[] redisKey, String field, JSONObject value) {
        RedisConnection connection = null;
        try {
            connection = getRedisConnection();
            connection.expire(redisKey, 60 * 60 * 4);// s为单位, 4h过期
            connection
                .hSet(redisKey, field.getBytes(CHARSET), value.toJSONString().getBytes(CHARSET));
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
     * @param field    字段名
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
     * @param pageNum  当前页码
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
     * @param searchVo   搜索条件
     * @param pageNum    当前页码
     * @param resultJson 搜索结果
     */
    public static void setCachedSearchList(Object searchVo, Integer pageNum,
        JSONObject resultJson) {
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
