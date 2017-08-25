package com.javastar920905.spider.util;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import javax.net.ssl.SSLException;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.javastar920905.spider.util.api.dama.DamaOpsUtil;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.proxy.SimpleProxyProvider;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.utils.ProxyUtils;
import us.codecraft.webmagic.utils.UrlUtils;

/**
 * Created by ouzhx on 2017/7/6.
 */
public class SpiderUtil {
  private static Logger LOGGER = LoggerFactory.getLogger(SpiderUtil.class);

  public static String userAgent =
      "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36";
  public static String accept =
      "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
  public static String accept_encoding = "gzip, deflate, sdch";
  public static String accept_language = "zh-CN,zh;q=0.8";
  public static String cache_control = "max-age=0";
  public static String upgrade_request = "1";
  public static String connection_live = "keep-alive";

  // setMaxConnTotal设置连接池最大连接数 setMaxConnPerRoute 设置单个路由最大连接数
  private static CloseableHttpClient httpClient = null;
  // 设置5s超时
  private static int timeout = 5 * 1000;
  // 设置请求超时时间 .setProxy(new HttpHost("myotherproxy", 8080))
  private static RequestConfig config =
      RequestConfig.copy(RequestConfig.DEFAULT).setSocketTimeout(timeout).setConnectTimeout(timeout)
          .setConnectionRequestTimeout(timeout).build();
  private static HttpRequestRetryHandler myRetryHandler = null;

  static {
    myRetryHandler = new HttpRequestRetryHandler() {

      public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        if (executionCount >= 3) {
          // 更新代理
          updateHttpClientProxy();
        }
        if (executionCount >= 5) {
          HttpClientContext clientContext = HttpClientContext.adapt(context);
          HttpRequest request = clientContext.getRequest();
          System.out.println("重试请求超过限定次数,放弃重试: " + request.getRequestLine().getUri());
          // Do not retry if over max retry count
          return false;
        }
        if (exception instanceof InterruptedIOException) {
          // Timeout
          return true;
        }
        if (exception instanceof NoHttpResponseException) {
          // 没有响应重试
          return true;
        }
        if (exception instanceof UnknownHostException) {
          // Unknown host
          return false;
        }
        if (exception instanceof ConnectTimeoutException) {
          // Connection refused
          return true;
        }
        if (exception instanceof SSLException) {
          // SSL handshake exception
          return false;
        }
        HttpClientContext clientContext = HttpClientContext.adapt(context);
        HttpRequest request = clientContext.getRequest();
        boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
        if (idempotent) {
          // Retry if the request is considered idempotent
          return true;
        }
        return false;
      }

    };

