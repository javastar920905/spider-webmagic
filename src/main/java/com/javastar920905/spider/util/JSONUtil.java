package com.javastar920905.spider.util;

import java.util.Set;

import com.alibaba.fastjson.serializer.SerializerFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.PropertyPreFilter;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;

import static com.javastar920905.spider.util.StringUtil.SHORT_DATE_FORMAT;

/**
 * 阿里json工具类
 */
public class JSONUtil {

  private JSONUtil() {}

  public static String toJsonStr(Object obj) {
    return JSON.toJSONStringWithDateFormat(obj, SHORT_DATE_FORMAT);
  }


  /**
   * 获取JSON排除属性Filter
   * 
   * @param properties
   * @return SimplePropertyPreFilter
   * @author chenjun
   */
  public static PropertyPreFilter getExcludeFilter(String... properties) {
    SimplePropertyPreFilter filter = new SimplePropertyPreFilter();
    Set<String> excludes = filter.getExcludes();

    Assert.notNull(properties);

    for (String property : properties) {
      excludes.add(property);
    }
    return filter;
  }

  /**
   * 获取JSON包含属性Filter
   *
   * @param properties
   * @return SimplePropertyPreFilter
   * @author chenjun
   */
  public static PropertyPreFilter getIncludeFilter(String... properties) {
    SimplePropertyPreFilter filter = new SimplePropertyPreFilter();
    Set<String> includes = filter.getIncludes();

    Assert.notNull(properties);

    for (String property : properties) {
      includes.add(property);
    }
    return filter;
  }

  /**
   * 把Java对象转换成JSON对象
   * 
   * @param object
   * @param filter
   * @return JSONObject
   * @author chenjun
   */
  public static JSONObject parseObjectToJSONObject(Object object, SerializeFilter filter) {
    String jsonStr = JSON.toJSONString(object, filter);

    return JSON.parseObject(jsonStr);
  }

  public static JSONObject parseObjectToJSONObject(Object object) {
    return JSON.parseObject(JSON.toJSONString(object, SerializerFeature.WriteMapNullValue));
  }

  /**
   * 把Java对象转换成JSON数组
   *
   * @param object
   * @param filter
   * @return JSONArray
   * @author chenjun
   */
  public static JSONArray parseObjectToJSONArray(Object object, SerializeFilter filter) {
    String jsonStr = JSON.toJSONString(object, filter);

    return JSON.parseArray(jsonStr);
  }

  /**
   * 将对象转为jsonArray 并且指定日期格式
   *
   * @param
   * @return
   * @author ouzhx on 2017/5/24
   */
  public static JSONArray toJsonArray(Object obj, String dateFormat) {
    if (dateFormat != null) {
      return JSON.parseArray(JSON.toJSONStringWithDateFormat(obj, dateFormat,
          SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullStringAsEmpty));
    } else {
      return JSON.parseArray(JSON.toJSONStringWithDateFormat(obj, SHORT_DATE_FORMAT,
          SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullStringAsEmpty));
    }
  }

}
