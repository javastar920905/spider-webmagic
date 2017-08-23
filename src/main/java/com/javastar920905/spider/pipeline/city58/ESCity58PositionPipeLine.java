package com.javastar920905.spider.pipeline.city58;

import static com.javastar920905.spider.util.SpiderConstantKey.COMPANY_JSON;
import static com.javastar920905.spider.util.SpiderConstantKey.POSITION_JSON;
import static com.javastar920905.spider.util.StringUtil.RESULT;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.javastar920905.spider.util.ESOpsUtil;
import com.javastar920905.spider.util.SpringContextUtil;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

/**
 * Created by ouzhx on 2017/8/22.
 */
public class ESCity58PositionPipeLine extends ESOpsUtil implements Pipeline {
  private static final Logger LOGGER = LoggerFactory.getLogger(ESCity58PositionPipeLine.class);

  @Override
  public void process(ResultItems resultItems, Task task) {
    JSONArray jsonArray = resultItems.get(RESULT);
    BulkProcessor bulkProcessor = SpringContextUtil.getBean("bulkProcessor", BulkProcessor.class);
    if (jsonArray != null && jsonArray.size() > 0) {
      for (Object positionObj : jsonArray) {
        String positionString = JSONObject.toJSONString(positionObj);
        JSONObject positionExtraJson = JSONObject.parseObject(positionString);
        JSONObject positionJson = positionExtraJson.getJSONObject(POSITION_JSON);
        JSONObject companyJson = positionExtraJson.getJSONObject(COMPANY_JSON);

        bulkProcessor.add(new IndexRequest(INDEX_RCJ, TYPE_POSITION).source(positionJson));
        bulkProcessor.add(new IndexRequest(INDEX_RCJ, TYPE_COMPANY).source(companyJson));
      }
    }

    // 手动flash Flush any remaining requests
    bulkProcessor.flush();

  }
}
