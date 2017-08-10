package com.javastar920905.spider.config;

import com.javastar920905.spider.pageprocessor.rcj.job51.Job51PositionListPageIncrementProcessor;
import com.javastar920905.spider.pageprocessor.rcj.zhilian.ZhiLianPositionListPageIncrementProcessor;
import com.javastar920905.spider.util.SpiderUtil;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Created by ouzhx on 2017/8/10.
 */
public class Main {
    public static void main(String[] args) {
        ConfigurableApplicationContext context =
            new AnnotationConfigApplicationContext(SpiderUtil.class, RedisConfig.class);

        Job51PositionListPageIncrementProcessor.runIncrementSpider();
        ZhiLianPositionListPageIncrementProcessor.runIncrementSpider();
    }
}
