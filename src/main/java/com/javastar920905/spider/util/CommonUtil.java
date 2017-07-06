package com.javastar920905.spider.util;

import static com.javastar920905.spider.util.Job51PositionUtil.userAgent;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;

/**
 * Created by ouzhx on 2017/7/6.
 */
public class CommonUtil {
  public static Site setSite(Site site) {
    site.setRetryTimes(3).setSleepTime(100);
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
  public static Request getRequest(String aimUrl) {
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
}
