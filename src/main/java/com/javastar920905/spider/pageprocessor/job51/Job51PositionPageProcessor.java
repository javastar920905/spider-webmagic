package com.javastar920905.spider.pageprocessor.job51;

import java.util.List;

import com.javastar920905.spider.pipeline.job51.RedisJob51PositionPipeLine;
import com.javastar920905.spider.util.SpiderConstantKey;
import com.javastar920905.spider.util.SpiderUtil;
import com.javastar920905.spider.util.StringUtil;
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

import static com.javastar920905.spider.util.SpiderConstantKey.COMPANY_JSON;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.COMPANY_ADDRESS;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.LOGO;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.NAME;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.OFFICE_ENVIRONMENT;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.PROFILE;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.SOURCE;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.WEBSITE;
import static com.javastar920905.spider.util.SpiderConstantKey.ID;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.COMPANY_LINK;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.COMPANY_NAME;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.DEGREE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.JOB_DESC;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.LABEL_LIST;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_LINK;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_NAME;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_TITLE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.PUBLISHED_DATE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.SALARY;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.WELFARE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.WORKPLACE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.WORK_EXPERIENCE;
import static com.javastar920905.spider.util.StringUtil.RESULT;

/**
 * Created by ouzhx on 2017/7/5.
 *
 * 扒取51job 职位详情页面
 *
 */
