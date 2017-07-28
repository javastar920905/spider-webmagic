package com.javastar920905.spider.listener;

import com.javastar920905.spider.util.RedisOpsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;


import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.SpiderListener;

/**
 * 职位扒取失败页面监听 Created by zengch on 2016/11/16.
 */
public class FailedPageListener implements SpiderListener {
  private Logger logger = LoggerFactory.getLogger(FailedPageListener.class);


  @Override
  public void onSuccess(Request request) {
    // do nothing
  }

  // 爬虫出错的时候切换IP
  @Override
  public void onError(Request request) {
    logger.info("=====不知道怎么回事, 页面就是下载不下来(URL:" + request.getUrl() + ")=====");
    RedisConnection connection = RedisOpsUtil.getRedisConnection();
    connection.lRem(RedisOpsUtil.KEY_JOB51_POSITION_LINK, 1, request.getUrl().getBytes());
    RedisOpsUtil.closeRedisConnection(connection);
  }
}
