package com.javastar920905.spider.listener;

import com.alibaba.fastjson.JSONObject;
import com.javastar920905.spider.util.SpiderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.SpiderListener;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.proxy.SimpleProxyProvider;
import us.codecraft.webmagic.utils.ProxyUtils;


/**
 * 爬虫代理监听器
 */
public class UpdateProxyListener implements SpiderListener {
  private Logger logger = LoggerFactory.getLogger(UpdateProxyListener.class);
  private Spider spider;

  /**
   * 将spider 对象注入当前监听器的方式
   * 
   * Spider city58Spider =Spider.create(xx).addUrl(xx).addPipeline(xx).thread(50);
   * 
   * // 将当期city58Spider对象注入 Listener中 List<SpiderListener> spiderListenerList = new ArrayList<>();
   * spiderListenerList.add(new UpdateProxyListener(city58Spider));
   * city58Spider.setSpiderListeners(spiderListenerList);
   * 
   * //最后启动spider
   *
   * city58Spider.start();
   * 
   * @param spider
   */
  public UpdateProxyListener(Spider spider) {
    this.spider = spider;
  }

  @Override
  public void onSuccess(Request request) {
    // do nothing
  }

  // 爬虫出错的时候切换IP
  @Override
  public void onError(Request request) {
    logger.info("=====不知道怎么回事, 突然间想换个IP(URL:" + request.getUrl() + ")=====");
    updateSpiderProxy(spider);
  }

  // 更新当前spider的代理
  public static void updateSpiderProxy(Spider spider) {
    JSONObject jsonObject = SpiderUtil.SpiderProxy.getProxy();
    if (jsonObject != null) {
      Proxy proxy = new Proxy(jsonObject.getString("ip"), jsonObject.getInteger("port"));
      if (ProxyUtils.validateProxy(proxy)) {
        HttpClientDownloader httpClientDownloader = new HttpClientDownloader();
        httpClientDownloader.setProxyProvider(SimpleProxyProvider.from(proxy));
        spider.setDownloader(httpClientDownloader);
      }
    }

  }
}
