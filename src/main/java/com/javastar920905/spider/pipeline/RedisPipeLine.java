package com.javastar920905.spider.pipeline;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.javastar920905.spider.pageprocessor.Job51PositionPageProcessor;
import com.javastar920905.spider.util.RedisOpsUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

/**
 * Created by ouzhx on 2017/7/5.
 */
public class RedisPipeLine extends RedisOpsUtil implements Pipeline {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisPipeLine.class);

  // 一个页面请求完成触发一次
  public void process(ResultItems resultItems, Task task) {
    // 将请求发送到消息队列,空闲时处理
    RedisConnection connection = null;
    try {
      connection = getRedisConnection();
      JSONArray jsonArray = resultItems.get("positionJsonArray");
      if (jsonArray != null) {
        for (Object positionObj : jsonArray) {
          String positionString = JSONObject.toJSONString(positionObj);
          JSONObject positionJson = JSONObject.parseObject(positionString);
          connection.hSet(KEY_JOB51_POSITION.getBytes(),
              positionJson.getString("positionId").getBytes(), positionString.getBytes());
        }
      }
    } catch (Exception e) {
      closeRedisConnection(connection);
      LOGGER.error("存取结果失败 {}", resultItems.getRequest().getUrl(), e);
    } finally {
      closeRedisConnection(connection);
    }
  }
}
