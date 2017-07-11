package com.javastar920905.spider.pageprocessor.job51;

import java.util.LinkedList;
import java.util.List;

import javax.management.JMException;

import com.javastar920905.spider.pipeline.job51.RedisJob51CompanyPipeLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.javastar920905.spider.config.RedisConfig;
import com.javastar920905.spider.pipeline.job51.RedisJob51PositionPipeLine;
import com.javastar920905.spider.util.Job51PositionUtil;
import com.javastar920905.spider.util.SpiderUtil;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.RedisScheduler;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

/**
 * Created by ouzhx on 2017/7/5.
 *
 * 扒取51job 公司详情页面(//TODO 扒取详情页前,需要先扒取所有列表页,找到所有职位url :Job51PositionListPageProcessor.java)
 *
 */
public class Job51CompanyPageProcessor extends Job51PositionUtil implements PageProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(Job51CompanyPageProcessor.class);
  // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
  private Site site = Site.me();
  private static List<String> urls = new LinkedList<>();

  public static void main(String[] args) {
    // spring 容器加载redis
    ConfigurableApplicationContext context =
        new AnnotationConfigApplicationContext(SpiderUtil.class, RedisConfig.class);

    // 发起页面请求,开启5个线程并启动爬虫
    Spider webMagicIOSpider = Spider.create(new Job51CompanyPageProcessor())
        .setScheduler(new RedisScheduler(RedisConfig.Host)).addRequest(getRequest(Company.fistPage))
        .thread(5).addPipeline(new RedisJob51CompanyPipeLine());

    // 将爬虫对象交给spring 托管
    context.getBeanFactory().registerSingleton("webMagicIOSpider", webMagicIOSpider);

    try {
      // 添加扒取数量监控
      SpiderMonitor.instance().register(webMagicIOSpider);
    } catch (JMException e) {
      e.printStackTrace();
    }
    SpiderUtil.currentWebMagicIOSpider = webMagicIOSpider;
    webMagicIOSpider.start();
  }



  // process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑
  @Override
  public void process(Page page) {
    // 部分二：定义如何抽取页面信息，并保存下来
    Request request = page.getRequest();
    Html html = page.getHtml();

    // 公司信息获取 (这里有多少个字段就相当于有多少个数组)
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
        // 部分三: 如果启动时设置了pipeline 就需要到指定类处理抓取后的结果
        page.putField(RESULT, json);
      }


      // 部分四：从页面发现后续的url地址来抓取
      if (urls.size() < 1) {
        synchronized (urls) {
          if (urls.size() < 1) {
            urls = Company.getUrls();
            page.addTargetRequests(urls);
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
