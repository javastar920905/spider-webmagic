package com.javastar920905.spider.pageprocessor.job51;

import java.util.ArrayList;
import java.util.List;

import com.javastar920905.spider.config.RedisConfig;
import com.javastar920905.spider.pipeline.job51.RedisJob51PositionListPipeLine;
import com.javastar920905.spider.util.SpiderUtil;
import com.javastar920905.spider.util.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.javastar920905.spider.util.Job51PositionUtil;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.RedisScheduler;
import us.codecraft.webmagic.selector.Html;

import javax.management.JMException;

/**
 * Created by ouzhx on 2017/7/5. 扒取51job 职位列表(注解方式不好用,还是这个模式实在)
 *
 */
public class Job51PositionListPageProcessor extends Job51PositionUtil implements PageProcessor {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(Job51PositionListPageProcessor.class);
  // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
  private Site site = Site.me();
  private static List<String> historyAreaNumber = new ArrayList<>();

  public static void main(String[] args) {
    // spring 容器加载redis
    ConfigurableApplicationContext context =
        new AnnotationConfigApplicationContext(SpiderUtil.class, RedisConfig.class);
    SpringContextUtil.printIocContextBeanNames();



    // 启动spider 爬虫,没有托管给spring
    // 发起页面请求,开启5个线程并启动爬虫 // 输出到文件,每次抓取会覆盖掉原来的文件
    // .addPipeline(new JsonFilePipeline("D:/webmgicData/"))
    Spider webMagicIOSpider = Spider.create(new Job51PositionListPageProcessor())
        // .setScheduler(new FileCacheQueueScheduler("D:/webmgicData/cache")) //
        // 使用文件保存抓取URL，可以在关闭程序并下次启动时，从之前抓取到的URL继续抓取(fileScheduler /redis 用一个即可)
        .setScheduler(new RedisScheduler(RedisConfig.Host))
        .addRequest(getRequest(PositionList.fistPage))
        .addPipeline(new RedisJob51PositionListPipeLine()).thread(5);

    //将爬虫对象交给spring 托管
    context.getBeanFactory().registerSingleton("webMagicIOSpider", webMagicIOSpider);
    try { // 添加扒取数量监控
      SpiderMonitor.instance().register(webMagicIOSpider);
    } catch (JMException e) {
      e.printStackTrace();
    }
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
    // 职位列表css定位
    String parentCss = "#resultList .el ";
    // 职位信息获取 (这里有多少个字段就相当于有多少个数组)
    List<String> positionIdList = html.$(parentCss + ".t1 input", "value").all(); // id
    List<String> positionNameList = html.css(parentCss + ".t1 span a").xpath("a/text()").all(); // 职位名
    List<String> positionLinkList = html.css(parentCss + ".t1 span a").links().all();
    List<String> companyNameList = html.css(parentCss + ".t2 a").xpath("a/text()").all(); // 公司名
    List<String> companyLinkList = html.css(parentCss + ".t2 a").links().all();
    List<String> workPlaceList =
        html.css(parentCss + ".t3:not(.title .t3)").xpath("span/text()").all();
    List<String> salaryList =
        html.css(parentCss + ".t4:not(.title .t4)").xpath("span/text()").all();
    List<String> publishDateList =
        html.css(parentCss + " .t5:not(.title .t5)").xpath("span/text()").all();

    JSONArray positionJsonArray = new JSONArray();
    int i = 0;
    try {
      if (positionIdList != null) {
        int listSize = positionIdList.size();
        for (; i < listSize; i++) {
          JSONObject json = new JSONObject();
          json.put("positionId", positionIdList.get(i));
          json.put("positionName", positionNameList.get(i));
          json.put("positionLink", positionLinkList.get(i));
          json.put("companyName", companyNameList.get(i));
          json.put("companyLink", companyLinkList.get(i));
          json.put("workPlace", workPlaceList.get(i));
          json.put("salary", salaryList.get(i));
          json.put("publishDate", publishDateList.get(i));
          positionJsonArray.add(json);
        }
      }

      // 部分三: 如果启动时设置了pipeline 就需要到指定类处理抓取后的结果
      page.putField(RESULT, positionJsonArray);


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
