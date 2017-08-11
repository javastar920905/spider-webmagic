package com.javastar920905.spider.config;

import com.javastar920905.spider.pageprocessor.rcj.job51.BaseJob51PositionProcessor;
import com.javastar920905.spider.pageprocessor.rcj.job51.Job51PositionListPageIncrementProcessor;
import com.javastar920905.spider.pageprocessor.rcj.zhilian.BaseZhiLianPositionProcessor;
import com.javastar920905.spider.pageprocessor.rcj.zhilian.ZhiLianPositionListPageIncrementProcessor;
import com.javastar920905.spider.util.SpiderUtil;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Created by ouzhx on 2017/8/10.
 */
public class Main {
  public static void main(String[] args) {
    ConfigurableApplicationContext context =
        new AnnotationConfigApplicationContext(SpiderUtil.class, RedisConfig.class);

    // 往reids中填充要扒取的url数据
    try {
        new Thread(()-> BaseJob51PositionProcessor.Increment.generateIncreUrls()).start();
        new Thread(()-> BaseZhiLianPositionProcessor.PositionList.Increment.generateIncreUrls()).start();
      ;
      Thread.sleep(5 * 1000);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // 启动爬虫
    Job51PositionListPageIncrementProcessor.runIncrementSpider();
    ZhiLianPositionListPageIncrementProcessor.runIncrementSpider();
  }
}
