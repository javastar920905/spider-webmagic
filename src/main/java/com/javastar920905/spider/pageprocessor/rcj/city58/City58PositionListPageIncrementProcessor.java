package com.javastar920905.spider.pageprocessor.rcj.city58;



import com.javastar920905.spider.util.SpiderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

import static com.javastar920905.spider.util.StringUtil.RESULT;

/**
 * 58同城职位增量爬取
 *
 * Created by ouzhx on 2017/7/31.
 */
public class City58PositionListPageIncrementProcessor extends BaseCity58PositionProcessor
    implements PageProcessor {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(City58PositionListPageIncrementProcessor.class);
  // 抓取配置
  private Site site = SpiderUtil.setSite(Site.me());


  @Override
  public void process(Page page) {
    page.putField(RESULT, PositionList.dealPositionList(page.getHtml()));

    try {
      // 获取后续需要爬取的url
      page.addTargetRequests(PositionList.Increment.getIncreUrls());
    } catch (Exception e) {
      LOGGER.error("获取页面失败 {}", page.getUrl().get(), e);
    }
  }

  public Site getSite() {
    return this.site;
  }

}
