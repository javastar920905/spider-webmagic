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
import static com.javastar920905.spider.util.SpiderConstantKey.Position.CITY;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.COMPANY_LINK;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.COMPANY_NAME;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.DEGREE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.JOB_DESC;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.JOB_NATURE;
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
      public static String firstfunc = funTypeData[0];// 第一个工作职能
      public static String timeNumber = "1";// 24小时内
      // 从北京地区+计算机软件行业 开始扒取 指定时间内新增url 根据发布时间排序
      public static String pageUrl = "http://sou.zhaopin.com/jobs/searchresult.ashx?bj=" + firstfunc
          + "&in=" + firstIndustry + "&jl=%E5%B9%BF%E4%B8%9C&isadv=0&isfilter=1&p=1&pd=1";
      public static final String fistPage = pageUrl;

      /**
       * 获取指定时间段内 的增量url (省份+行业)排列組合 *pageSize
       */
      public static List<String> getIncreUrls(Html html, String provinceNumber) throws Exception {
        int pageSize = getPageSize(html);

        List<String> urls = new LinkedList();

        // 生成请求列表[(省份i~行业n),(地区i+1,行业1)]
        int areaIndex = 0;
        boolean canExit = false;
        // 行业52*省份30*职能59=92040 *页码
        for (; areaIndex < provinceValueData.length; areaIndex++) {
          provinceNumber = URLDecoder.decode(provinceNumber, UTF8_CHARSET);
          if (provinceValueData[areaIndex].equals(provinceNumber)) {
            String encodeProvince = URLEncoder.encode(provinceValueData[areaIndex], UTF8_CHARSET);
            for (String industryNum : industryData) {
              for (String func : funTypeData) {
                int pageNum = 1;
                for (; pageNum <= pageSize; pageNum++) {
                  // 生成 指定省份+行业+今天内 的所有页码的职位列表url
                  urls.add("http://sou.zhaopin.com/jobs/searchresult.ashx?bj=" + func + "&in="
                      + industryNum + "&jl=" + encodeProvince + "&isadv=0&isfilter=1&p=" + pageNum
                      + "&pd=1");
                }
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
          String changeCityUrl = "http://sou.zhaopin.com/jobs/searchresult.ashx?bj=" + firstfunc
              + "&in=" + firstIndustry + "&jl="
              + URLEncoder.encode(provinceValueData[areaIndex], "utf-8")
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
      String parentCss = "#newlist_list_content_table";
      // 职位信息获取 (这里有多少个字段就相当于有多少个数组)
      List<String> positionLinkList =
          html.css(parentCss + " td.zwmc div a:nth-child(1)").links().all();
      List<String> companyLinkList = html.css(parentCss + " td.gsmc a:nth-child(1)").links().all();
      JSONArray positionJsonArray = new JSONArray();
      int i = 0;
      if (positionLinkList != null) {
        int listSize = positionLinkList.size();
        for (; i < listSize; i++) {
          JSONObject json = new JSONObject();
          String positinLink = positionLinkList.get(i);
          String companyLink = companyLinkList.get(i);
          json.put("positionLink", positinLink);
          json.put("companyLink", companyLink);
          if (positinLink.startsWith("http://jobs.zhaopin.com")) {
            // 特殊职位处理
            // https://xiaoyuan.zhaopin.com/job/CC000956761J90000110000?ssidkey=y&ss=201&ff=03&sg=28c70134fdee4c78af997854f955252a&so=33
            json.put(POSITION_JSON, dealPositionInfo(positinLink));
          }
          // 抓取公司详情
          if (StringUtil.isNotEmpty(companyLink)
              && companyLink.startsWith("http://company.zhaopin.com")) {
            // 特殊公司处理 http://special.zhaopin.com/pagepublish/41124893/index.html
            json.put(COMPANY_JSON, dealCompanyJson(companyLink));
          }
          json.put(SOURCE, "zhilian");// 数据来源
          // 扒取职位信息
          positionJsonArray.add(json);
        }
      }
      return positionJsonArray;
    }

    private static JSONObject dealPositionInfo(String url) {
      Html html = SpiderUtil.captureHtml(url, UTF8_CHARSET);
      if (html == null) {
        return null;
      }
      // 职位信息获取
      String positionId = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")); // id
      String positionLink = url;
      Selectable companyDom = html.xpath("/html/body/div[5]/div[1]/div[1]/h2/a");
      String companyName = companyDom.xpath("a/text()").get(); // 公司名
      String companyLink = companyDom.links().get();
      String workExp = html.xpath("/html/body/div[6]/div[1]/ul/li[5]/strong/text()").get(),
          degree = html.xpath("/html/body/div[6]/div[1]/ul/li[6]/strong/text()").get(),
          publishDate = html.xpath("//*[@id=\"span4freshdate\"]/text()").get();// 日期转换 刚刚 前天,昨天,今天
                                                                               // 2小时前 15天前 else
                                                                               // 07-21

      Selectable positionDesc = html.css(
          "div.terminalpage.clearfix > div.terminalpage-left > div.terminalpage-main.clearfix  div.tab-inner-cont");

      try {
        if (positionId != null && !StringUtils.isEmpty(positionId)) {
          JSONObject json = new JSONObject();
          json.put(ID, positionId);
          json.put(POSITION_NAME, html.xpath("/html/body/div[5]/div[1]/div[1]/h1/text()").get());// 职位名
          json.put(POSITION_TITLE,
              html.xpath("/html/body/div[6]/div[1]/ul/li[8]/strong/a/text()").get());// 工作职能
          json.put(WELFARE, html.xpath("/html/body/div[5]/div[1]/div[1]/div[1]/span/text()").all());// 福利标签
          json.put(POSITION_LINK, positionLink);
          json.put(WORK_EXPERIENCE, workExp);// 工作年限
          json.put(DEGREE, degree);// 学历
          json.put(PUBLISHED_DATE, publishDate);
          json.put(COMPANY_NAME, companyName);
          json.put(COMPANY_LINK, companyLink);
          json.put(CITY, html.xpath("/html/body/div[6]/div[1]/ul/li[2]/strong/a/text()").get());
          json.put(WORKPLACE, positionDesc.css(" h2").xpath("h2/text()").get());
          json.put(SALARY, html.xpath("/html/body/div[6]/div[1]/ul/li[1]/strong/text()").get());
          json.put(JOB_DESC, positionDesc);
          json.put(JOB_NATURE, html.xpath("/html/body/div[6]/div[1]/ul/li[4]/strong/text()"));
          return json;
        }
      } catch (Exception e) {
        LOGGER.error("获取页面失败 {}", url, e);
      }
      return null;
    }


    private static JSONObject dealCompanyJson(String url) {
      Html html = SpiderUtil.captureHtml(url, UTF8_CHARSET);
      // 公司信息获取
      String companyId = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")); // id
      if (companyId != null && !StringUtils.isEmpty(companyId)) {
        JSONObject json = new JSONObject();
        json.put(ID, companyId);
        json.put(NAME, html.xpath("/html/body/div[2]/div[1]/div[1]/h1/text()").get()); // 公司名

        json.put(COMPANY_ADDRESS, html
            .xpath("/html/body/div[2]/div[1]/div[1]/table/tbody/tr[4]/td[2]/span/text()").get());
        json.put(SpiderConstantKey.Company.COMPANY_NATURE, html
            .xpath("/html/body/div[2]/div[1]/div[1]/table/tbody/tr[1]/td[2]/span/text()").get()); // 公司类型
        json.put(SpiderConstantKey.Company.COMPANY_SCALE, html
            .xpath("/html/body/div[2]/div[1]/div[1]/table/tbody/tr[2]/td[2]/span/text()").get()); // 规模
        json.put(SpiderConstantKey.Company.INDUSTRY, html
            .xpath("/html/body/div[2]/div[1]/div[1]/table/tbody/tr[3]/td[2]/span/text()").get()); // 公司行业
        json.put(PROFILE, html.css("div.main div.company-content"));// 公司简介
        json.put(LOGO,
            html.css("div.main div.company-content img.companyLogo").$("img", "src").get());
        return json;
      }
      return null;
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
    private static final String[] funTypeData = {"4010200", "7001000", "7002000", "4000000",
        "4082000", "4084000", "7004000", "2060000", "5002000", "3010000", "201300", "2023405",
        "1050000", "160000", "160300", "160200", "160400", "200500", "200300", "5001000", "141000",
        "140000", "142000", "2071000", "2070000", "7006000", "200900", "4083000", "4010300",
        "4010400", "121100", "160100", "7003000", "7003100", "5003000", "7005000", "5004000",
        "121300", "120500", "2120000", "2100708", "2140000", "2090000", "2080000", "2120500",
        "5005000", "4040000", "201100", "2050000", "2051000", "6270000", "130000", "2023100",
        "100000", "200100", "5006000", "200700", "300100", "300200"};
    // </editor-fold>
  }

}
