package com.javastar920905.spider.pageprocessor.job51;


import java.util.List;

import com.javastar920905.spider.pipeline.job51.RedisJob51PositionPipeLine;
import com.javastar920905.spider.util.Job51PositionUtil;
import com.javastar920905.spider.util.SpiderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import static com.javastar920905.spider.util.StringUtil.RESULT;

/**
 * Created by ouzhx on 2017/7/5.
 *
 * 扒取51job 职位详情页面
 *
 */
@Component
public class Job51PositionPageProcessor extends Job51PositionUtil implements PageProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(Job51PositionPageProcessor.class);
  private Site site = Site.me();
  private static Spider webMagicIOSpider = newPositionSpiderInstance();

  // spider 调用一次start()方法后,再次设置url会报错,所以需要重新获取新的spider实例
  private static Spider newPositionSpiderInstance() {
    return Spider.create(new Job51PositionPageProcessor()).thread(10)
        .addPipeline(new RedisJob51PositionPipeLine()).setExitWhenComplete(true);
  }

  public static void runPositionSpider() {
    try {
      Thread.sleep(1000 * 10);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    while (true) {
      try {
        // 获取前n条url(默认50)
        List urls = Position.getUrls();
        LOGGER.info("redis 获取数据" + urls.size());

        if (urls != null && urls.size() > 0) {
          if (webMagicIOSpider.getStatus() != Spider.Status.Running) {
            LOGGER.info(" 开始新一轮url扒取  urlSize: {}  spider status: {}", urls.size(),
                webMagicIOSpider.getStatus());
            if (webMagicIOSpider.getStatus() == Spider.Status.Stopped) {
              webMagicIOSpider = newPositionSpiderInstance();
              LOGGER.info(" 新spider  urlSize: {}  spider status: {}", urls.size(),
                  webMagicIOSpider.getStatus());
            }
            // 发起页面请求,开启5个线程并启动爬虫
            webMagicIOSpider.startUrls(urls);
            webMagicIOSpider.start();
          }
        }

        Thread.sleep(positionSpiderSleepInterval);
      } catch (Exception e) {
        webMagicIOSpider = newPositionSpiderInstance();
        LOGGER.info(" 职位详情扒取报错 ", e);
      }
    }
  }



  @Override
  public void process(Page page) {
    Request request = page.getRequest();
    Html html = page.getHtml();

    // 职位信息获取 TODO 这里可以优化一下直接放入json,避免产生临时变量
    String positionId = html.xpath("//*[@id=\"hidJobID\"]").$("input", "value").get(); // id
    String positionName = html.xpath("/html/body/div[2]/div[2]/div[2]/div/div[1]/h1//text()").get(); // 职位名
    String positionLink = request.getUrl();
    Selectable companyDom = html.xpath("/html/body/div[2]/div[2]/div[2]/div/div[1]/p[1]/a");
    String companyName = companyDom.xpath("a/text()").get(); // 公司名
    String companyLink = companyDom.links().get();
    String workPlace = html.xpath("/html/body/div[2]/div[2]/div[3]/div[5]/div/p/text()").get();
    String salary = html.xpath("/html/body/div[2]/div[2]/div[2]/div/div[1]/strong/text()").get();
    String industry = html.xpath("/html/body/div[2]/div[2]/div[2]/div/div[1]/p[2]/text()").get();
    String publishDate =
        html.xpath("/html/body/div[2]/div[2]/div[3]/div[1]/div/div/span[2]/text()").get();
    String positionDesc = html.xpath("/html/body/div[2]/div[2]/div[3]/div[4]/div/text()").get();

    try {

      if (positionId != null && !StringUtils.isEmpty(positionId)) {
        JSONObject json = new JSONObject();
        json.put("positionId", positionId);
        json.put("positionName", positionName);
        json.put("positionLink", positionLink);
        json.put("companyName", companyName);
        json.put("companyLink", companyLink);
        json.put("workPlace", workPlace);
        json.put("salary", salary);
        json.put("publishDate", publishDate);
        json.put("industry", industry);
        json.put("positionDesc", positionDesc);
        // 部分三: 如果启动时设置了pipeline 就需要到指定类处理抓取后的结果
        page.putField(RESULT, json);
      }

      // 移除已经扒取的url
      String url = request.getUrl();
      Position.removeSpideredUrl(url);

    } catch (Exception e) {
      LOGGER.error("获取页面失败 {}", request.getUrl(), e);
    }
  }


  public Site getSite() {
    return SpiderUtil.setSite(site);
  }


}
