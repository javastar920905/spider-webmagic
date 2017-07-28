package com.javastar920905.spider.pipeline.job51;



import com.javastar920905.spider.util.RedisOpsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;

import com.alibaba.fastjson.JSONObject;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import static com.javastar920905.spider.util.StringUtil.RESULT;

/**
 * Created by ouzhx on 2017/7/5. 职位详情扒取结果处理
 */
public class RedisJob51PositionPipeLine extends RedisOpsUtil implements Pipeline {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisJob51PositionPipeLine.class);

  public void process(ResultItems resultItems, Task task) {
    RedisConnection connection = null;
    try {
      Request request = resultItems.getRequest();
      JSONObject positionJson = resultItems.get(RESULT);
      if (positionJson != null) {
        connection = getRedisConnection();
        connection.rPush(KEY_JOB51_POSITION_DETAIL, positionJson.toJSONString().getBytes());
        // 移除(从左到右方式)已经扒取的职位url
        connection.lRem(KEY_JOB51_POSITION_LINK, 1, request.getUrl().getBytes());
      }
    } catch (Exception e) {
      closeRedisConnection(connection);
      LOGGER.error("存取结果失败 {}", resultItems.getRequest().getUrl(), e);
    } finally {
      closeRedisConnection(connection);
    }
  }
}
