package com.javastar920905.spider.pageprocessor.job51;


import java.util.List;

import com.javastar920905.spider.pipeline.job51.RedisJob51CompanyPipeLine;
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

import static com.javastar920905.spider.util.StringUtil.RESULT;

/**
 * Created by ouzhx on 2017/7/5.
 *
 * 扒取51job 公司详情页面(
 *
 *当前类已经被废弃,使用了其他实现方式
 */
@Deprecated
@Component
public class Job51CompanyPageProcessor extends Job51PositionUtil implements PageProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(Job51CompanyPageProcessor.class);
  // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
  private Site site = Site.me();
  private static Spider webMagicIOSpider = newInstance();


  private static Spider newInstance() {
    return Spider.create(new Job51CompanyPageProcessor()).thread(10)
        .addPipeline(new RedisJob51CompanyPipeLine()).setExitWhenComplete(true);
  }


  public static void runCompanySpider() {
    try {
      Thread.sleep(1000 * 20);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    while (true) {
      try {
        // 避免多次获取urls
        List requests = Company.getRequestList();
        LOGGER.info("redis 获取数据" + requests.size());

        if (requests != null && requests.size() > 0) {
          if (webMagicIOSpider.getStatus() != Spider.Status.Running) {
            LOGGER.info(" 开始新一轮url扒取  urlSize: {}  spider status: {}", requests.size(),
                webMagicIOSpider.getStatus());
            if (webMagicIOSpider.getStatus() == Spider.Status.Stopped) {
              webMagicIOSpider = newInstance();
              LOGGER.info(" 新spider  urlSize: {}  spider status: {}", requests.size(),
                  webMagicIOSpider.getStatus());
            }
            // 发起页面请求,开启5个线程并启动爬虫
            webMagicIOSpider.startRequest(requests);
            webMagicIOSpider.start();
          }
        }

        Thread.sleep(companySpiderSleepInterval);
      } catch (Exception e) {
        webMagicIOSpider = newInstance();
        LOGGER.info(" 公司详情扒取报错 ", e);
      }
    }

  }



  @Override
  public void process(Page page) {
    Request request = page.getRequest();
    Html html = page.getHtml();

    // 公司信息获取
    String companyId = html.xpath("//*[@id=\"hidCOID\"]").$("input", "value").get(); // id
    try {
      JSONObject positionJson = (JSONObject) request.getExtra("positionJson");
      if (companyId != null && !StringUtils.isEmpty(companyId)) {
        JSONObject json = new JSONObject();
        json.put("companyId", companyId);
        json.put("companyName", html.xpath("/html/body/div[2]/div[2]/div[2]/div/h1/text()").get()); // 公司名
        json.put("companyLogoLink",
            html.xpath("/html/body/div[2]/div[2]/div[2]/div/img").$("img", "src").get());
        json.put("workPlace",
            html.xpath("/html/body/div[2]/div[2]/div[3]/div[2]/div/p/text()").get());
        json.put("industry", html.xpath("/html/body/div[2]/div[2]/div[2]/div/p[1]/text()").get()); // 类型
        json.put("companyDesc",
            html.xpath("/html/body/div[2]/div[2]/div[3]/div[1]/div/div/div[1]/p/text()").get());
        json.put("positionJson", positionJson);
        page.putField(RESULT, json);
      }


      // 从redis 移除已经扒取的requst
      Company.removePositionListItem(positionJson);

    } catch (

    Exception e) {
      LOGGER.error("获取页面失败 {}", request.getUrl(), e);
    }
  }


  public Site getSite() {
    return SpiderUtil.setSite(site);
  }


}
