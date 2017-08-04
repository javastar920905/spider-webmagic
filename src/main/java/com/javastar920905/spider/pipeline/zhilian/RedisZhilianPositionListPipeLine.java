package com.javastar920905.spider.pipeline.zhilian;



import static com.javastar920905.spider.util.SpiderConstantKey.COMPANY_JSON;
import static com.javastar920905.spider.util.SpiderConstantKey.POSITION_JSON;
import static com.javastar920905.spider.util.StringUtil.RESULT;

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
 * Created by ouzhx on 2017/7/5. 职位列表扒取结果处理
 */
public class RedisZhilianPositionListPipeLine extends RedisOpsUtil implements Pipeline {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(RedisZhilianPositionListPipeLine.class);

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
          JSONObject positionExtraJson = JSONObject.parseObject(positionString);
          JSONObject positionJson = positionExtraJson.getJSONObject(POSITION_JSON);
          JSONObject companyJson = positionExtraJson.getJSONObject(COMPANY_JSON);
          if (positionJson != null) {
            connection.rPush(KEY_ZHILIAN_POSITION_DETAIL, positionJson.toJSONString().getBytes());
            if (companyJson != null) {
              connection.rPush(KEY_ZHILIAN_COMPANY_DETAIL, companyJson.toJSONString().getBytes());
            }
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("存取结果失败 {}", resultItems.getRequest().getUrl(), e);
    } finally {
      closeRedisConnection(connection);
    }
  }
}
