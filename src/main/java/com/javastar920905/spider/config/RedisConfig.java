package com.javastar920905.spider.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import com.javastar920905.spider.util.SpringContextUtil;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by ouzhx on 2017/7/7.
 */
@Configuration
@Import(SpringContextUtil.class)
public class RedisConfig {
  public static final String Host = "192.168.1.170";
  public static final int port = 6379;


  @Bean
  public JedisConnectionFactory jedisConnectionFactory() {
    JedisConnectionFactory factory = new JedisConnectionFactory();
    factory.setUsePool(true);
    factory.setPoolConfig(new JedisPoolConfig());
    factory.setHostName(Host);
    factory.setPort(port);
    return factory;
  }

  @Bean
  public RedisTemplate redisTemplate(JedisConnectionFactory jedisConnectionFactory) {
    RedisTemplate redisTemplate = new RedisTemplate();
    redisTemplate.setConnectionFactory(jedisConnectionFactory);
    return redisTemplate;

  }
}
