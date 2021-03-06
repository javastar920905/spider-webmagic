package com.javastar920905.spider.pageprocessor.demo.zhilian;

import static com.javastar920905.spider.util.StringUtil.RESULT;

import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.javastar920905.spider.config.RedisConfig;
import com.javastar920905.spider.pipeline.zhilian.RedisZhilianPositionListPipeLine;
import com.javastar920905.spider.util.SpiderUtil;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;

/**
 * 智联招聘职位增量爬取
 *
 * Created by ouzhx on 2017/7/31.
 */
public class ZhiLianPositionListPageIncrementProcessor extends BaseZhiLianPositionProcessor
    implements PageProcessor {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ZhiLianPositionListPageIncrementProcessor.class);
  // 部分一：抓取网站的相关配置
  private Site site = Site.me();
  // 记录已经扒取过的地区
  private static List<String> historyAreaNumber = new Vector<>();

  public Site getSite() {
    return SpiderUtil.setSite(site);
  }

  private static Spider newInstance() {
    return Spider.create(new ZhiLianPositionListPageIncrementProcessor())
        .addRequest(getRequest(BaseZhiLianPositionProcessor.PositionList.Increment.fistPage))
        .addPipeline(new RedisZhilianPositionListPipeLine()).thread(5);
  }

  // 从url中截取地区编号
  private static String getCurrentAreaNumber(String url) {
    if (url == null) {
      return null;
    }
    String currentAreaNumber = url.substring(url.lastIndexOf("&jl=") + 4, url.indexOf("&isadv="));
    LOGGER.debug("线程名称:{} ===================> 历史url地区编号:{}, 当前地区编号:{}",
        Thread.currentThread().getName(), historyAreaNumber, currentAreaNumber);
    return currentAreaNumber;
  }


  // spring 容器加载redis
  public static void main(String[] args) {
    ConfigurableApplicationContext context =
        new AnnotationConfigApplicationContext(SpiderUtil.class, RedisConfig.class);
    LOGGER.info("runIncrementSpider begin! ...");

    Spider webMagicIOSpider = newInstance();
    webMagicIOSpider.runAsync();
  }


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
            page.addTargetRequests(
                BaseZhiLianPositionProcessor.PositionList.Increment.getIncreUrls(html, currentNum));
          }
        }
      }


    } catch (Exception e) {
      LOGGER.error("获取页面失败 {}", request.getUrl(), e);
    }
  }



}
