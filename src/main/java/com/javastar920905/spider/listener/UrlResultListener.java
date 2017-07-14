package com.javastar920905.spider.listener;

import com.javastar920905.spider.pageprocessor.job51.Job51CompanyPageProcessor;
import com.javastar920905.spider.pageprocessor.job51.Job51PositionPageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.SpiderListener;

/**
 * url抓取结果监听器监听器 Created by zengch on 2016/11/16.
 */
public class UrlResultListener implements SpiderListener {
  private Logger logger = LoggerFactory.getLogger(UrlResultListener.class);


  @Override
  public void onSuccess(Request request) {
    // do nothing
  }

  // 爬虫出错的时候切换IP
  @Override
  public void onError(Request request) {
    logger.info("=====不知道怎么回事, 这个页面就是下载不下来 +_+!!! :" + request.getUrl());
    String url = request.getUrl();
    if (Job51CompanyPageProcessor.urls.contains(url)) {
      synchronized (Job51CompanyPageProcessor.urls) {
        Job51CompanyPageProcessor.urls.remove(url);
      }
    } else if (Job51PositionPageProcessor.urls.contains(url)) {
      synchronized (Job51PositionPageProcessor.urls) {
        Job51PositionPageProcessor.urls.remove(url);
      }
    }
  }
}
