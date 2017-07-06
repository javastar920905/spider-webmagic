package com.javastar920905.spider.pageprocessor;

import java.util.List;

import javax.management.JMException;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

public class WebmgicPageProcessor implements PageProcessor {
  private Site site = Site.me().setRetryTimes(3).setRetrySleepTime(5);

  public Site getSite() {
    return site;
  }

  public void process(Page page) {
    // 这里主要是对xpath 的要求较高
    Html html = page.getHtml();// 获取当前页面
    Selectable rows=html.css(".summary li a");//获取一个列表
    page.putField("summary", rows.xpath("a/text()").all()); //获取列表中所有a标签中的内容
    page.putField("links",rows.$("a","href").all()); //获取所有a标签的href属性, 返回一个list<String>
  }


  public static void main(String[] args) {
    // 获取当前网站的所有title 并输出到文件,每次抓取会覆盖掉原来的文件
    Spider webMagicIOSpider =
        Spider.create(new WebmgicPageProcessor()).addUrl("http://webmagic.io/docs/zh/")
            .addPipeline(new JsonFilePipeline("D:/webmgicData/")).thread(5);

    try {
      // 添加扒取数量监控
      SpiderMonitor.instance().register(webMagicIOSpider);
    } catch (JMException e) {
      e.printStackTrace();
    }
    webMagicIOSpider.start();

  }

}
