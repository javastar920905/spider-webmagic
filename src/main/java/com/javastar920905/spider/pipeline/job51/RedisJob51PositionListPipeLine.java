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

import static com.javastar920905.spider.util.StringUtil.RESULT;

/**
 * Created by ouzhx on 2017/7/5.
 */
public class RedisJob51PositionListPipeLine extends RedisOpsUtil implements Pipeline {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisJob51PositionListPipeLine.class);

  // 一个页面请求完成触发一次
  public void process(ResultItems resultItems, Task task) {
    // 将请求发送到消息队列,空闲时处理
    RedisConnection connection = null;
    try {
      connection = getRedisConnection();
      JSONArray jsonArray = resultItems.get(RESULT);
      if (jsonArray != null) {
        for (Object positionObj : jsonArray) {
          String positionString = JSONObject.toJSONString(positionObj);
          JSONObject positionJson = JSONObject.parseObject(positionString);
          byte[] positionId = positionJson.getString("positionId").getBytes();
          byte[] positionLink = positionJson.getString("positionLink").getBytes();
          byte[] companyName = positionJson.getString("companyName").getBytes();
          byte[] companyLink = positionJson.getString("companyLink").getBytes();
          connection.hSet(KEY_JOB51_POSITION, positionId, positionString.getBytes());
          connection.hSet(KEY_JOB51_POSITION_LINK, positionId, positionLink);
          connection.hSet(KEY_JOB51_COMPANY_LINK, companyName, companyLink);
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
