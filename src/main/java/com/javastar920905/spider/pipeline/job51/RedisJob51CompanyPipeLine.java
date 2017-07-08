package com.javastar920905.spider.pipeline.job51;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;

import com.alibaba.fastjson.JSONObject;
import com.javastar920905.spider.util.RedisOpsUtil;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import static com.javastar920905.spider.util.StringUtil.RESULT;

/**
 * Created by ouzhx on 2017/7/5.
 */
public class RedisJob51CompanyPipeLine extends RedisOpsUtil implements Pipeline {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisJob51CompanyPipeLine.class);

  // 一个页面请求完成触发一次
  public void process(ResultItems resultItems, Task task) {
    // 将请求发送到消息队列,空闲时处理
    RedisConnection connection = null;
    try {
      connection = getRedisConnection();
      JSONObject companyJson = resultItems.get(RESULT);
      if (companyJson != null) {
        byte[] positionId = companyJson.getString("companyId").getBytes();
        connection.hSet(KEY_JOB51_COMPANY_DETAIL, positionId,
            companyJson.toJSONString().getBytes());
      }
    } catch (Exception e) {
      closeRedisConnection(connection);
      LOGGER.error("存取结果失败 {}", resultItems.getRequest().getUrl(), e);
    } finally {
      closeRedisConnection(connection);
    }
  }
}
