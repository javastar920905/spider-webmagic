package com.javastar920905.spider.pipeline.job51;



import com.javastar920905.spider.util.RedisOpsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import static com.javastar920905.spider.util.SpiderConstantKey.COMPANY_JSON;
import static com.javastar920905.spider.util.SpiderConstantKey.POSITION_JSON;
import static com.javastar920905.spider.util.StringUtil.RESULT;

/**
 * Created by ouzhx on 2017/7/5. 职位列表扒取结果处理
 */
public class RedisJob51PositionListPipeLine extends RedisOpsUtil implements Pipeline {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(RedisJob51PositionListPipeLine.class);

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
          String positionLink = positionExtraJson.getString("positionLink");
          if (positionLink.startsWith("http://jobs.51job.com")) {
            JSONObject positionJson = positionExtraJson.getJSONObject(POSITION_JSON);
            JSONObject companyJson = positionExtraJson.getJSONObject(COMPANY_JSON);
            if (positionJson != null) {
              connection.rPush(KEY_JOB51_POSITION_DETAIL, positionJson.toJSONString().getBytes());
              if (companyJson!=null){
                connection.rPush(KEY_JOB51_COMPANY_DETAIL, companyJson.toJSONString().getBytes());
              }
            }
          } else {
            // 美团职位等子域名 需要单独扒取 http://meituan.51job.com/sc/job_shuoming.php?jobid=87517160
            // http://hundsun.51job.com/sc/show_job_detail.php?jobid=80057520
            // http://career.dell.51job.com/jobinfo.php?jobid=83925993
            // connection.rPush(KEY_JOB51_POSITION_DIY_LINK, positionLink.getBytes());
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
