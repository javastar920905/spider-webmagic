package com.javastar920905.spider.pageprocessor.job51;

import javax.management.JMException;

import com.javastar920905.spider.pipeline.job51.RedisJob51PositionPipeLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.alibaba.fastjson.JSONObject;
import com.javastar920905.spider.config.RedisConfig;
import com.javastar920905.spider.util.SpiderUtil;
import com.javastar920905.spider.util.Job51PositionUtil;

import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by ouzhx on 2017/7/5.
 *
 * 扒取51job 职位详情页面(//TODO 扒取详情页前,需要先扒取所有列表页,找到所有职位url :Job51PositionListPageProcessor.java)
 *
 */
public class Job51PositionPageProcessor extends Job51PositionUtil implements PageProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(Job51PositionPageProcessor.class);
  // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
  private Site site = Site.me();
  private static List<String> urls = new LinkedList<>();

  public static void main(String[] args) {
    // spring 容器加载redis
    ApplicationContext context =
        new AnnotationConfigApplicationContext(SpiderUtil.class, RedisConfig.class);

    // 启动spider 爬虫,没有托管给spring
    // 发起页面请求,开启5个线程并启动爬虫
    Spider webMagicIOSpider = Spider.create(new Job51PositionPageProcessor())
        // .setScheduler(new RedisScheduler(RedisConfig.Host))
        .addRequest(getRequest(Position.fistPage)).thread(5)
        .addPipeline(new RedisJob51PositionPipeLine());

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

    // 职位信息获取 (这里有多少个字段就相当于有多少个数组) TODO 这里可以优化一下直接放入json,避免产生临时变量
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
        page.putField("positionJson", json);
      }


      // 部分四：从页面发现后续的url地址来抓取
      if (urls.size() < 1) {
        synchronized (urls) {
          if (urls.size() < 1) {
            urls = Position.getUrls();
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
