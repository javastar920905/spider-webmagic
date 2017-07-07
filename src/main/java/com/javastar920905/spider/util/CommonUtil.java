package com.javastar920905.spider.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;

/**
 * Created by ouzhx on 2017/7/6.
 */
public class CommonUtil {
  public static String userAgent =
      "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36";
  public static String accept =
      "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
  public static String accept_encoding = "gzip, deflate, sdch";
  public static String accept_language = "zh-CN,zh;q=0.8";
  public static String cache_control = "max-age=0";
  public static String upgrade_request = "1";
  public static String connection_live = "keep-alive";

  public static Site setSite(Site site) {
    site.setRetryTimes(3).setSleepTime(100);
    site.setUseGzip(true);
    site.setUserAgent(userAgent);
    // 设置Request Header，防止被ban
    site.addHeader("User-Agent", userAgent).addHeader("Accept", accept)
        .addHeader("Accept-Encoding", accept_encoding).addHeader("Accept-Language", accept_language)
        .addHeader("Cache-Control:", cache_control).addHeader("Connection", connection_live)
        .addHeader("Upgrade-Insecure-Requests", upgrade_request);
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
    request.addHeader("Accept", accept);
    request.addHeader("Accept-Encoding", accept_encoding);
    request.addHeader("Accept-Language", accept_language);
    request.addHeader("User-Agent", userAgent);
    return request;
  }
}
