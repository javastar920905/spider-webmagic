package com.javastar920905.spider.config;

import java.net.InetAddress;

import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by ouzhx on 2017/8/22.
 */
@Configuration
public class ESConfig {
  @Bean
  public TransportClient client(ConfigurableApplicationContext context) throws Exception {
    TransportClient client = new PreBuiltTransportClient(Settings.EMPTY).addTransportAddress(
        new InetSocketTransportAddress(InetAddress.getByName("192.168.1.116"), 9300));
    buildBulkProcessor(context, client);
    return client;
  }

  /**
   * es 缓存批处理对象
   * https://www.elastic.co/guide/en/elasticsearch/client/java-api/5.5/java-docs-bulk-processor.html
   */
  public void buildBulkProcessor(ConfigurableApplicationContext context, TransportClient client) {
    BulkProcessor bulkProcessor = BulkProcessor.builder(client, new BulkProcessor.Listener() {
      @Override
      public void beforeBulk(long executionId, BulkRequest request) {
        //System.out.println("开始执行es批处理操作");
      }

      @Override
      public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
        System.out.println("执行es批处理操作成功!");
      }

      @Override
      public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
        System.err.println("执行es批处理操作失败");
      }
    }).setBulkActions(1000).setBulkSize(new ByteSizeValue(5, ByteSizeUnit.MB))// 每1k(默认1k)请求执行一次bulk,每5mb刷新一次缓存
        .setFlushInterval(TimeValue.timeValueSeconds(5)).setConcurrentRequests(1)// 每5s刷新一次
        .setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueMillis(100), 3))
        .build();
    context.getBeanFactory().registerSingleton("bulkProcessor", bulkProcessor);
  }

}
