package com.javastar920905.spider.pageprocessor.job51;


import static com.javastar920905.spider.util.StringUtil.RESULT;

import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import com.javastar920905.spider.config.RedisConfig;
import com.javastar920905.spider.pipeline.job51.RedisJob51PositionListPipeLine;
import com.javastar920905.spider.util.Job51PositionUtil;
import com.javastar920905.spider.util.SpiderUtil;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;

/**
 * Created by ouzhx on 2017/7/5. 增量爬取51job 职位列表(发现指定时间段内新增职位url)
 *
 */
@Component
public class Job51PositionListPageIncrementProcessor extends Job51PositionUtil
    implements PageProcessor {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(Job51PositionListPageIncrementProcessor.class);
  // 部分一：抓取网站的相关配置
  private Site site = Site.me();
  private static List<String> historyAreaNumber = new Vector<>();


  // @Scheduled(cron = "0 0 9/3 * * ?") // 9点开始每3个小时执行一次
  // @Scheduled(fixedDelay = 70)
  // public static void runIncrementSpider() {
  public static void main(String[] args) {
    // spring 容器加载redis
    ConfigurableApplicationContext context =
        new AnnotationConfigApplicationContext(SpiderUtil.class, RedisConfig.class);
    LOGGER.info("runIncrementSpider begin! ...");

    // 发起页面请求,开启5个线程并启动爬虫
    Spider webMagicIOSpider = Spider.create(new Job51PositionListPageIncrementProcessor())
        // .setScheduler(new RedisScheduler(REDIS_HOST))
        .addRequest(getRequest(PositionList.Increment.fistPage))
        .addPipeline(new RedisJob51PositionListPipeLine()).thread(5);
    webMagicIOSpider.runAsync();

    // 开始执行详情页的爬取
    new Thread(() -> Job51PositionPageProcessor.runPositionSpider()).start();
    new Thread(() -> Job51CompanyPageProcessor.runCompanySpider()).start();
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
      // 部分四：发现指定时间内增量url

      String currentNum = getCurrentAreaNumber(request.getUrl());
      if (currentNum != null && !historyAreaNumber.contains(currentNum)) {
        synchronized (historyAreaNumber) {
          if (!historyAreaNumber.contains(currentNum)) {
            historyAreaNumber.add(currentNum);
            // 这里调用的是获取增量url方法
            page.addTargetRequests(PositionList.Increment.getIncreUrls(html, currentNum));
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
