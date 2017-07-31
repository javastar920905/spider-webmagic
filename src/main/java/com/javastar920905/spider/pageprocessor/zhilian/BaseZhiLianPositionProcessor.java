package com.javastar920905.spider.pageprocessor.zhilian;

import static com.javastar920905.spider.util.RedisOpsUtil.closeRedisConnection;
import static com.javastar920905.spider.util.RedisOpsUtil.getRedisConnection;
import static com.javastar920905.spider.util.SpiderConstantKey.COMPANY_JSON;
import static com.javastar920905.spider.util.SpiderConstantKey.ID;
import static com.javastar920905.spider.util.SpiderConstantKey.POSITION_JSON;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.COMPANY_ADDRESS;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.LOGO;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.NAME;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.OFFICE_ENVIRONMENT;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.PROFILE;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.SOURCE;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.WEBSITE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.COMPANY_LINK;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.COMPANY_NAME;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.DEGREE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.JOB_DESC;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.LABEL_LIST;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_LINK;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_NAME;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_TITLE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.PUBLISHED_DATE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.SALARY;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.WELFARE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.WORKPLACE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.WORK_EXPERIENCE;
import static com.javastar920905.spider.util.StringUtil.UTF8_CHARSET;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.javastar920905.spider.listener.FailedPageListener;
import com.javastar920905.spider.util.RedisOpsUtil;
import com.javastar920905.spider.util.SpiderConstantKey;
import com.javastar920905.spider.util.SpiderUtil;
import com.javastar920905.spider.util.StringUtil;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.SpiderListener;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

/**
 * Created by ouzhx on 2017/7/6.
 * 
 * 51job 职位列表url
 */
