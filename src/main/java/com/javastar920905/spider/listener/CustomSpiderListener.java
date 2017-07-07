package com.javastar920905.spider.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.SpiderListener;

/**
 * Created by ouzhx on 2017/7/7.
 */
public class CustomSpiderListener implements SpiderListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(CustomSpiderListener.class);

  @Override
  public void onSuccess(Request request) {
    LOGGER.info("抓取页面成功! {}", request.getUrl());
  }

  @Override
  public void onError(Request request) {
    LOGGER.info("抓取页面失败! {}", request.getUrl());
  }
}
