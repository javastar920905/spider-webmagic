package com.javastar920905.spider.pipeline.job51;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.javastar920905.spider.util.RedisOpsUtil;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

/**
 * Created by ouzhx on 2017/7/5.
 */
public class RedisJob51PositionPipeLine extends RedisOpsUtil implements Pipeline {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisJob51PositionPipeLine.class);

  // 一个页面请求完成触发一次
  public void process(ResultItems resultItems, Task task) {
    // 将请求发送到消息队列,空闲时处理
    RedisConnection connection = null;
    try {
      connection = getRedisConnection();
      JSONObject positionJson = resultItems.get("positionJson");
      if (positionJson != null) {
        byte[] positionId = positionJson.getString("positionId").getBytes();
        connection.hSet(KEY_JOB51_POSITION_DETAIL.getBytes(), positionId,
            positionJson.toJSONString().getBytes());
      }
    } catch (Exception e) {
      closeRedisConnection(connection);
      LOGGER.error("存取结果失败 {}", resultItems.getRequest().getUrl(), e);
    } finally {
      closeRedisConnection(connection);
    }
  }
}