    httpClient = HttpClients.custom().setMaxConnTotal(200).setMaxConnPerRoute(100)
        .setUserAgent(userAgent).setRetryHandler(myRetryHandler).build();

  }

  // 从讯代理获取代理ip资源
  public static int retry = 0;

  public static void retryGetProxy() {
    retry++;
    if (retry < 3) {
      getProxy();
    } else {
      retry = 0;// 跳过本次调用
    }
  }

  public static JSONObject getProxy() {
    CloseableHttpClient client = HttpClients.createDefault();
    HttpGet get = new HttpGet(
        "http://www.xdaili.cn/ipagent//newExclusive/getIp?spiderId=a281f7b266e049cfb65862c1203374b6&orderno=MF201782366170cIkYn&returnType=2&count=1&machineArea=");

    JSONObject jsonProxyObject = null;
    try {
      CloseableHttpResponse response = client.execute(get);
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        JSONObject resultJson = JSONObject.parseObject(response.getEntity().getContent(), null);
        if ("0".equals(resultJson.get("ERRORCODE"))) {
          JSONArray array = resultJson.getJSONArray("RESULT");
          if (array != null && array.size() > 0) {
            jsonProxyObject = (JSONObject) array.get(0);
            System.out.println("====================>获取到的代理ip :" + array.toJSONString());
          } else {
            retryGetProxy();
          }
        } else {
          System.err.println("获取代理ip失败 :" + resultJson.toJSONString());
          JSONObject freeProxy[] = new JSONObject[3];
          JSONObject jsonObject = new JSONObject();
          jsonObject.put("ip", "27.153.128.91");
          jsonObject.put("port", "30213");
          freeProxy[0] = jsonObject;
          JSONObject jsonObject1 = new JSONObject();
          jsonObject1.put("ip", "114.229.35.218");
          jsonObject1.put("port", "30069");
          freeProxy[1] = jsonObject;
          JSONObject jsonObject2 = new JSONObject();
          jsonObject2.put("ip", "125.112.205.249");
          jsonObject2.put("port", "25033");
          freeProxy[2] = jsonObject;
          Random random = new Random();
          jsonProxyObject = freeProxy[random.nextInt(3)];
        }
      }
      response.close();
      get.releaseConnection();
    } catch (IOException e) {
      e.printStackTrace();
    }


    if (jsonProxyObject != null) {
      Proxy proxy = new Proxy(jsonProxyObject.getString("ip"), jsonProxyObject.getInteger("port"));
      if (ProxyUtils.validateProxy(proxy) == false) {
        retryGetProxy();
      }
    }

    return jsonProxyObject;
  }

  // 更新httpClient对象的代理
  public static void updateHttpClientProxy() {
    JSONObject jsonObject = getProxy();
    if (jsonObject != null) {
      String ip = jsonObject.getString("ip");
      Integer port = jsonObject.getInteger("port");

      // 更新代理
      HttpHost proxy = new HttpHost(ip, port);
      DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
      httpClient =
          HttpClients.custom().setMaxConnTotal(200).setMaxConnPerRoute(100).setUserAgent(userAgent)
              .setRetryHandler(myRetryHandler).setRoutePlanner(routePlanner).build();
    }

  }



  public static Site setSite(Site site) {
    // 该机制会将下载失败的url重新放入队列尾部重试，直到达到重试次数，以保证不因为某些网络原因漏抓页面。
    site.setRetryTimes(2).setCycleRetryTimes(2).setSleepTime(100);
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
    return request;
  }



  /**
   * 使用apache httpclient api抓取网页 并封装成 webmagic html对象
   *
   * @param aimUrl 要抓取页面的url
   * @return
   * @throws Exception
   */
  public static Html captureHtml(String aimUrl, String charset) {
    if (aimUrl == null) {
      return null;
    }
    Html html = null;
    try {
      HttpGet get = new HttpGet(aimUrl);
      // get.addHeader("User-Agent", userAgent);
      get.setConfig(config);
      CloseableHttpResponse response = httpClient.execute(get);
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        String webPageString = EntityUtils.toString(response.getEntity(), charset);
        html = new Html(webPageString, aimUrl);
      }
      response.close();
      get.releaseConnection();
    } catch (IOException e) {
      System.err.println("抓取页面信息失败 ! " + aimUrl);
      e.printStackTrace();
    }
    if (aimUrl.contains(".58.com/")) {
      if (spiderValidCode.deal58CityVerifyCode(html)) {
        html = captureHtml(aimUrl, charset);
      }
    }

    return html;
  }


  public static class SpiderProxy {
    // public static String ip = "222.95.21.191";
    // public static int port = 808;
    // 更新webmagic对象的代理
    public static Spider city58Spider;
    private static Queue<HttpHost> queue = new LinkedList<>();

    /**
     * 扒取讯代理的免费代理ip列表
     *
     * @return
     */
    public static void getProxyList() {
      CloseableHttpClient client = HttpClients.createDefault();
      String aimUrl = "http://www.xdaili.cn/ipagent//freeip/getFreeIps?page=1&rows=10";
      HttpGet get = new HttpGet(aimUrl);

      try {
        CloseableHttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == org.apache.http.HttpStatus.SC_OK) {
          JSONObject resultJson = JSONObject.parseObject(response.getEntity().getContent(), null);
          if ("0".equals(resultJson.get("ERRORCODE"))) {
            JSONArray array = resultJson.getJSONObject("RESULT").getJSONArray("rows");
            for (Object object : array) {
              JSONObject json = JSONUtil.parseObjectToJSONObject(object, null);
              HttpHost httpHost = new HttpHost(json.getString("ip"), json.getIntValue("port"));
              queue.offer(httpHost);
            }
          }
        }
        response.close();
        get.releaseConnection();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }


    /**
     * 验证58同城是否出现验证码
     *
     * @param html 要验证的页面
     * @return
     */
    public static boolean isValidCodeShow(Html html) {
      String title = "";
      if (html != null) {
        title = html.css("title").xpath("title/text()").get();
      }
      if (html == null || title.equals("请输入验证码")) {
        if (queue.size() < 1) {
          synchronized (queue) {
            if (queue.size() < 1) {
              getProxyList();
            }
          }
        }
        HttpHost host = queue.poll();
        LOGGER.info("++++++++++++++++++ OMG! 58同城,出现验证码了*_*,换个ip试试: " + host.toHostString());
        if (host != null) {
          updateHttpClientProxy(host);
          if (city58Spider != null) {
            updateSpiderProxy(host);
          }
        }
        return true;
      }
      return false;
    }


    // 更新httpClient对象的代理
    public static void updateHttpClientProxy(HttpHost proxy) {
      DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
      httpClient =
          HttpClients.custom().setMaxConnTotal(200).setMaxConnPerRoute(100).setUserAgent(userAgent)
              .setRetryHandler(myRetryHandler).setRoutePlanner(routePlanner).build();
    }

    public static void updateSpiderProxy(HttpHost host) {
      Proxy proxy1 = new Proxy(host.getHostName(), host.getPort());
      if (ProxyUtils.validateProxy(proxy1)) {
        HttpClientDownloader httpClientDownloader = new HttpClientDownloader();
        httpClientDownloader.setProxyProvider(SimpleProxyProvider.from(proxy1));
        city58Spider.setDownloader(httpClientDownloader);
      }
    }
  }

  // 验证码解析类
  public static class spiderValidCode {

    /**
     * 处理验证58同城验证码
     *
     * @param html 要验证的页面
     * @param
     * @return
     */
    public static boolean deal58CityVerifyCode(Html html) {
      String title = "";
      if (html != null) {
        title = html.css("title").xpath("title/text()").get();
      }
      if (title.equals("请输入验证码")) {
        System.out.println("++++++++++++++++++ OMG! 58同城,出现验证码了*_* ");
        String uuid = html.css("#uuid").$("input", "value").get();

        // 1 下载验证码
        String validCodeUrl = html.css("#verify_img").$("img", "src").get();
        String uri = validCodeUrl.substring(7);
        validCodeUrl = "http://callback.58.com/firewall/code/" + uri;
        // uuid + ".png"
        byte[] imgByteData =
            ImageSpider.downLoadFromUrl(validCodeUrl, "verify58Code.png");
        // 2 提交打码兔接口,获取验证码识别结果
        String verify_code = DamaOpsUtil.getValidateCode(imgByteData,42);
        // 3 提交验证码到58
        String url = html.css("#url").$("input", "value").get();
        String namespace = html.css("#namespace").$("input", "value").get();
        String verify58Url = "http://callback.58.com/firewall/valid/455480577.do?namespace="
            + namespace + "&url=" + url;

        List<NameValuePair> formparams = new ArrayList<>();
        formparams.add(new BasicNameValuePair("uuid", uuid));
        formparams.add(new BasicNameValuePair("namespace", namespace));
        formparams.add(new BasicNameValuePair("url", url));
        formparams.add(new BasicNameValuePair("verify_code", verify_code));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
        String result = doPostRequest(verify58Url, entity);

        // 4 获取58验证码验证结果
        JSONObject parsedJson = JSONObject.parseObject(result);
        if (parsedJson.getIntValue("code") == 0) {
          LOGGER.info("++++++++++++++++++  验证结果: {}", parsedJson.getString("msg"));
        } else {
          System.err.println("验证码验证失败!  " + parsedJson.getString("msg"));
          return true;
        }
      }
      return false;
    }
  }

  public static String doPostRequest(String aimUrl, HttpEntity entity) {
    CloseableHttpClient client = HttpClients.createDefault();
    HttpPost post = new HttpPost(aimUrl);
    if (entity != null) {
      post.setEntity(entity);
    }

    try {
      CloseableHttpResponse response = client.execute(post);
      if (response.getStatusLine().getStatusCode() == org.apache.http.HttpStatus.SC_OK) {
        String webPageString = EntityUtils.toString(response.getEntity(), "utf-8");
        return webPageString;
      }
      response.close();
      post.releaseConnection();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

}
