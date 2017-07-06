package com.javastar920905.spider.pipeline;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import java.util.Map;

/**
 * Created by ouzhx on 2017/7/5.
 */
public class RedisPipeLine implements Pipeline {
  // 一个页面请求完成触发一次
  public void process(ResultItems resultItems, Task task) {

    for (Map.Entry<String, Object> entry : resultItems.getAll().entrySet()) {
      System.out.println(entry.getKey() + ":\t" + entry.getValue());
    }
  }
}
