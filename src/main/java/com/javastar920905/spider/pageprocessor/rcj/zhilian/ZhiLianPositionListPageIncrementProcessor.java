package com.javastar920905.spider.pageprocessor.rcj.zhilian;



import static com.javastar920905.spider.util.StringUtil.RESULT;

import com.javastar920905.spider.pipeline.city58.ESCity58PositionPipeLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;

import com.javastar920905.spider.pipeline.zhilian.RedisZhilianPositionListPipeLine;
import com.javastar920905.spider.util.RedisOpsUtil;
import com.javastar920905.spider.util.SpiderUtil;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;

/**
 * 智联招聘职位增量爬取
 * <p>
 * Created by ouzhx on 2017/7/31.
 */
public class ZhiLianPositionListPageIncrementProcessor extends BaseZhiLianPositionProcessor
    implements PageProcessor {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ZhiLianPositionListPageIncrementProcessor.class);
  // 部分一：抓取网站的相关配置
  private Site site = Site.me();


  public Site getSite() {
    return SpiderUtil.setSite(site);
  }

  // 创建一个新的spider对象
  private static Spider newInstance() {
    return Spider.create(new ZhiLianPositionListPageIncrementProcessor())
        .addRequest(getRequest(BaseZhiLianPositionProcessor.PositionList.Increment.fistPage))
        .addPipeline(new ESCity58PositionPipeLine()).thread(50);
  }

  // 提供接口用于启动当前spider
  public static void runIncrementSpider() {
    LOGGER.info("zhilian runIncrementSpider begin! ...");
    // 发起页面请求,开启5个线程并启动爬虫
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
      // 部分四：获取后续需要爬取的url
      page.addTargetRequests(BaseZhiLianPositionProcessor.PositionList.Increment.getIncreUrls());
    } catch (Exception e) {
      LOGGER.error("获取页面失败 {}", request.getUrl(), e);
    }
  }



}