@Component
@Deprecated
public class Job51PositionPageProcessor extends BaseJob51PositionProcessor implements PageProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(Job51PositionPageProcessor.class);
  private Site site = Site.me();
  private static Spider webMagicIOSpider = newPositionSpiderInstance();



  /*
   * public static void main(String[] args) {
   * webMagicIOSpider.addUrl("http://jobs.51job.com/kunming-whq/92169260.html?s=01&t=0");
   * webMagicIOSpider.start(); }
   */



  // spider 调用一次start()方法后,再次设置url会报错,所以需要重新获取新的spider实例
  private static Spider newPositionSpiderInstance() {
    return Spider.create(new Job51PositionPageProcessor()).thread(threadNumber)
        .addPipeline(new RedisJob51PositionPipeLine()).setExitWhenComplete(true)
        .setSpiderListeners(LISTENERS);
  }

  public static void runPositionSpider() {
    try {
      Thread.sleep(1000 * 30);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    while (true) {
      try {
        // 获取前n条url(默认500)
        List urls = Position.getUrls();
        LOGGER.info("redis 获取职位数据" + urls.size());

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

    // 职位信息获取
    String positionId = html.xpath("//*[@id=\"hidJobID\"]").$("input", "value").get(); // id
    String positionLink = request.getUrl();
    Selectable companyDom = html.xpath("/html/body/div[2]/div[2]/div[2]/div/div[1]/p[1]/a");
    String companyName = companyDom.xpath("a/text()").get(); // 公司名
    String companyLink = companyDom.links().get();
    String industry = html.xpath("/html/body/div[2]/div[2]/div[2]/div/div[1]/p[2]/text()").get();
    List<String> requirements =
        html.xpath("/html/body/div[2]/div[2]/div[3]/div[1]/div/div/span/text()").all();
    String workExp = null, degree = null, peopleCount = null, publishDate = null;
    for (String element : requirements) {
      if (element.contains("经验")) {
        workExp = element;
      } else if (element.contains("招聘")) {
        // peopleCount = element;
      } else if (element.contains("发布")) {
        publishDate = element.substring(0, element.indexOf("发布"));
      } else {
        degree = element;
      }
    }

    String positionDesc = html.xpath("/html/body/div[2]/div[2]/div[3]/div[4]/div/text()").get();

    try {
      if (positionId != null && !StringUtils.isEmpty(positionId)) {
        JSONObject json = new JSONObject();
        json.put(ID, positionId);
        json.put(POSITION_NAME,
            html.xpath("/html/body/div[2]/div[2]/div[2]/div/div[1]/h1/text()").get());// 职位名
        json.put(POSITION_TITLE,
            html.css(
                "div.tCompanyPage > div.tCompany_center.clearfix > div.tCompany_main > div:nth-child(4) > div > div.mt10 > p:nth-child(1) > span.el")
                .xpath("span/text()").all());// 工作职能
        json.put(WELFARE,
            html.xpath("/html/body/div[2]/div[2]/div[3]/div[1]/div/p/span/text()").all());// 福利标签
        json.put(LABEL_LIST,
            html.css(
                "div.tCompanyPage > div.tCompany_center.clearfix > div.tCompany_main > div:nth-child(4) > div > div.mt10 > p:nth-child(2) > span.el")
                .xpath("span/text()").all());// 关键字(有关键字就一定有工作职能)
        json.put(POSITION_LINK, positionLink);
        json.put(WORK_EXPERIENCE, workExp);// 工作年限
        json.put(DEGREE, degree);// 学历
        json.put(PUBLISHED_DATE, publishDate);
        json.put(COMPANY_NAME, companyName);
        json.put(COMPANY_LINK, companyLink);
        json.put(WORKPLACE,
            html.xpath("/html/body/div[2]/div[2]/div[3]/div[5]/div/p/text()").get());
        json.put(SALARY,
            html.xpath("/html/body/div[2]/div[2]/div[2]/div/div[1]/strong/text()").get());
        // 处理行业 公司规模 公司性质等信息
        dealIndustryInfo(industry, json);
        json.put(JOB_DESC, positionDesc);
        // 抓取公司详情
        if (StringUtil.isNotEmpty(companyLink)) {
          json.put(COMPANY_JSON, dealCompanyJson(captureHtml(companyLink)));
        }
        // 部分三: 如果启动时设置了pipeline 就需要到指定类处理抓取后的结果
        page.putField(RESULT, json);
      }
    } catch (Exception e) {
      LOGGER.error("获取页面失败 {}", request.getUrl(), e);
    }
  }

  public static JSONObject dealCompanyJson(Html html) {
    // 公司信息获取
    String companyId = html.xpath("//*[@id=\"hidCOID\"]").$("input", "value").get(); // id
    if (companyId != null && !StringUtils.isEmpty(companyId)) {
      JSONObject json = new JSONObject();
      json.put(ID, companyId);
      json.put(NAME, html.xpath("/html/body/div[2]/div[2]/div[2]/div/h1/text()").get()); // 公司名
      json.put(LOGO, html.xpath("/html/body/div[2]/div[2]/div[2]/div/img").$("img", "src").get());
      json.put(COMPANY_ADDRESS,
          html.xpath("/html/body/div[2]/div[2]/div[3]/div[2]/div/p/text()").get());
      String companyIndustryString =
          html.xpath("/html/body/div[2]/div[2]/div[2]/div/p[1]/text()").get();
      dealIndustryInfo(companyIndustryString, json);
      json.put(PROFILE,
          html.xpath("/html/body/div[2]/div[2]/div[3]/div[1]/div/div/div[1]/p/text()").get());// 公司简介
                                                                                              // 描述
      json.put(WEBSITE, html.xpath("/html/body/div[2]/div[2]/div[3]/div[3]/div/p/a/text()").get());// 官网
      json.put(OFFICE_ENVIRONMENT,
          html.xpath("//*[@id=\"divCoPoster\"]/ul/li/a/img").$("img", "src").all());// 办公环境
      json.put(SOURCE, "51job");// 数据来源
      return json;
    }
    return null;
  }

  // 处理行业 公司规模 和企业性质信息
  private static void dealIndustryInfo(String industryString, JSONObject json) {
    String[] industryArr = industryString.split("\\|");
    if (industryArr != null) {
      String nature = null, scale = null, industry = null;
      for (String element : industryArr) {
        if (element.contains("人")) {
          scale = element;
        } else if (element.contains("公司") || element.contains("资") || element.contains("企")
            || element.contains("机") || element.contains("单位")) {
          nature = element;
        } else {
          industry = element;
        }
      }
      json.put(SpiderConstantKey.Company.COMPANY_NATURE, nature); // 公司类型
      json.put(SpiderConstantKey.Company.COMPANY_SCALE, scale); // 规模
      json.put(SpiderConstantKey.Company.INDUSTRY, industry); // 公司行业
    }
  }


  public Site getSite() {
    return SpiderUtil.setSite(site);
  }


}
