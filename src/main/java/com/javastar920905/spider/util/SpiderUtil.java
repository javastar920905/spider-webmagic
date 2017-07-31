package com.javastar920905.spider.util;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.utils.UrlUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by ouzhx on 2017/7/6.
 */
public class SpiderUtil {

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

  public static Request getRequest(String aimUrl) {
    return new Request(aimUrl);
  }

  /**
   * 使用java.net.URL api抓取网页 并封装成 webmagic html对象
   *
   * @param aimUrl 要抓取页面的url
   * @return
   * @throws Exception
   */
  public static Html captureHtml(String aimUrl) {
    if (aimUrl == null) {
      return null;
    }
    Html html = null;
    try {
      URL url = new URL(aimUrl);
      HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
      InputStreamReader input = new InputStreamReader(httpConn.getInputStream(), "gb2312");
      BufferedReader bufReader = new BufferedReader(input);
      String line = "";
      StringBuilder contentBuf = new StringBuilder();
      while ((line = bufReader.readLine()) != null) {
        contentBuf.append(line);
      }
      String webPageString = contentBuf.toString();
      html = new Html(webPageString, aimUrl);
    } catch (IOException e) {
      System.err.println("抓取页面信息失败 ! " + aimUrl);
    }
    return html;
  }
}
