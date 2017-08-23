package com.javastar920905.spider.config;

import com.javastar920905.spider.pageprocessor.rcj.city58.BaseCity58PositionProcessor;
import com.javastar920905.spider.pageprocessor.rcj.city58.City58PositionListPageIncrementProcessor;
import com.javastar920905.spider.pageprocessor.rcj.job51.BaseJob51PositionProcessor;
import com.javastar920905.spider.pageprocessor.rcj.job51.Job51PositionListPageIncrementProcessor;
import com.javastar920905.spider.pageprocessor.rcj.zhilian.BaseZhiLianPositionProcessor;
import com.javastar920905.spider.pageprocessor.rcj.zhilian.ZhiLianPositionListPageIncrementProcessor;
import com.javastar920905.spider.pipeline.city58.ESCity58PositionPipeLine;
import com.javastar920905.spider.util.SpiderUtil;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.ConsolePipeline;

/**
 * Created by ouzhx on 2017/8/10.
 */
public class Main {
  public static void main(String[] args) {
    ConfigurableApplicationContext context =
        new AnnotationConfigApplicationContext(SpiderUtil.class, RedisConfig.class, ESConfig.class);

    // 往reids中填充要扒取的url数据
    try {
      // new Thread(() -> BaseJob51PositionProcessor.Increment.generateIncreUrls()).start();
      // new Thread(() ->
      // BaseZhiLianPositionProcessor.PositionList.Increment.generateIncreUrls()).start();
      //new Thread(BaseCity58PositionProcessor.PositionList.Increment::generateIncreUrls).start();


      Thread.sleep(5 * 1000);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // 启动爬虫
    // Job51PositionListPageIncrementProcessor.runIncrementSpider();
    // ZhiLianPositionListPageIncrementProcessor.runIncrementSpider();
    Spider.create(new City58PositionListPageIncrementProcessor())
        // .addUrl(BaseCity58PositionProcessor.PositionList.Increment.fistPage)
        .addUrl(
            "http://bj.58.com/tech/?postdate=20170822_20170823&PGTID=0d303655-0000-16a7-43e6-eedd91f3c35c&ClickID=4")
        .addPipeline(new ESCity58PositionPipeLine()).thread(50).start();
  }
}
