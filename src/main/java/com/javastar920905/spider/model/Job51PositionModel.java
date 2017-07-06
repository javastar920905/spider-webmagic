package com.javastar920905.spider.model;

import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.model.ConsolePageModelPipeline;
import us.codecraft.webmagic.model.OOSpider;
import us.codecraft.webmagic.model.annotation.ExtractBy;
import us.codecraft.webmagic.model.annotation.TargetUrl;

/**
 * Created by ouzhx on 2017/7/5. 参考教程: http://webmagic.io/docs/zh/posts/ch5-annotation/
 *
 * 这是一个扒取51job 所有职位的demo
 */
@TargetUrl("") //详情页
@ExtractBy(value = "//ul[@id=\"promos_list2\"]/li",multi = true)
public class Job51PositionModel {
  @ExtractBy(value = "//h1[@class='entry-title public']/strong/a/text()", notNull = true)
  private String positionId;

  @ExtractBy(value = "//*[@id=\"resultList\"]/div[3]/p/span/a/text()", notNull = true)
  private String positionTitle;


  public static void main(String[] args) {
    String aimUrl =
        "http://search.51job.com/list/000000,000000,0000,00,9,99,%2520,2,1.html?lang=c&stype=&postchannel=0000&workyear=99&cotype=99&degreefrom=99&jobterm=99&companysize=99&providesalary=99&lonlat=0%2C0&radius=-1&ord_field=0&confirmdate=9&fromType=&dibiaoid=0&address=&line=&specialarea=00&from=&welfare=";

    OOSpider.create(Site.me().setSleepTime(1000), new ConsolePageModelPipeline(),
        Job51PositionModel.class).addUrl(aimUrl).thread(5).run();
  }
}
