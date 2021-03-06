package com.javastar920905.spider.pageprocessor.rcj.job51;


import static com.javastar920905.spider.util.SpiderConstantKey.COMPANY_JSON;
import static com.javastar920905.spider.util.SpiderConstantKey.POSITION_JSON;
import static com.javastar920905.spider.util.SpiderConstantKey.SOURCE;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.COMPANY_ADDRESS;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.LOGO;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.NAME;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.OFFICE_ENVIRONMENT;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.PROFILE;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.WEBSITE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.COMPANY_LINK;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.DEGREE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.JOB_DESC;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.LABEL_LIST;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_LINK;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_MAJOR_REQUIREMENT;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_TITLE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.PUBLISHED_DATE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.SALARY;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.WELFARE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.WORK_EXPERIENCE;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.javastar920905.spider.util.RedisOpsUtil;
import com.javastar920905.spider.util.SpiderConstantKey;
import com.javastar920905.spider.util.SpiderUtil;
import com.javastar920905.spider.util.StringUtil;

import us.codecraft.webmagic.selector.Html;

/**
 * Created by ouzhx on 2017/7/6.
 * 
 * 51job 职位列表url
 */
public class BaseJob51PositionProcessor extends SpiderUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(BaseJob51PositionProcessor.class);
  public static final String GB2312_CHARSET = "gb2312";
  public static String firstArea = "030000";
  public static String firstIndustry = "01";// 计算机软件行业
  // 从北京地区+计算机软件行业 开始扒取
  public static String pageUrl = "http://search.51job.com/list/" + firstArea + ",000000,0000,"
      + firstIndustry + ",9,99,%2B,1,";
  // 根据发布时间排序
  public static String queryString =
      "?lang=c&stype=1&postchannel=0000&workyear=99&cotype=99&degreefrom=99&jobterm=99&companysize=99&lonlat=0%2C0&radius=-1&ord_field=1&confirmdate=9&fromType=1&dibiaoid=0&address=&line=&specialarea=00&from=&welfare=";
  public static String pageUrl_suffix = ".html" + queryString;
  public static final String fistPage = pageUrl + 1 + pageUrl_suffix;

  // <editor-fold desc="获取所有目标页面的url">
  /**
   * 获取所有目标页面的url (地区+行业)排列組合 *pageSize
   */
  public static List<String> getUrls(Html html, String currentCityNum) {
    // 第一页已经在request中请求了,<=pageSize页需要请求
    List<String> urls = new LinkedList();

    // 生成请求列表[(地区i~行业n),(地区i+1,行业1)]
    int areaIndex = 0;
    boolean canExit = false;
    for (; areaIndex < areaValueData.length; areaIndex++) {
      if (areaValueData[areaIndex].equals(currentCityNum)) {
        for (String industryNum : industryData) {
          String url = "http://search.51job.com/list/" + currentCityNum + ",000000,0000,"
              + industryNum + ",9,99,%2B,1,";
          int pageSize = getPageSize(url + 1 + pageUrl_suffix);
          int pageNum = 1;
          for (; pageNum <= pageSize; pageNum++) {
            urls.add(url + pageNum + pageUrl_suffix);
          }
        }
        canExit = true;// 可以终止最外层循环了
      } else {
        if (canExit) {
          break;
        }
      }
    }
    if (areaIndex < areaValueData.length) {
      String changeCityUrl = "http://search.51job.com/list/" + areaValueData[areaIndex]
          + ",000000,0000," + industryData[0] + ",9,99,%2B,1," + 1 + pageUrl_suffix;
      // 退出循环时areaIndex已经+1了
      urls.add(changeCityUrl);
      LOGGER.info(" 当前下标为{},对应城市为:{},列表最后一条url:为{} ", areaIndex, areaValueData[areaIndex],
          changeCityUrl);
    }
    return urls;
  }

  public static int getPageSize(String url) {
    Html html = SpiderUtil.captureHtml(url, GB2312_CHARSET);
    if (html == null) {
      return 0;
    }
    // 原内容为 "共2000页，到第"
    String pageSizeStr = html.xpath("/html/body/div[2]/div[5]/div/div/div/span[1]/text()").get();
    if (pageSizeStr != null && !pageSizeStr.equals("0")) {
      // 截取"共"和"页"之间的数值
      int pageSize = Integer
          .valueOf(pageSizeStr.substring(pageSizeStr.indexOf("共") + 1, pageSizeStr.indexOf("页")));
      LOGGER.debug("当前url有 {} 页数据,url为: {}", pageSize, url);
      return pageSize;
    }
    return 0;
  }

  /**
   * 获取职位增量url
   */
  public static class Increment {
    public static String firstArea = null;
    public static String firstIndustry = "01";// 计算机软件行业
    public static String timeNumber = "0";// 24小时内
    // 从北京地区+计算机软件行业 开始扒取 指定时间内新增url 根据发布时间排序
    public static String pageUrl = "http://search.51job.com/list/" + firstArea + ",000000,0000,"
        + firstIndustry + "," + timeNumber + ",99,%2B,1,";
    public static String queryString =
        "?lang=c&stype=1&postchannel=0000&workyear=99&cotype=99&degreefrom=99&jobterm=99&companysize=99&lonlat=0%2C0&radius=-1&ord_field=1&confirmdate=9&fromType=1&dibiaoid=0&address=&line=&specialarea=00&from=&welfare=";
    public static String pageUrl_suffix = ".html" + queryString;
    public static final String fistPage = pageUrl + 1 + pageUrl_suffix;

    /**
     * 获取指定时间段内 的增量url (省份+行业)排列組合 *pageSize
     */
    public static void generateIncreUrls() {
      int areaIndex = 0;
      int allprovinceSize = allProvinceValueData.size();

      try {
        RedisConnection connection = RedisOpsUtil.getRedisConnection();
        for (; areaIndex < allprovinceSize; areaIndex++) {
          String provinceNumber = allProvinceValueData.get(areaIndex);
          if (provinceValueData_hot_list.contains(provinceNumber)) {
            // 热门地区加行业组合 热门城市7*行业60=420
            for (String industryNum : industryData) {
              String url = "http://search.51job.com/list/" + provinceNumber + ",000000,0000,"
                  + industryNum + "," + timeNumber + ",99,%2B,1,";
              int pageSize = getPageSize(url + 1 + pageUrl_suffix);
              int pageNum = 1;
              for (; pageNum <= pageSize; pageNum++) {
                connection.rPush(RedisOpsUtil.KEY_51JOB_LIST_URLS,
                    (url + pageNum + pageUrl_suffix).getBytes());
              }
            }
          } else {
            // 普通城市组合 28
            String url = "http://search.51job.com/list/" + provinceNumber + ",000000,0000,00,"
                + timeNumber + ",99,%2B,1,";
            int pageSize = getPageSize(url + 1 + pageUrl_suffix);
            int pageNum = 1;
            for (; pageNum <= pageSize; pageNum++) {
              connection.rPush(RedisOpsUtil.KEY_51JOB_LIST_URLS,
                  (url + pageNum + pageUrl_suffix).getBytes());
            }
          }
        }
        connection.close();
      } catch (DataAccessException e) {
        e.printStackTrace();
      }
    }

    /**
     * 获取一定数量的需要爬取的url
     *
     * @throws Exception
     */
    public static List<String> getIncreUrls() throws Exception {
      RedisConnection connection = RedisOpsUtil.getRedisConnection();
      // 获取前50条url
      List<byte[]> positionUrlList = connection.lRange(RedisOpsUtil.KEY_51JOB_LIST_URLS, 0, 49);
      connection.lTrim(RedisOpsUtil.KEY_51JOB_LIST_URLS, 49, -1);
      connection.close();
      List<String> urls = RedisOpsUtil.getStringList(positionUrlList);
      return urls;
    }
  }
  // </editor-fold>


  protected JSONArray dealPositionList(Html html) {
    List<String> positionLinkList = html.css("#resultList .el .t1 span a").links().all();
    List<String> companyLinkList = html.css("#resultList .el .t2 a").links().all();

    JSONArray positionJsonArray = new JSONArray();
    if (CollectionUtils.isNotEmpty(positionLinkList)) {
      for (int i = 0; i < positionLinkList.size(); i++) {
        String positionLink = positionLinkList.get(i);
        // 先排除需要单独处理的子域名
        if (StringUtils.startsWithIgnoreCase(positionLink, "http://jobs.51job.com")) {
          String companyLink = companyLinkList.get(i);
          JSONObject json = new JSONObject();
          json.put("companyLink", companyLink);
          json.put("positionLink", positionLink);
          // 处理公司详情
          json.put(COMPANY_JSON, dealCompanyInfo(companyLink));
          // 处理职位信息
          json.put(POSITION_JSON, dealPositionInfo(positionLink));
          positionJsonArray.add(json);
        }
      }
    }
    return positionJsonArray;
  }

  private static JSONObject dealPositionInfo(String url) {
    Html html = SpiderUtil.captureHtml(url, GB2312_CHARSET);
    if (html == null) {
      return null;
    }
    // 职位信息获取
    List<String> requirements =
        html.xpath("/html/body/div[2]/div[2]/div[3]/div[1]/div/div/span/html()").all();
    String workExp = null; // 工作经验要求
    String degree = null; // 学历要求
    String publishDate = null; // 发布时间
    String majorRequirements = null; // 专业要求
    for (String element : requirements) {
      if (element.contains("i1")) {
        workExp = trimTag(element);
      } else if (element.contains("i2")) {
        degree = trimTag(element);
      } else if (element.contains("i4")) {
        publishDate = new DateTime().getYear() + "-" + trimTag(element).replaceAll("发布", "");
      } else if (element.contains("i6")) {
        majorRequirements = trimTag(element);
      }
    }
    // 职位描述
    String positionDesc = html.xpath("/html/body/div[2]/div[2]/div[3]/div[4]/div/html()").get();
    if (StringUtil.isNotEmpty(positionDesc)) {
      positionDesc = trimStr(positionDesc.replaceAll("<span class=\"label\"[\\D\\d]+?</span>", "")
          .replaceAll("<div class=\"mt10\"[\\D\\d]+?</div>", "")
          .replaceAll("<a[\\D\\d]+?</a>", ""));
    }
    try {
      JSONObject json = new JSONObject();
      json.put(POSITION_TITLE, html.css(
          "div.tCompanyPage > div.tCompany_center.clearfix > div.tCompany_main > div:nth-child(4) > div > div.mt10 > p:nth-child(1) > span.el")
          .xpath("span/text()").all());// 工作职能
      json.put(WELFARE,
          html.xpath("/html/body/div[2]/div[2]/div[3]/div[1]/div/p/span/text()").all());// 福利标签
      json.put(LABEL_LIST, html.css(
          "div.tCompanyPage > div.tCompany_center.clearfix > div.tCompany_main > div:nth-child(4) > div > div.mt10 > p:nth-child(2) > span.el")
          .xpath("span/text()").all());// 关键字(有关键字就一定有工作职能)
      json.put(POSITION_LINK, url);
      json.put(SpiderConstantKey.SOURCE, "51job");
      json.put(WORK_EXPERIENCE, workExp);// 工作年限
      json.put(DEGREE, degree);// 学历
      json.put(PUBLISHED_DATE, publishDate);
      json.put(POSITION_MAJOR_REQUIREMENT, majorRequirements);
      json.put("workplace_city", html.xpath("//span[@class='lname']/text()").get());
      json.put("workplace_address",
          html.xpath("/html/body/div[2]/div[2]/div[3]/div[5]/div/p/text()").get());
      json.put(SALARY,
          html.xpath("/html/body/div[2]/div[2]/div[2]/div/div[1]/strong/text()").get());
      json.put(JOB_DESC, positionDesc);

      return json;
    } catch (Exception e) {
      LOGGER.error("获取页面失败 {}", url, e);
    }
    return null;
  }

  private static JSONObject dealCompanyInfo(String url) {
    Html html = SpiderUtil.captureHtml(url, GB2312_CHARSET);
    if (html == null) {
      return null;
    }
    // 公司信息获取
    JSONObject json = new JSONObject();
    try {
      // 公司名
      json.put(NAME, trimStr(html.xpath("/html/body/div[2]/div[2]/div[2]/div/h1/text()").get()));
      // LOGO
      json.put(LOGO,
          trimStr(html.xpath("/html/body/div[2]/div[2]/div[2]/div/img").$("img", "src").get()));
      // 公司地址
      json.put(COMPANY_ADDRESS,
          trimStr(html.xpath("/html/body/div[2]/div[2]/div[3]/div[2]/div/p/text()").get()));

      String companyIndustryString =
          trimStr(html.xpath("/html/body/div[2]/div[2]/div[2]/div/p[1]/text()").get());
      String[] industryArr = companyIndustryString.split("\\|");
      // 公司类型
      json.put(SpiderConstantKey.Company.COMPANY_NATURE, trimStr(industryArr[0]));
      if (industryArr.length == 2) {
        // 公司行业
        json.put(SpiderConstantKey.Company.INDUSTRY, trimStr(industryArr[1]).split(" "));
      } else if (industryArr.length == 3) {
        // 规模
        json.put(SpiderConstantKey.Company.COMPANY_SCALE, trimStr(industryArr[1]));
        // 公司行业
        json.put(SpiderConstantKey.Company.INDUSTRY, industryArr[2].split(" "));
      }
      // 公司简介
      String profile =
          html.xpath("/html/body/div[2]/div[2]/div[3]/div[1]/div/div/div[1]/p/html()").get();
      char br = 10;
      if (StringUtil.isNotEmpty(profile)) {
        profile = trimStr(profile).replaceAll("<br>", br + "").replaceAll("&nbsp;", "");
      }
      json.put(PROFILE, profile);
      // 官网
      json.put(WEBSITE,
          trimStr(html.xpath("/html/body/div[2]/div[2]/div[3]/div[3]/div/p/a/text()").get()));
      // 办公环境
      List<String> officeEnvironmentList =
          html.xpath("//*[@id=\"divCoPoster\"]/ul/li/a/img").$("img", "src").all();
      if (CollectionUtils.isNotEmpty(officeEnvironmentList)) {
        for (int i = 0; i < officeEnvironmentList.size(); i++) {
          officeEnvironmentList.set(i, officeEnvironmentList.get(i).replace("_small", ""));
        }
      }
      json.put(OFFICE_ENVIRONMENT, officeEnvironmentList);
      json.put(COMPANY_LINK, url);
    } catch (Exception e) {
      LOGGER.error("获取页面失败 {}", url, e);
    }
    // 数据来源
    json.put(SOURCE, "51job");
    return json;
  }

  // <editor-fold desc="定义地区 行业 工作职能等数据字典">
  /* 51job 所有行业编号 */
  private static final String[] industryData =
      {"01", "37", "38", "31", "39", "32", "40", "02", "35", "41", "03", "42", "43", "62", "04",
          "22", "05", "06", "44", "60", "45", "14", "33", "08", "46", "47", "12", "48", "49", "13",
          "15", "26", "09", "50", "51", "34", "63", "07", "59", "52", "18", "23", "24", "11", "53",
          "17", "54", "27", "21", "55", "19", "16", "36", "61", "56", "28", "57", "20", "29", "58"};
  /* 51job 非热门省份和直辖市 */
  private static final String[] provinceValueData = {"330000", "340000", "350000", "01", "320000",
      "310000", "300000", "290000", "280000", "270000", "260000", "250000", "240000", "230000",
      "220000", "210000", "200000", "190000", "170000", "160000", "150000", "140000", "130000",
      "120000", "110000", "100000", "060000", "050000"};


  /* 51job 热门省份和直辖市 */
  private static final String[] provinceValueData_hot =
      {"030000", "070000", "080000", "090000", "180000", "010000", "020000"};

  /* 51job所有地区编号 */
  private static final String[] areaValueData = {"010000", "010100", "010200", "010300", "010400",
      "010500", "010600", "010700", "010800", "010900", "011000", "011100", "011200", "011300",
      "011400", "011500", "011600", "011700", "011800", "020000", "020100", "020200", "020300",
      "020400", "020500", "020600", "020700", "020800", "020900", "021000", "021100", "021200",
      "021300", "021400", "021500", "021600", "021800", "021900", "030200", "030201", "030202",
      "030203", "030204", "030205", "030206", "030207", "030208", "030209", "030210", "030211",
      "030212", "030300", "030400", "030500", "030501", "030502", "030503", "030504", "030505",
      "030506", "030507", "030508", "030600", "030601", "030602", "030603", "030604", "030605",
      "030700", "030800", "030801", "030802", "030803", "030804", "030805", "030806", "030807",
      "030808", "030809", "030810", "030811", "030812", "030813", "030814", "030815", "030816",
      "030817", "030818", "030819", "030820", "030821", "030822", "030823", "030824", "030825",
      "030826", "030827", "030828", "030829", "030830", "030831", "030832", "030833", "031400",
      "031500", "031700", "031800", "031900", "032000", "032100", "032200", "032300", "032400",
      "032600", "032700", "032800", "032900", "040000", "040100", "040200", "040300", "040400",
      "040500", "040600", "040700", "040800", "040900", "041000", "050000", "050100", "050200",
      "050300", "050400", "050500", "050600", "050700", "050800", "050900", "051000", "051100",
      "051200", "051300", "051400", "051500", "051600", "060000", "060100", "060200", "060300",
      "060400", "060600", "060700", "060800", "060900", "061000", "061100", "061200", "061300",
      "061400", "061500", "061600", "061700", "061900", "062000", "062100", "062200", "062300",
      "062400", "062500", "062600", "062700", "062800", "062900", "063000", "063100", "063200",
      "063300", "063400", "063500", "063600", "063700", "063800", "063900", "064000", "070200",
      "070201", "070202", "070203", "070204", "070205", "070206", "070207", "070208", "070209",
      "070210", "070211", "070212", "070213", "070300", "070301", "070302", "070303", "070304",
      "070305", "070306", "070307", "070400", "070401", "070402", "070403", "070404", "070405",
      "070406", "070407", "070408", "070409", "070500", "070501", "070502", "070503", "070504",
      "070505", "070506", "070507", "070600", "070700", "070800", "070900", "071000", "071100",
      "071200", "071300", "071400", "071600", "071800", "071900", "072000", "072100", "072300",
      "072500", "080200", "080201", "080202", "080203", "080204", "080205", "080206", "080207",
      "080208", "080209", "080210", "080211", "080212", "080213", "080300", "080301", "080302",
      "080303", "080304", "080305", "080306", "080307", "080308", "080309", "080310", "080311",
      "080312", "080400", "080500", "080600", "080700", "080800", "080900", "081000", "081100",
      "081200", "081400", "081600", "090200", "090201", "090202", "090203", "090204", "090205",
      "090206", "090207", "090208", "090209", "090210", "090211", "090212", "090213", "090214",
      "090215", "090216", "090217", "090218", "090219", "090220", "090300", "090400", "090500",
      "090600", "090700", "090800", "090900", "091000", "091100", "091200", "091300", "091400",
      "091500", "091600", "091700", "091800", "091900", "092000", "092100", "092200", "092300",
      "100200", "100300", "100400", "100500", "100600", "100700", "100800", "100900", "101000",
      "101100", "101200", "101300", "101400", "101500", "101600", "101700", "101800", "101900",
      "102000", "102100", "110200", "110201", "110202", "110203", "110204", "110205", "110206",
      "110207", "110208", "110209", "110210", "110211", "110212", "110213", "110300", "110400",
      "110500", "110600", "110700", "110800", "110900", "111000", "120200", "120201", "120202",
      "120203", "120204", "120205", "120206", "120207", "120208", "120209", "120210", "120211",
      "120300", "120301", "120302", "120303", "120304", "120305", "120306", "120307", "120308",
      "120309", "120310", "120400", "120500", "120600", "120700", "120800", "120900", "121000",
      "121100", "121200", "121300", "121400", "121500", "121600", "121700", "121800", "130200",
      "130201", "130202", "130203", "130204", "130205", "130206", "130207", "130208", "130209",
      "130210", "130300", "130400", "130500", "130600", "130700", "130800", "130900", "131000",
      "131100", "131200", "140000", "140200", "140300", "140400", "140500", "140600", "140700",
      "140800", "140900", "141000", "141100", "141200", "141300", "141400", "141500", "150200",
      "150201", "150202", "150203", "150204", "150205", "150206", "150207", "150208", "150209",
      "150210", "150211", "150212", "150213", "150214", "150215", "150300", "150400", "150500",
      "150600", "150700", "150800", "150900", "151000", "151100", "151200", "151400", "151500",
      "151600", "151700", "151800", "160200", "160300", "160400", "160500", "160600", "160700",
      "160800", "160900", "161000", "161100", "161200", "161300", "170200", "170201", "170202",
      "170203", "170204", "170205", "170206", "170207", "170208", "170209", "170210", "170211",
      "170212", "170213", "170214", "170215", "170300", "170400", "170500", "170600", "170700",
      "170800", "170900", "171000", "171100", "171200", "171300", "171400", "171500", "171600",
      "171700", "171800", "171900", "172000", "180200", "180201", "180202", "180203", "180204",
      "180205", "180206", "180207", "180208", "180209", "180210", "180211", "180212", "180213",
      "180300", "180400", "180500", "180600", "180700", "180800", "180900", "181000", "181100",
      "181200", "181300", "181400", "181500", "181600", "181700", "181800", "190200", "190201",
      "190202", "190203", "190204", "190205", "190206", "190207", "190208", "190209", "190300",
      "190400", "190500", "190600", "190700", "190800", "190900", "191000", "191100", "191200",
      "191300", "191400", "191500", "200200", "200201", "200202", "200203", "200204", "200205",
      "200206", "200207", "200208", "200209", "200210", "200211", "200212", "200213", "200214",
      "200215", "200216", "200217", "200218", "200219", "200300", "200400", "200500", "200600",
      "200700", "200800", "200900", "201000", "201100", "201200", "210200", "210300", "210400",
      "210500", "210600", "210700", "210800", "210900", "211000", "211100", "211200", "220200",
      "220201", "220202", "220203", "220204", "220205", "220206", "220207", "220208", "220209",
      "220210", "220211", "220212", "220213", "220214", "220215", "220216", "220217", "220218",
      "220300", "220400", "220500", "220600", "220700", "220800", "220900", "221000", "221100",
      "221200", "221300", "221400", "230200", "230201", "230202", "230203", "230204", "230205",
      "230206", "230207", "230208", "230209", "230210", "230211", "230212", "230213", "230300",
      "230301", "230302", "230303", "230304", "230305", "230306", "230307", "230308", "230309",
      "230310", "230311", "230312", "230313", "230314", "230400", "230500", "230600", "230700",
      "230800", "230900", "231000", "231100", "231200", "231300", "231400", "231500", "240200",
      "240201", "240202", "240203", "240204", "240205", "240206", "240207", "240208", "240209",
      "240210", "240211", "240212", "240213", "240214", "240300", "240400", "240500", "240600",
      "240700", "240800", "240900", "241000", "241100", "250200", "250201", "250202", "250203",
      "250204", "250205", "250206", "250207", "250208", "250209", "250210", "250211", "250212",
      "250213", "250214", "250300", "250400", "250500", "250600", "251000", "251100", "251200",
      "251300", "251400", "251500", "251600", "251700", "251800", "251900", "252000", "260200",
      "260300", "260400", "260500", "260600", "260700", "260800", "260900", "261000", "270200",
      "270300", "270400", "270500", "270600", "270700", "270800", "270900", "271000", "271100",
      "271200", "271300", "271400", "271500", "280000", "280200", "280300", "280400", "280700",
      "280800", "280900", "281000", "281100", "281200", "281300", "281400", "281500", "290000",
      "290200", "290300", "290400", "290500", "290600", "300000", "300200", "300300", "300400",
      "300500", "300600", "300700", "300800", "310000", "310200", "310300", "310400", "310500",
      "310600", "310700", "310800", "310900", "311000", "311100", "311200", "311300", "311400",
      "311500", "311600", "311700", "311800", "311900", "320200", "320300", "320400", "320500",
      "320600", "320700", "320800", "320900", "330000", "340000", "350000", "01"};



  /* 51job 所有工作职能编号 */
  private static final String[] funTypeData =
      {"2400", "2401", "2402", "2403", "0100", "0106", "0107", "0144", "0148", "0145", "0146",
          "0117", "0147", "0137", "0123", "0127", "0143", "0108", "0141", "0142", "2500", "2501",
          "2514", "2502", "2537", "2530", "2503", "2516", "2531", "2525", "2526", "2524", "2532",
          "2533", "2534", "2535", "2527", "2528", "2504", "2515", "2536", "2539", "2512", "2513",
          "2505", "2506", "2507", "2508", "2517", "2518", "2519", "2520", "2521", "2522", "2523",
          "2509", "2511", "2600", "2601", "2602", "2603", "2604", "2605", "2606", "2607", "2608",
          "2609", "2700", "2701", "2702", "2712", "2715", "2703", "2704", "2705", "2706", "2707",
          "2708", "2709", "2713", "2714", "2710", "2716", "2717", "2711", "2800", "2801", "2802",
          "2803", "2804", "2805", "2806", "2807", "2808", "2810", "2811", "2809", "2900", "2901",
          "2902", "2903", "2917", "2924", "2920", "2921", "2922", "2904", "2905", "2906", "2918",
          "2907", "2908", "2909", "2910", "2919", "2911", "2912", "2923", "2913", "2914", "2915",
          "2925", "2916", "0200", "0201", "0202", "0203", "0232", "0233", "0207", "0220", "0235",
          "0208", "0230", "0226", "0234", "0231", "3000", "3009", "3001", "3002", "3003", "3004",
          "3005", "3010", "3008", "3006", "3011", "3012", "3007", "3100", "3101", "3102", "3108",
          "3109", "3103", "3104", "3105", "3106", "3107", "3200", "3201", "3202", "3203", "3204",
          "3210", "3205", "3206", "3207", "3208", "3213", "3211", "3212", "3209", "0400", "0444",
          "0401", "0402", "0445", "0403", "0404", "0405", "0414", "0422", "0448", "0406", "0407",
          "0408", "0409", "0449", "0450", "0410", "0419", "0411", "0412", "0446", "0443", "3300",
          "3301", "3302", "3303", "3304", "3312", "3315", "3305", "3306", "3307", "3313", "3308",
          "3309", "3314", "3310", "3311", "2200", "2207", "2231", "2223", "2224", "2225", "2226",
          "2227", "2228", "2208", "2209", "2215", "2229", "2210", "2212", "2211", "2213", "2214",
          "2230", "2222", "2232", "2216", "2221", "3400", "3401", "3402", "3403", "3404", "3405",
          "3406", "3414", "3407", "3408", "3409", "3410", "3411", "3413", "3412", "3500", "3501",
          "3502", "3513", "3503", "3504", "3505", "3506", "3514", "3507", "3509", "3515", "3508",
          "3512", "3516", "3510", "3511", "3600", "3601", "3602", "3603", "3604", "3605", "3606",
          "3607", "3608", "3615", "3609", "3610", "3614", "3611", "3612", "3613", "0500", "0510",
          "0511", "0547", "0559", "0584", "0512", "0513", "0514", "0515", "0523", "0560", "0582",
          "0539", "0561", "0548", "0544", "0580", "0537", "0581", "0562", "0563", "0564", "0565",
          "0566", "0567", "0568", "0569", "0570", "0583", "0571", "0572", "0575", "0576", "0573",
          "0577", "0585", "0578", "0579", "0574", "5400", "5401", "5402", "5403", "5414", "5412",
          "5404", "5405", "5406", "5411", "5900", "5901", "5903", "5907", "5912", "5913", "5914",
          "5905", "5906", "5908", "5902", "5904", "5910", "5911", "3700", "3710", "3701", "3707",
          "3715", "3716", "3703", "3717", "3706", "3718", "3719", "3720", "3721", "3722", "3709",
          "3723", "3724", "3708", "3725", "3726", "3727", "3713", "3800", "3812", "3801", "3813",
          "3802", "3803", "3804", "3814", "3805", "3806", "3811", "3808", "3809", "3807", "3815",
          "3816", "3817", "3818", "3819", "3820", "3821", "3822", "3823", "3824", "3825", "3826",
          "3827", "3828", "3810", "3900", "3901", "3902", "3903", "3904", "3905", "3908", "3909",
          "3907", "4000", "4001", "4002", "4003", "4004", "4005", "4006", "4007", "4008", "0800",
          "0827", "0801", "0802", "0814", "0828", "0825", "0826", "0803", "0804", "0808", "0809",
          "0834", "0810", "0833", "0829", "0830", "0832", "0811", "0812", "0815", "0813", "0831",
          "0835", "0823", "0836", "0824", "4100", "4101", "4116", "4103", "4104", "4126", "4105",
          "4106", "4123", "4107", "4108", "4109", "4110", "4120", "4121", "4122", "4111", "4112",
          "4117", "4124", "4118", "4113", "4125", "4114", "4119", "4115", "5500", "5501", "5502",
          "5503", "5504", "5505", "5506", "5507", "5509", "5508", "1300", "1302", "1328", "1301",
          "1317", "1318", "1319", "1320", "1308", "1327", "1321", "1322", "1313", "1325", "1309",
          "1314", "1304", "1310", "1323", "1324", "1305", "1315", "1326", "1311", "4200", "4201",
          "4202", "4203", "4204", "4205", "4206", "4212", "4211", "4207", "4208", "4209", "4210",
          "4300", "4315", "4301", "4302", "4303", "4304", "4305", "4306", "4307", "4308", "4309",
          "4310", "4312", "4313", "4314", "4311", "0300", "0301", "0302", "0303", "0304", "0305",
          "0324", "0306", "0307", "0330", "0308", "0335", "0336", "0337", "0310", "0311", "0312",
          "0338", "0329", "4400", "4401", "4402", "4403", "4414", "4404", "4406", "4411", "4407",
          "4408", "4415", "4412", "4413", "4410", "4500", "4501", "4502", "4517", "4503", "4516",
          "4504", "4505", "4507", "4508", "0900", "0930", "0931", "0904", "0932", "0924", "0933",
          "0925", "0926", "0927", "0919", "0920", "0928", "0929", "0921", "2100", "2123", "2101",
          "2131", "2132", "2102", "2118", "2119", "2125", "2103", "2126", "2104", "2122", "2109",
          "2108", "2117", "2120", "2110", "2127", "2105", "2124", "2106", "2133", "2121", "2107",
          "2128", "2129", "2130", "2111", "2134", "2135", "2136", "2137", "2138", "2139", "2140",
          "2141", "2142", "2143", "2144", "2145", "2116", "4600", "4601", "4602", "4603", "4608",
          "4610", "4611", "4612", "4607", "6000", "6009", "6010", "6001", "6002", "6004", "6007",
          "6006", "6008", "4700", "4702", "4714", "4703", "4705", "4715", "4706", "4701", "4704",
          "4709", "4710", "4711", "4708", "4712", "4713", "4707", "0600", "0601", "0602", "0603",
          "0604", "0605", "0606", "0626", "0607", "0608", "0627", "0628", "0609", "0610", "0629",
          "0630", "0625", "0700", "0701", "0707", "0702", "0704", "0705", "0710", "0711", "0712",
          "0708", "0709", "0703", "0706", "2300", "2301", "2302", "2303", "2304", "2305", "2307",
          "2308", "2306", "2309", "1400", "1401", "1402", "1403", "1406", "1404", "1409", "1408",
          "1407", "1405", "1100", "1101", "1103", "1106", "1102", "1107", "1109", "1110", "1108",
          "1105", "1200", "1213", "1208", "1204", "1201", "1209", "1207", "1215", "1202", "1210",
          "1205", "1214", "1216", "1211", "1206", "5700", "5701", "5702", "5703", "5707", "5706",
          "5704", "5705", "1000", "1002", "1001", "4800", "4801", "4819", "4802", "4803", "4806",
          "4807", "4820", "4821", "4822", "4823", "4812", "4809", "4816", "4808", "4804", "4824",
          "4818", "4813", "4811", "4825", "4814", "4815", "4817", "4810", "4900", "4901", "4902",
          "4916", "4917", "4903", "4905", "4912", "4915", "4904", "4906", "4918", "4907", "4914",
          "4908", "4919", "4920", "4921", "4909", "4910", "4913", "4911", "5000", "5018", "5016",
          "5001", "5019", "5002", "5013", "5014", "5020", "5021", "5022", "5004", "5005", "5006",
          "5017", "5003", "5023", "5007", "5024", "5010", "5011", "5100", "5101", "5112", "5114",
          "5115", "5102", "5116", "5117", "5105", "5118", "5103", "5119", "5104", "5120", "5121",
          "5113", "5108", "5109", "5110", "5111", "5106", "5107", "1800", "1822", "1823", "1825",
          "1827", "1810", "1830", "1831", "1832", "1833", "1826", "1835", "1836", "1837", "1838",
          "1839", "1840", "1824", "1801", "1828", "1829", "5200", "5206", "5209", "5210", "5211",
          "5205", "5212", "5213", "5214", "5202", "5215", "5216", "5203", "5207", "1500", "1501",
          "2000", "2001", "2002", "2003", "2004", "2005", "2010", "2006", "2011", "2009", "2007",
          "2012", "2013", "2008", "1600", "1605", "1602", "1601", "1604", "1700", "1702", "1701",
          "1703", "5300", "5301", "5600", "5601", "5604", "5609", "5605", "5606", "5602", "5607",
          "5608", "5603", "5800", "5801", "5802", "5803", "5804", "5805", "5806", "5807", "6100",
          "6101", "6102", "6103", "6104", "6105", "6106", "6107", "6108", "6200", "6201", "6202",
          "6203", "6204", "6205", "6206", "6207", "6208", "6209", "6210", "6211", "6212", "6213",
          "6214", "6215", "6216", "6217", "6218", "6219", "6220", "6221", "6222", "6300", "6301",
          "6302", "6304", "6305", "6306", "6307", "6308", "6309", "6310", "6311", "6312", "6313",
          "6314", "6315", "6316", "6317", "6318", "6400", "6401", "6402", "6403", "6404", "6405",
          "6406", "6407", "6408", "6500", "6501", "6502", "6503", "6504", "6505", "6506", "6507",
          "6509", "6510", "6511", "6508", "1900", "1902", "1903", "1901"};
  // </editor-fold>

  private static String trimStr(String str) {
    char white = 160;
    return StringUtils.trimWhitespace(
        StringUtils.trimLeadingCharacter(StringUtils.trimTrailingCharacter(str, white), white));
  }

  private static String trimTag(String str) {
    Pattern tagPattern = Pattern.compile("<.+?>");
    return trimStr(tagPattern.matcher(str).replaceAll(""));
  }

  private static List<String> provinceValueData_hot_list = new ArrayList<>();
  // 智联所有城市数据字典
  private static List<String> allProvinceValueData = new ArrayList<>();
  static {
    org.springframework.util.CollectionUtils.mergeArrayIntoCollection(provinceValueData_hot,
        provinceValueData_hot_list);
    /*
     * org.springframework.util.CollectionUtils.mergeArrayIntoCollection(provinceValueData_hot,
     * allProvinceValueData);
     * org.springframework.util.CollectionUtils.mergeArrayIntoCollection(provinceValueData,
     * allProvinceValueData);
     */
    // TODO 【k/1d】职位爬取范围 地区：深圳
    allProvinceValueData.add("040000");
    provinceValueData_hot_list.add("040000");
    Increment.firstArea ="040000"; //allProvinceValueData.get(0);

    LOGGER.info("*************************************************************");
    LOGGER.info("51job 数据字典统计  热门省份城市数量: {} 普通城市数量:{} 所有城市数量:{},行业数量:{},职能数量:{}",
        provinceValueData_hot.length, provinceValueData.length, allProvinceValueData.size(),
        industryData.length, funTypeData.length);
    LOGGER.info("*************************************************************");
  }
}
