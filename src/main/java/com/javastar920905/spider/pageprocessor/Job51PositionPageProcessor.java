package com.javastar920905.spider.pageprocessor;

import static com.javastar920905.spider.util.CommonUtil.getRequest;

import java.util.List;

import javax.management.JMException;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.javastar920905.spider.pipeline.RedisPipeLine;
import com.javastar920905.spider.util.CommonUtil;
import com.javastar920905.spider.util.Job51PositionUtil;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;

/**
 * Created by ouzhx on 2017/7/5. 扒取51job 职位列表(注解方式不好用,还是这个模式实在)
 *
 */
public class Job51PositionPageProcessor extends Job51PositionUtil implements PageProcessor {
  // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
  private Site site = Site.me();
  private static String historyAreaNumber;

  private static String getCurrentAreaNumber(String url) {
    if (url == null) {
      return null;
    }
    String currentAreaNumber = url.substring(url.lastIndexOf("/") + 1, url.indexOf(","));
    System.out.println(currentAreaNumber + " ===================>  :" + historyAreaNumber);
    return currentAreaNumber;
  }



  // process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑
  public void process(Page page) {
    // 部分二：定义如何抽取页面信息，并保存下来
    Request request = page.getRequest();
    Html html = page.getHtml();

    // positionlistBox
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
    if (positionIdList != null) {
      for (; i < positionIdList.size(); i++) {
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

    page.putField("positionJsonArray", positionJsonArray);


    // 部分三：从页面发现后续的url地址来抓取
    String currentNum = getCurrentAreaNumber(request.getUrl());
    if (currentNum != null && !currentNum.equals(historyAreaNumber))
      historyAreaNumber = currentNum;
    page.addTargetRequests(getUrls(html, currentNum));
  }


  public Site getSite() {
    return CommonUtil.setSite(site);
  }



  public static void main(String[] args) {
    // 发起页面请求,开启5个线程并启动爬虫 // 输出到文件,每次抓取会覆盖掉原来的文件
    Spider webMagicIOSpider =
        Spider.create(new Job51PositionPageProcessor()).addRequest(getRequest(fistPositionPage))
            .addPipeline(new JsonFilePipeline("D:/webmgicData/")).addPipeline(new RedisPipeLine())
            .thread(5);

    try { // 添加扒取数量监控
      SpiderMonitor.instance().register(webMagicIOSpider);
    } catch (JMException e) {
      e.printStackTrace();
    }
    webMagicIOSpider.start();
  }

}
