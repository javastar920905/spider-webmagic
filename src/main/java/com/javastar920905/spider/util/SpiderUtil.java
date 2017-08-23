package com.javastar920905.spider.util;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.utils.UrlUtils;

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

    // setMaxConnTotal设置连接池最大连接数 setMaxConnPerRoute 设置单个路由最大连接数
    private static CloseableHttpClient httpClient = null;
    // 设置5s超时
    private static int timeout = 3 * 1000;
    // 设置请求超时时间 .setProxy(new HttpHost("myotherproxy", 8080))
    private static RequestConfig config =
        RequestConfig.copy(RequestConfig.DEFAULT).setSocketTimeout(timeout)
            .setConnectTimeout(timeout).setConnectionRequestTimeout(timeout).build();

    static {
        HttpRequestRetryHandler myRetryHandler = new HttpRequestRetryHandler() {

            public boolean retryRequest(IOException exception, int executionCount,
                HttpContext context) {
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

    public static Site setSite(Site site) {
        // 该机制会将下载失败的url重新放入队列尾部重试，直到达到重试次数，以保证不因为某些网络原因漏抓页面。
        site.setRetryTimes(2).setCycleRetryTimes(2).setSleepTime(100);
        site.setUseGzip(true);
        site.setUserAgent(userAgent);
        // 设置Request Header，防止被ban
        site.addHeader("User-Agent", userAgent).addHeader("Accept", accept)
            .addHeader("Accept-Encoding", accept_encoding)
            .addHeader("Accept-Language", accept_language)
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

        return html;
    }



}
