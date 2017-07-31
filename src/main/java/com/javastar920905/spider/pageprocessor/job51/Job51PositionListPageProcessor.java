package com.javastar920905.spider.pageprocessor.job51;


import static com.javastar920905.spider.util.StringUtil.RESULT;

import java.util.ArrayList;
import java.util.List;

import javax.management.JMException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.javastar920905.spider.config.RedisConfig;
import com.javastar920905.spider.pipeline.job51.RedisJob51PositionListPipeLine;
import com.javastar920905.spider.util.SpiderUtil;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;

/**
 * Created by ouzhx on 2017/7/5. 扒取51job 职位列表(注解方式不好用,还是这个模式实在)
 *
 */
public class Job51PositionListPageProcessor extends BaseJob51PositionProcessor implements PageProcessor {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(Job51PositionListPageProcessor.class);
  // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
  private Site site = Site.me();
  private static List<String> historyAreaNumber = new ArrayList<>();

  public static void main(String[] args) {
    // spring 容器加载redis
    ConfigurableApplicationContext context =
        new AnnotationConfigApplicationContext(SpiderUtil.class, RedisConfig.class);


    // 启动spider 爬虫,没有托管给spring
    // 发起页面请求,开启5个线程并启动爬虫 // 输出到文件,每次抓取会覆盖掉原来的文件
    Spider webMagicIOSpider = Spider.create(new Job51PositionListPageProcessor())
        // 使用文件保存抓取URL，可以在关闭程序并下次启动时，从之前抓取到的URL继续抓取(fileScheduler /redis 用一个即可)
        // .setScheduler(new RedisScheduler(REDIS_HOST))
        .addRequest(getRequest(PositionList.fistPage))
        .addPipeline(new RedisJob51PositionListPipeLine()).thread(5);

    try { // 添加扒取数量监控
      SpiderMonitor.instance().register(webMagicIOSpider);
    } catch (JMException e) {
      e.printStackTrace();
    }
    /*context.getBeanFactory().registerSingleton("webMagicIOSpider",webMagicIOSpider);
    Spider webMagicIOSpider2 = context.getBean("webMagicIOSpider",Spider.class);*/
    webMagicIOSpider.start();
  }

  // 从url中截取地区编号
  private static String getCurrentAreaNumber(String url) {
    if (url == null) {
      return null;
    }
    String currentAreaNumber = url.substring(url.lastIndexOf("/") + 1, url.indexOf(","));
    LOGGER.debug("线程名称:{} ===================> 历史url地区编号:{}, 当前地区编号:{}",
        Thread.currentThread().getName(), historyAreaNumber, currentAreaNumber);
    return currentAreaNumber;
  }



  // process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑
  @Override
  public void process(Page page) {
    // 部分二：定义如何抽取页面信息，并保存下来
    Request request = page.getRequest();
    Html html = page.getHtml();
    // 部分三: 如果启动时设置了pipeline 就需要到指定类处理抓取后的结果
    page.putField(RESULT, PositionList.dealPositionList(html));


    try {
      // 部分四：从页面发现后续的url地址来抓取
      String currentNum = getCurrentAreaNumber(request.getUrl());
      if (currentNum != null && !historyAreaNumber.contains(currentNum)) {
        synchronized (historyAreaNumber) {
          if (!historyAreaNumber.contains(currentNum)) {
            historyAreaNumber.add(currentNum);
            page.addTargetRequests(PositionList.getUrls(html, currentNum));
          }
        }
      }


    } catch (Exception e) {
      LOGGER.error("获取页面失败 {}", request.getUrl(), e);
    }
  }


  public Site getSite() {
    return SpiderUtil.setSite(site);
  }


}
