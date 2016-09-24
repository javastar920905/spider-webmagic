package com.javastar920905.spider.pageprocessor;

import java.util.ArrayList;
import java.util.List;

import javax.management.JMException;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;

public class WebmgicPageProcessor implements PageProcessor {
	private Site site=Site.me().setRetryTimes(3).setRetrySleepTime(5);

	public Site getSite() {
		return site;
	}

	public void process(Page page) {
		//这里主要是对xpath 的要求较高
		List<String> sumarry=page.getHtml().xpath("/html/body/div/div/ul/li//a/text()").all();
		for (String s:sumarry) {
			System.out.println(s);
		}
		page.putField("summary", sumarry);
		
		//添加另外的请求地址 ,会多调用两次该抓取方法,只是请求页面不同而已
		List<String> urlList=new ArrayList<>();
		urlList.add("http://webmagic.io/docs/zh/posts/ch1-overview/README.html");
		urlList.add("http://webmagic.io/docs/zh/posts/ch2-install/README.html");
		page.addTargetRequests(urlList);

	}
	
	public static void main(String[] args) {
		//获取当前网站的所有title  并输出到文件,每次抓取会覆盖掉原来的文件
		Spider webMagicIOSpider= Spider.create(new WebmgicPageProcessor()).addUrl("http://webmagic.io/docs/zh/")
		.addPipeline(new JsonFilePipeline("D:/eclipse_workspace/webmgicData/")).thread(5);
		
		try {
			//添加扒取数量监控
			SpiderMonitor.instance().register(webMagicIOSpider);
		} catch (JMException e) {
			e.printStackTrace();
		}
		webMagicIOSpider.start();
		
	}

}
