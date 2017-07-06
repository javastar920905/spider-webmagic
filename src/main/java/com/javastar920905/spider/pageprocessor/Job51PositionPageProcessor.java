package com.javastar920905.spider.pageprocessor;

import java.util.LinkedList;
import java.util.List;

import javax.management.JMException;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.javastar920905.spider.pipeline.RedisPipeLine;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;

/**
 * Created by ouzhx on 2017/7/5. 扒取51job 职位列表
 *
 */
public class Job51PositionPageProcessor implements PageProcessor {
  // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
  private Site site = Site.me().setRetryTimes(3).setSleepTime(100);
  private static String pageUrl = "http://search.51job.com/list/000000,000000,0000,00,9,99,%2B,2,";
  // 根据发布时间排序
  private static String queryString =
      "?lang=c&stype=1&postchannel=0000&workyear=99&cotype=99&degreefrom=99&jobterm=99&companysize=99&lonlat=0%2C0&radius=-1&ord_field=1&confirmdate=9&fromType=&dibiaoid=0&address=&line=&specialarea=00&from=&welfare=";
  private static String pageUrl_suffix = ".html" + queryString;
  private static String userAgent =
      "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36";

  // process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑
  public void process(Page page) {
    // 部分二：定义如何抽取页面信息，并保存下来
    Request initRequest = page.getRequest();
    Html html = page.getHtml();

    // positionlistBox
    String parentCss = "#resultList .el ";

    // 职位信息获取
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
    // page.addTargetRequests(getUrls(html));
  }

  // <editor-fold desc="获取所有目标页面的url">
  /**
   * 获取所有目标页面的url
   * 
   * 分析分页 url<a href="http://search.51job.com/list/000000,000000,0000,00,9,99,%2B,2,2.html">
   *
   * 发现只有末尾的2,*.html 会产生变化,所以我们可以for循环生成 PageSize(获取分页数 )*每页显示条目的url
   *
   * (51job有2000页职位(而且只显示最新的2000页),一次性扒取不完,需要记录扒取的当前页数)
   * 
   * @param html
   * @return
   */
  private List<String> getUrls(Html html) {
    int pageSize = getPageSize(html);
    int i = 2;
    // 第一页已经在request中请求了,<=pageSize页需要请求
    List<String> urls = new LinkedList();
    for (; i <= pageSize; i++) {
      urls.add(pageUrl + i + pageUrl_suffix);
    }
    return urls;
  }

  private int getPageSize(Html html) {
    // 原内容为 "共2000页，到第"
    String pageSizeStr = html.xpath("/html/body/div[2]/div[5]/div/div/div/span[1]/text()").get();
    if (pageSizeStr != null) {
      // 截取"共"和"页"之间的数值
      return Integer
          .valueOf(pageSizeStr.substring(pageSizeStr.indexOf("共") + 1, pageSizeStr.indexOf("页")));
    }
    return 1;
  }
  // </editor-fold>

  // <editor-fold desc="站点和请求头配置">
  public Site getSite() {
    site.setUseGzip(true);
    site.setUserAgent(userAgent);
    site.addHeader("User-Agent", userAgent);
    return site;
  }

  /**
   * 必须设置User-Agent请求头信息[默认请求手机版的页面,数据格式不一致]
   * 
   * @return
   */
  private static Request getRequest() {
    String aimUrl = pageUrl + 1 + pageUrl_suffix;
    // 使用360浏览器的请求头
    Request request = new Request(aimUrl);
    request.setMethod("get");
    request.addHeader("Accept",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
    request.addHeader("Accept-Encoding", "gzip, deflate, sdch");
    request.addHeader("Accept-Language", "zh-CN,zh;q=0.8");
    request.addHeader("Host", "search.51job.com");
    request.addHeader("User-Agent", userAgent);
    return request;
  }
  // </editor-fold>



  public static void main(String[] args) {
    // 发起页面请求,开启5个线程并启动爬虫
    // 输出到文件,每次抓取会覆盖掉原来的文件
    Spider webMagicIOSpider = Spider.create(new Job51PositionPageProcessor())
        .addRequest(getRequest()).addPipeline(new JsonFilePipeline("D:/webmgicData/"))
        .addPipeline(new RedisPipeLine()).thread(5);

    try {
      // 添加扒取数量监控
      SpiderMonitor.instance().register(webMagicIOSpider);
    } catch (JMException e) {
      e.printStackTrace();
    }
    webMagicIOSpider.start();
  }
}
