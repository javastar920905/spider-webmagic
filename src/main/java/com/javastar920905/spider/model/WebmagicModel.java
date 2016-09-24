package com.javastar920905.spider.model;

import java.util.List;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.model.ConsolePageModelPipeline;
import us.codecraft.webmagic.model.OOSpider;
import us.codecraft.webmagic.model.annotation.ExtractBy;
import us.codecraft.webmagic.model.annotation.TargetUrl;

/**
 *  TargetUrl与HelpUrl

HelpUrl/TargetUrl是一个非常有效的爬虫开发模式，TargetUrl是我们最终要抓取的URL，最终想要的数据都来自这里；而HelpUrl则是为了发现这个最终URL，我们需要访问的页面。几乎所有垂直爬虫的需求，都可以归结为对这两类URL的处理：

对于博客页，HelpUrl是列表页，TargetUrl是文章页。
对于论坛，HelpUrl是帖子列表，TargetUrl是帖子详情。
对于电商网站，HelpUrl是分类列表，TargetUrl是商品详情。
 * @author Administrator
 *
 */
@TargetUrl("http://webmagic.io/docs/zh/posts/ch1-overview/README.html")
//@HelpUrl("http://webmagic.io/docs/zh/")
public class WebmagicModel {
	/**
	 * 抓取左侧导航菜单
	 * @ExtractBy注解主要作用于字段，它表示“使用这个抽取规则，将抽取到的结果保存到这个字段中”
	 * notnull

		@ExtractBy包含一个notNull属性，如果熟悉mysql的同学一定能明白它的意思
		：此字段不允许为空。如果为空，这条抽取到的结果会被丢弃。对于一些页面的关键性属性（例如文章的标题等），
		设置notnull为true，可以有效的过滤掉无用的页面。
	 * 默認使用xpath類型
	 * 显式指定转换类型
		一般情况下，Formatter会根据字段类型进行转换，但是特殊情况下，我们会需要手动指定类型。这主要发生在字段是List类型的时候。
	 */
	@ExtractBy(value="/html/body/div/div/ul/li//a/text()",type=ExtractBy.Type.XPath,notNull=true)
	private List<String> menu;

	public List<String> getMenu() {
		return menu;
	}

	public void setMenu(List<String> menu) {
		for (String m:menu) {
			System.out.println(m);
		}
		this.menu = menu;
	}
	
	public static void main(String[] args) {
		/**
		 * 注解模式的入口是OOSpider，它继承了Spider类，提供了特殊的创建方法，其他的方法是类似的。
		 * 创建一个注解模式的爬虫需要一个或者多个Model类，以及一个或者多个PageModelPipeline——定义处理结果的方式。
		 * public static OOSpider create(Site site, PageModelPipeline pageModelPipeline, Class... pageModels);
		 */
		OOSpider.create(Site.me().setSleepTime(1000)
                , new ConsolePageModelPipeline(), WebmagicModel.class).addUrl("http://webmagic.io/docs/zh/").thread(5).run();
	}
}
