package com.javastar920905.spider.pageprocessor.job51;

import static com.rencaijia.common.util.StringUtil.RESULT;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.rencaijia.spider.listener.UrlResultListener;
import com.rencaijia.spider.pipeline.job51.RedisJob51CompanyPipeLine;
import com.rencaijia.spider.util.Job51PositionUtil;
import com.rencaijia.spider.util.SpiderUtil;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.SpiderListener;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;

/**
 * Created by ouzhx on 2017/7/5.
 *
 * 扒取51job 公司详情页面(//TODO 扒取详情页前,需要先扒取所有列表页,找到所有职位url :Job51PositionListPageProcessor.java)
 *
 */
@Component
public class Job51CompanyPageProcessor extends Job51PositionUtil implements PageProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(Job51CompanyPageProcessor.class);
  // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
  private Site site = Site.me();
  public static List<String> urls = new Vector<>();
  private static Spider webMagicIOSpider = newInstance();


  private static Spider newInstance() {
    List<SpiderListener> spiderListenerList = new ArrayList<>();
    spiderListenerList.add(new UrlResultListener());
    return Spider.create(new Job51CompanyPageProcessor()).thread(5)
        .addPipeline(new RedisJob51CompanyPipeLine()).setExitWhenComplete(true)
        .setSpiderListeners(spiderListenerList);
  }

  public static void runCompanySpider() {

    while (true) {
      try {
        LOGGER.info("   urlSize: {}  spider status: {}", urls.size(), webMagicIOSpider.getStatus());

        // 避免多次获取urls
        if (CollectionUtils.isEmpty(urls)) {
          urls = Company.getUrls();
          LOGGER.info("redis 获取数据" + urls.size());
          if (CollectionUtils.isEmpty(urls)) {
            // url库中没有数据休息 5min
            Thread.sleep(1000 * 60 * 1);
          }
        }

        if (urls != null && urls.size() > 0) {
          if (webMagicIOSpider.getStatus() != Spider.Status.Running) {
            LOGGER.info(" 开始新一轮url扒取  urlSize: {}  spider status: {}", urls.size(),
                webMagicIOSpider.getStatus());
            if (webMagicIOSpider.getStatus() == Spider.Status.Stopped) {
              webMagicIOSpider = newInstance();
              LOGGER.info(" 新spider  urlSize: {}  spider status: {}", urls.size(),
                  webMagicIOSpider.getStatus());
            }
            // 发起页面请求,开启5个线程并启动爬虫
            webMagicIOSpider.startUrls(urls);
            webMagicIOSpider.start();
          }
        }

        Thread.sleep(1000 * 60 * 1);// 启动spider后 主线程没什么事了, 每两分钟循环检查一次数据库中的url
      } catch (Exception e) {
        webMagicIOSpider = newInstance();
        if (CollectionUtils.isEmpty(urls)) {
          urls = new LinkedList<>();
        }
        LOGGER.info(" 公司详情扒取报错 ", e);
        LOGGER.info(JSONObject.toJSONString(urls));
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
        page.putField(RESULT, json);
      }


      // 移除已经扒取的url
      String url = request.getUrl();
      if (urls.contains(url)) {
        synchronized (url) {
          urls.remove(url);
        }
        LOGGER.debug(" {}  ........... rest  number  ", urls.size());
      }


    } catch (Exception e) {
      LOGGER.error("获取页面失败 {}", request.getUrl(), e);
    }
  }


  public Site getSite() {
    return SpiderUtil.setSite(site);
  }


}
