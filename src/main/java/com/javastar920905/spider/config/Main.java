package com.javastar920905.spider.config;

import com.javastar920905.spider.listener.UpdateProxyListener;
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
import us.codecraft.webmagic.SpiderListener;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.pipeline.ConsolePipeline;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ouzhx on 2017/8/10.
 */
public class Main {
  public static void main(String[] args) {
    ConfigurableApplicationContext context =
        new AnnotationConfigApplicationContext(SpiderUtil.class, RedisConfig.class, ESConfig.class);

    Spider city58Spider = Spider.create(new City58PositionListPageIncrementProcessor())
        .addUrl(BaseCity58PositionProcessor.PositionList.Increment.fistPage)
        .addPipeline(new ESCity58PositionPipeLine()).thread(20);
    // 将当期spider对象注入 Listener中
    List<SpiderListener> spiderListenerList = new ArrayList<>();
    spiderListenerList.add(new UpdateProxyListener(city58Spider));
    // city58Spider.setSpiderListeners(spiderListenerList);
    SpiderUtil.SpiderProxy.city58Spider = city58Spider;

    // 往reids中填充要扒取的url数据
    try {
      new Thread(() -> BaseJob51PositionProcessor.Increment.generateIncreUrls()).start();
     new Thread(() -> BaseZhiLianPositionProcessor.PositionList.Increment.generateIncreUrls())
          .start();
      new Thread(BaseCity58PositionProcessor.PositionList.Increment::generateIncreUrls).start();


      Thread.sleep(5 * 1000);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // 启动爬虫
    Job51PositionListPageIncrementProcessor.runIncrementSpider();
    ZhiLianPositionListPageIncrementProcessor.runIncrementSpider();


    city58Spider.start();

  }
}