public class BaseZhiLianPositionProcessor extends SpiderUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(BaseZhiLianPositionProcessor.class);

  // 职位列表工具类
  protected static class PositionList {
    // 根据发布时间排序
    // <editor-fold desc="获取所有目标页面的url">
    public static int getPageSize(Html html) {
      // 原内容为 "共2000页，到第"
      String pageSizeStr = html.xpath("/html/body/div[3]/div[3]/div[2]/span[1]/em/text()").get();
      if (pageSizeStr != null) {
        // 截取"共"和"页"之间的数值
        int totalSize = Integer.valueOf(pageSizeStr);
        int rest = totalSize % 60;
        int pageSize = totalSize / 60;
        if (pageSize > 90) {
          return 90;// 智联分页最多显示90条记录
        }
        return rest > 0 ? (pageSize + 1) : pageSize;
      }
      return 1;
    }

    /**
     * 获取职位增量url
     */
    public static class Increment {
      public static String firstArea = provinceValueData[0];
      public static String firstIndustry = industryData[0];// 计算机软件行业
      public static String timeNumber = "1";// 24小时内
      // 从北京地区+计算机软件行业 开始扒取 指定时间内新增url 根据发布时间排序
      public static String pageUrl = "http://sou.zhaopin.com/jobs/searchresult.ashx?in="
          + firstIndustry + "&jl=%E5%B9%BF%E4%B8%9C&isadv=0&isfilter=1&p=1&pd=1";
      public static final String fistPage = pageUrl;

      /**
       * 获取指定时间段内 的增量url (省份+行业)排列組合 *pageSize
       */
      public static List<String> getIncreUrls(Html html, String provinceNumber) throws Exception {
        int pageSize = getPageSize(html);
        int pageNum = 2;
        // 第一页已经在request中请求了,<=pageSize页需要请求
        List<String> urls = new LinkedList();

        // 生成请求列表[(省份i~行业n),(地区i+1,行业1)]
        int areaIndex = 0;
        boolean canExit = false;
        for (; areaIndex < provinceValueData.length; areaIndex++) {
          provinceNumber = URLDecoder.decode(provinceNumber, UTF8_CHARSET);
          if (provinceValueData[areaIndex].equals(provinceNumber)) {
            for (String industryNum : industryData) {
              for (; pageNum <= pageSize; pageNum++) {
                // 生成 指定省份+行业+今天内 的所有页码的职位列表url
                urls.add("http://sou.zhaopin.com/jobs/searchresult.ashx?in=" + industryNum + "&jl="
                    + URLEncoder.encode(provinceValueData[areaIndex], UTF8_CHARSET)
                    + "&isadv=0&isfilter=1&p=" + pageNum + "&pd=1");
              }
            }
            canExit = true;// 可以终止最外层循环了
          } else {
            if (canExit) {
              break;
            }
          }
        }
        if (areaIndex < provinceValueData.length) {
          String changeCityUrl = "http://sou.zhaopin.com/jobs/searchresult.ashx?in=" + firstIndustry
              + "&jl=" + URLEncoder.encode(provinceValueData[areaIndex], "utf-8")
              + "&isadv=0&isfilter=1&p=1&pd=1";
          // 退出循环时areaIndex已经+1了
          urls.add(changeCityUrl);
          LOGGER.info(" 当前下标为{},对应城市为:{},列表最后一条url:为{} ", areaIndex, provinceValueData[areaIndex],
              changeCityUrl);
        }
        return urls;
      }
    }
    // </editor-fold>

    public static JSONArray dealPositionList(Html html) {
      // 职位列表css定位
      String parentCss = "#resultList .el ";
      // 职位信息获取 (这里有多少个字段就相当于有多少个数组)
      List<String> positionIdList = html.$(parentCss + ".t1 input", "value").all(); // id
      List<String> positionNameList = html.css(parentCss + ".t1 span a").xpath("a/text()").all(); // 职位名
      List<String> positionLinkList = html.css(parentCss + ".t1 span a").links().all();
      List<String> companyNameList = html.css(parentCss + ".t2 a").xpath("a/text()").all(); // 公司名
      List<String> companyLinkList = html.css(parentCss + ".t2 a").links().all();
      List<String> workPlaceList =
          html.css(parentCss + ".t3:not(.title .t3)").xpath("span/text()").all();
      List<String> salaryList =
          html.css(parentCss + ".t4:not(.title .t4)").xpath("span/text()").all();
      List<String> publishDateList =
          html.css(parentCss + " .t5:not(.title .t5)").xpath("span/text()").all();

      JSONArray positionJsonArray = new JSONArray();
      int i = 0;
      if (positionIdList != null) {
        int listSize = positionIdList.size();
        for (; i < listSize; i++) {
          JSONObject json = new JSONObject();
          // json.put("positionId", positionIdList.get(i));
          json.put("positionLink", positionLinkList.get(i));
          json.put("companyLink", companyLinkList.get(i));
          /*
           * json.put("positionName", positionNameList.get(i)); json.put("companyName",
           * companyNameList.get(i)); json.put("workPlace", workPlaceList.get(i));
           * json.put("salary", salaryList.get(i)); json.put("publishDate", publishDateList.get(i));
           */
          json.put(POSITION_JSON, dealPositionInfo(positionLinkList.get(i)));
          // 抓取公司详情
          if (StringUtil.isNotEmpty(companyLinkList.get(i))) {
            json.put(COMPANY_JSON, dealCompanyJson(companyLinkList.get(i)));
          }
          // 扒取职位信息
          positionJsonArray.add(json);
        }
      }
      return positionJsonArray;
    }

    private static JSONObject dealPositionInfo(String url) {
      Html html = SpiderUtil.captureHtml(url);
      if (html == null) {
        return null;
      }
      // 职位信息获取
      String positionId = html.xpath("//*[@id=\"hidJobID\"]").$("input", "value").get(); // id
      String positionLink = url;
      Selectable companyDom = html.xpath("/html/body/div[2]/div[2]/div[2]/div/div[1]/p[1]/a");
      String companyName = companyDom.xpath("a/text()").get(); // 公司名
      String companyLink = companyDom.links().get();
      String industry = html.xpath("/html/body/div[2]/div[2]/div[2]/div/div[1]/p[2]/text()").get();
      List<String> requirements =
          html.xpath("/html/body/div[2]/div[2]/div[3]/div[1]/div/div/span/text()").all();
      String workExp = null, degree = null, peopleCount = null, publishDate = null;
      for (String element : requirements) {
        if (element.contains("经验")) {
          workExp = element;
        } else if (element.contains("招聘")) {
          // peopleCount = element;
        } else if (element.contains("发布")) {
          publishDate = element.substring(0, element.indexOf("发布"));
        } else {
          degree = element;
        }
      }

      String positionDesc = html.xpath("/html/body/div[2]/div[2]/div[3]/div[4]/div/text()").get();

      try {
        if (positionId != null && !StringUtils.isEmpty(positionId)) {
          JSONObject json = new JSONObject();
          json.put(ID, positionId);
          json.put(POSITION_NAME,
              html.xpath("/html/body/div[2]/div[2]/div[2]/div/div[1]/h1/text()").get());// 职位名
          json.put(POSITION_TITLE,
              html.css(
                  "div.tCompanyPage > div.tCompany_center.clearfix > div.tCompany_main > div:nth-child(4) > div > div.mt10 > p:nth-child(1) > span.el")
                  .xpath("span/text()").all());// 工作职能
          json.put(WELFARE,
              html.xpath("/html/body/div[2]/div[2]/div[3]/div[1]/div/p/span/text()").all());// 福利标签
          json.put(LABEL_LIST,
              html.css(
                  "div.tCompanyPage > div.tCompany_center.clearfix > div.tCompany_main > div:nth-child(4) > div > div.mt10 > p:nth-child(2) > span.el")
                  .xpath("span/text()").all());// 关键字(有关键字就一定有工作职能)
          json.put(POSITION_LINK, positionLink);
          json.put(WORK_EXPERIENCE, workExp);// 工作年限
          json.put(DEGREE, degree);// 学历
          json.put(PUBLISHED_DATE, publishDate);
          json.put(COMPANY_NAME, companyName);
          json.put(COMPANY_LINK, companyLink);
          json.put(WORKPLACE,
              html.xpath("/html/body/div[2]/div[2]/div[3]/div[5]/div/p/text()").get());
          json.put(SALARY,
              html.xpath("/html/body/div[2]/div[2]/div[2]/div/div[1]/strong/text()").get());
          // 处理行业 公司规模 公司性质等信息
          dealIndustryInfo(industry, json);
          json.put(JOB_DESC, positionDesc);
          return json;
        }
      } catch (Exception e) {
        LOGGER.error("获取页面失败 {}", url, e);
      }
      return null;
    }


    private static JSONObject dealCompanyJson(String url) {
      Html html = SpiderUtil.captureHtml(url);
      // 公司信息获取
      String companyId = html.xpath("//*[@id=\"hidCOID\"]").$("input", "value").get(); // id
      if (companyId != null && !StringUtils.isEmpty(companyId)) {
        JSONObject json = new JSONObject();
        json.put(ID, companyId);
        json.put(NAME, html.xpath("/html/body/div[2]/div[2]/div[2]/div/h1/text()").get()); // 公司名
        json.put(LOGO, html.xpath("/html/body/div[2]/div[2]/div[2]/div/img").$("img", "src").get());
        json.put(COMPANY_ADDRESS,
            html.xpath("/html/body/div[2]/div[2]/div[3]/div[2]/div/p/text()").get());
        String companyIndustryString =
            html.xpath("/html/body/div[2]/div[2]/div[2]/div/p[1]/text()").get();
        dealIndustryInfo(companyIndustryString, json);
        json.put(PROFILE,
            html.xpath("/html/body/div[2]/div[2]/div[3]/div[1]/div/div/div[1]/p/text()").get());// 公司简介
        // 描述
        json.put(WEBSITE,
            html.xpath("/html/body/div[2]/div[2]/div[3]/div[3]/div/p/a/text()").get());// 官网
        json.put(OFFICE_ENVIRONMENT,
            html.xpath("//*[@id=\"divCoPoster\"]/ul/li/a/img").$("img", "src").all());// 办公环境
        json.put(SOURCE, "51job");// 数据来源
        return json;
      }
      return null;
    }


    // 处理行业 公司规模 和企业性质信息
    private static void dealIndustryInfo(String industryString, JSONObject json) {
      String[] industryArr = industryString.split("\\|");
      if (industryArr != null) {
        String nature = null;
        String scale = null;
        String[] industry = null;
        for (String element : industryArr) {
          if (element.contains("人")) {
            scale = element;
          } else if (element.contains("公司") || element.contains("资") || element.contains("企")
              || element.contains("机") || element.contains("单位")) {
            nature = element;
          } else {
            industry = element.split(" ");
          }
        }
        json.put(SpiderConstantKey.Company.COMPANY_NATURE, nature); // 公司类型
        json.put(SpiderConstantKey.Company.COMPANY_SCALE, scale); // 规模
        json.put(SpiderConstantKey.Company.INDUSTRY, industry); // 公司行业
      }
    }

    // <editor-fold desc="定义地区 行业 工作职能等数据字典">
    /* 智联招聘 所有行业编号 */
    private static final String[] industryData = {"210500", "160400", "160000", "160500", "160200",
        "300100", "160100", "160600", "180000", "180100", "300500", "300900", "140000", "140100",
        "140200", "200300", "200302", "201400", "201300", "300300", "120400", "120200", "170500",
        "170000", "300700", "201100", "120800", "121000", "129900", "121100", "121200", "210600",
        "120700", "121300", "121500", "300000", "150000", "301100", "121400", "200600", "200800",
        "210300", "200700", "130000", "120500", "130100", "201200", "200100", "120600", "100000",
        "100100", "990000"};

    /* 智联招聘 所有省份和直辖市 */
    private static final String[] provinceValueData = {"广东", "湖北", "陕西", "四川", "辽宁", "吉林", "江苏",
        "山东", "浙江", "广西", "安徽", "河北", "山西", "内蒙", "黑龙江", "福建", "江西", "河南", "湖南", "海南", "贵州", "云南",
        "西藏", "甘肃", "青海", "宁夏", "新疆", "香港", "澳门", "台湾省"};

    /* 智联所有地区编号 */
    private static final String[] areaValueData = {};



    /* 智联招聘 所有工作职能编号 */
    private static final String[] funTypeData = {};
    // </editor-fold>
  }

}
