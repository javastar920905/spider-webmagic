package com.javastar920905.spider.config;

import com.javastar920905.spider.listener.CustomSpiderListener;
import com.javastar920905.spider.pageprocessor.Job51PositionPageProcessor;
import com.javastar920905.spider.pipeline.RedisPipeLine;
import com.javastar920905.spider.util.SpiderUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.javastar920905.spider.util.SpringContextUtil;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.SpiderListener;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.scheduler.FileCacheQueueScheduler;
import us.codecraft.webmagic.scheduler.RedisScheduler;

import javax.management.JMException;
import java.util.ArrayList;
import java.util.List;

import static com.javastar920905.spider.util.CommonUtil.getRequest;
import static com.javastar920905.spider.util.Job51PositionUtil.fistPositionPage;

/**
 * Created by ouzhx on 2017/7/7.
 */
public class AppConfig {

  public static void main(String[] args) {
    // spring 容器加载redis
    ApplicationContext context =
        new AnnotationConfigApplicationContext(SpiderUtil.class, RedisConfig.class);
    SpringContextUtil.printIocContextBeanNames();



    // 启动spider 爬虫,没有托管给spring
    // 发起页面请求,开启5个线程并启动爬虫 // 输出到文件,每次抓取会覆盖掉原来的文件
    // .addPipeline(new JsonFilePipeline("D:/webmgicData/"))
    Spider webMagicIOSpider = Spider.create(new Job51PositionPageProcessor())
        .setScheduler(new FileCacheQueueScheduler("D:/webmgicData/cache")) // 使用文件保存抓取URL，可以在关闭程序并下次启动时，从之前抓取到的URL继续抓取
        .setScheduler(new RedisScheduler(RedisConfig.Host)).addRequest(getRequest(fistPositionPage))
        .addPipeline(new RedisPipeLine()).thread(5);

    try { // 添加扒取数量监控
      SpiderMonitor.instance().register(webMagicIOSpider);
    } catch (JMException e) {
      e.printStackTrace();
    }
    SpiderUtil.webMagicIOSpider = webMagicIOSpider;
    webMagicIOSpider.start();
  }



}
