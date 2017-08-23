package com.javastar920905.spider.pageprocessor.rcj.city58;



import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.javastar920905.spider.pageprocessor.rcj.zhilian.BaseZhiLianPositionProcessor;
import com.javastar920905.spider.util.DateUtil;
import com.javastar920905.spider.util.RedisOpsUtil;
import com.javastar920905.spider.util.SpiderConstantKey;
import com.javastar920905.spider.util.SpiderUtil;
import com.javastar920905.spider.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import static com.javastar920905.spider.util.SpiderConstantKey.COMPANY_JSON;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.COMPANY_ADDRESS;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.LOGO;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.NAME;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.PROFILE;
import static com.javastar920905.spider.util.SpiderConstantKey.ID;
import static com.javastar920905.spider.util.SpiderConstantKey.POSITION_JSON;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.CITY;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.COMPANY_LINK;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.COMPANY_NAME;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.DEGREE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.JOB_DESC;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_LINK;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_NAME;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_TITLE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.PUBLISHED_DATE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.SALARY;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.WELFARE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.WORKPLACE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.WORK_EXPERIENCE;
import static com.javastar920905.spider.util.SpiderConstantKey.SOURCE;

/**
 * Created by ouzhx on 2017/7/6.
 * 
 * 职位列表url
 */
public class BaseCity58PositionProcessor extends SpiderUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(BaseCity58PositionProcessor.class);
  public static final String UTF8_CHARSET = "UTF-8";
  private static final String date_format = "yyyy-MM-dd";


  // 职位列表工具类
  public static class PositionList {
    // 根据发布时间排序
    // <editor-fold desc="获取所有目标页面的url">
    public static int getPageSize(String url) {
      // 原内容为 "共2000页，到第"
      Html html = SpiderUtil.captureHtml(url, UTF8_CHARSET);
      if (html == null) {
        return 0;
      }
      String pageSizeStr = html.xpath("/html/body/div[3]/div[3]/div/div/div/span[2]/text()").get();
      if (pageSizeStr != null && !pageSizeStr.equals("0")) {
        int pageSize = Integer.valueOf(pageSizeStr);
        // LOGGER.info("当前url有 {} 页数据,url为: {}", pageSize, url);
        if (pageSize >= 69) {
          return 70;// 58分页最多显示70页记录
        }
        return pageSize;
      }
      return 0;
    }



    /**
     * 获取职位增量url
     */
    public static class Increment {
      public static String firstArea = "bj";
      public static String firstIndustry = "244";// 计算机软件行业
      public static String firstfunc = "4010200";// 第一个工作职能
      public static String timeNumber = "1";// 24小时内
      // 从xx地区+计算机软件行业 开始扒取 指定时间内新增url 根据发布时间排序
      public static String pageUrl = "http://sou.zhaopin.com/jobs/searchresult.ashx?bj=" + firstfunc
          + "&in=" + firstIndustry + "&jl=" + firstArea + "&isadv=0&isfilter=1&p=1&pd=1";
      public static final String fistPage = pageUrl;

      // 冷门城市搜索条件
      public static String lonelyCityUrl = "http://{0}.58.com/job/?postdate={1}";
      // 普通城市搜索条件
      public static String nomalCityUrl = "http://{0}.58.com/job/pve_5363_{1}/?postdate={2}";
      // 热门城市搜索条件 地区+职能+行业+日期(注意页码为1时要省略pn1 否则报错)
      public static String hotCityUrl = "http://{0}.58.com/{1}/pve_5363_{2}/?postdate={3}";

      /**
       * 生成指定时间段内的增量url (省份+行业)排列組合 *pageSize 并保存到(消息队列或者redis)
       */
      public static void generateIncreUrls() {
        // 生成日期
        Date today = new Date();
        String start = DateUtil.dateFormat(new Date(), "yyyyMMdd");
        String end = DateUtil.dateFormat(DateUtil.getLastFewDays(today, -1), "yyyyMMdd");
        String postdate = start + "_" + end;

        int areaIndex = 0;
        int cityDatalen = allAreaValueData.size();
        try {
          RedisConnection connection = RedisOpsUtil.getRedisConnection();
          for (; areaIndex < cityDatalen; areaIndex++) {
            String provinceNumber = allAreaValueData.get(areaIndex);
            if (areaValueData_loney_list.contains(provinceNumber)) {
              // 组合方式:地区+今天 ;获取分页次数 非热门地区数目384
              String url = MessageFormat.format(lonelyCityUrl, provinceNumber, postdate);
              int pageSize = getPageSize(url);
              int pageNum = 1;
              for (; pageNum <= pageSize; pageNum++) {
                if (pageNum == 1) {
                  connection.rPush(RedisOpsUtil.KEY_58CITY_LIST_URLS, url.getBytes());
                } else {
                  connection.rPush(RedisOpsUtil.KEY_58CITY_LIST_URLS, ("http://" + provinceNumber
                      + ".58.com/job/pn" + pageNum + "/?postdate=" + postdate).getBytes());
                }

              }
            } else {
              for (String industryNum : allIndustryValueData) {
                // 北京 上海 等热门城市的热门行业 需要+职能单独处理 热门行业14*热门城市12*职能59 =9912 *页码
                if (areaValueData_hot_list.contains(provinceNumber)
                    && industryData_hot_list.contains(industryNum)) {
                  for (String func : funTypeData) {
                    String url = MessageFormat.format(hotCityUrl, provinceNumber, func, industryNum,
                        postdate);
                    int pageSize = getPageSize(url);
                    int pageNum = 1;
                    for (; pageNum <= pageSize; pageNum++) {
                      if (pageNum == 1) {
                        connection.rPush(RedisOpsUtil.KEY_58CITY_LIST_URLS, url.getBytes());
                      } else {
                        connection.rPush(RedisOpsUtil.KEY_58CITY_LIST_URLS,
                            ("http://" + provinceNumber + ".58.com/" + func + "/pn" + pageNum
                                + "/pve_5363_" + industryNum + "/?postdate=" + postdate)
                                    .getBytes());
                      }

                    }
                  }
                } else {
                  // 组合方式:地区+今天 ;获取分页次数 非热门地区数目384
                  String url =
                      MessageFormat.format(nomalCityUrl, provinceNumber, industryNum, postdate);
                  int pageSize = getPageSize(url);
                  int pageNum = 1;
                  for (; pageNum <= pageSize; pageNum++) {
                    if (pageNum == 1) {
                      connection.rPush(RedisOpsUtil.KEY_58CITY_LIST_URLS, url.getBytes());
                    } else {
                      connection
                          .rPush(RedisOpsUtil.KEY_58CITY_LIST_URLS,
                              ("http://" + provinceNumber + ".58.com/job/pn" + pageNum
                                  + "/pve_5363_" + industryNum + "/?postdate=" + postdate)
                                      .getBytes());
                    }

                  }
                }
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
        List<byte[]> positionUrlList = connection.lRange(RedisOpsUtil.KEY_58CITY_LIST_URLS, 0, 49);
        // 删除前50条url
        connection.lTrim(RedisOpsUtil.KEY_58CITY_LIST_URLS, 49, -1);
        connection.close();
        List<String> urls = RedisOpsUtil.getStringList(positionUrlList);
        return urls;
      }
    }
    // </editor-fold>

    public static JSONArray dealPositionList(Html html) {
      List<String> positionLinkList =
          html.css("#list_con li div.job_title div.job_name a").links().all();
      List<String> companyLinkList =
          html.css("#list_con li div.job_comp div.comp_name a").links().all();
      JSONArray positionJsonArray = new JSONArray();
      if (positionLinkList != null) {
        for (int i = 0; i < positionLinkList.size(); i++) {
          JSONObject json = new JSONObject();
          String positinLink = positionLinkList.get(i);
          String companyLink = companyLinkList.get(i);
          json.put("positionLink", positinLink);
          json.put("companyLink", companyLink);
          json.put(POSITION_JSON, dealPositionInfo(positinLink));
          // 抓取公司详情
          if (StringUtil.isNotEmpty(companyLink)) {
            json.put(COMPANY_JSON, dealCompanyJson(companyLink));
          }
          json.put(SOURCE, "city58");// 数据来源
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
      String positionId =
          url.substring(url.lastIndexOf("&psid=") + 6, url.lastIndexOf("&entinfo=")); // id
      String positionLink = url;
      Selectable companyDom = html.css(".comp_baseInfo_title .baseInfo_link a");
      String companyName = companyDom.xpath("a/text()").get(); // 公司名
      String companyLink = companyDom.links().get();
      String workExp = html.xpath("/html/body/div[3]/div[3]/div[1]/div[4]/span[3]/text()").get(),
          degree = html.xpath("/html/body/div[3]/div[3]/div[1]/div[4]/span[2]/text()").get(),
          publishDate =
              html.xpath("/html/body/div[3]/div[3]/div[1]/div[1]/span[1]/span/text()").get();

      publishDate = BaseZhiLianPositionProcessor.PositionList.dealPublishDate(publishDate);
      Selectable positionDesc = html.xpath("/html/body/div[3]/div[3]/div[2]/div[1]/div[1]");

      try {
        if (positionId != null && !StringUtils.isEmpty(positionId)) {
          JSONObject json = new JSONObject();
          json.put(ID, positionId);
          json.put(POSITION_NAME, html.xpath("/html/body/div[3]/div[3]/div[1]/span/text()").get());// 职位名
          json.put(POSITION_TITLE,
              html.xpath("/html/body/div[3]/div[3]/div[1]/div[2]/span[1]/text()").get());// 工作职能
          json.put(WELFARE, html.xpath("/html/body/div[3]/div[3]/div[1]/div[3]/span/text()").all());// 福利标签
          json.put(POSITION_LINK, positionLink);
          json.put(WORK_EXPERIENCE, workExp);// 工作年限
          json.put(DEGREE, degree);// 学历
          json.put(PUBLISHED_DATE, publishDate);
          json.put(COMPANY_NAME, companyName);
          json.put(COMPANY_LINK, companyLink);
          String city = html.css(".bread .zp_crumb a").xpath("a/text()").get();
          json.put(CITY, city != null ? city.substring(0, city.indexOf("58同城")) : StringUtil.EMPTY);
          json.put(WORKPLACE, html.xpath("/html/body/div[3]/div[3]/div[1]/div[5]/span[2]/text()"));
          json.put(SALARY,
              html.xpath("/html/body/div[3]/div[3]/div[1]/div[2]/span[2]/text()").get());
          json.put(JOB_DESC, positionDesc);
          json.put(SOURCE, "city58");
          return json;
        }
      } catch (Exception e) {
        LOGGER.error("获取页面失败 {}", url, e);
      }
      return null;
    }



    private static JSONObject dealCompanyJson(String url) {
      Html html = SpiderUtil.captureHtml(url, UTF8_CHARSET);
      if (html == null) {
        return null;
      }
      // 公司信息获取
      String companyId =
          html.xpath("/html/body/div[3]/div[1]/div[1]/div[1]/h1/a").$("a", "href").get(); // id
      if (companyId != null && !StringUtils.isEmpty(companyId)) {
        JSONObject json = new JSONObject();
        companyId = companyId.substring(companyId.indexOf("58.com/") + 7);
        companyId = companyId.replaceAll("/", "");
        json.put(ID, companyId);
        json.put(NAME, html.xpath("/html/body/div[3]/div[1]/div[1]/div[1]/h1/a/text()").get()); // 公司名

        json.put(COMPANY_ADDRESS,
            html.xpath("/html/body/div[3]/div[1]/div[2]/div[2]/ul/li[8]/div/var/text()").get());
        json.put(SpiderConstantKey.Company.COMPANY_NATURE,
            html.xpath("/html/body/div[3]/div[1]/div[2]/div[2]/ul/li[5]/text()").get()); // 公司类型
        json.put(SpiderConstantKey.Company.COMPANY_SCALE,
            html.xpath("/html/body/div[3]/div[1]/div[2]/div[2]/ul/li[7]/text()").get()); // 规模
        json.put(SpiderConstantKey.Company.INDUSTRY,
            html.xpath("/html/body/div[3]/div[1]/div[2]/div[2]/ul/li[9]/div/a/text()").get()); // 公司行业
        json.put(PROFILE, html.xpath("/html/body/div[3]/div[1]/div[2]/div[2]/div[3]"));// 公司简介
        json.put(LOGO, html.css(".head_info .head_info_img img").$("img", "src").get());
        json.put(SOURCE, "city58");
        return json;
      }
      return null;
    }



    // <editor-fold desc="定义地区 行业 工作职能等数据字典">
    /* 热门行业编号 */
    private static final String[] industryData_hot = {"244", "245", "252", "262", "275", "277",
        "278", "3527", "287", "249", "254", "255", "269", "272", "261"};

    private static final String[] industryData =
        {"246", "247", "248", "291", "250", "251", "253", "295", "256", "257", "258", "296", "259",
            "260", "263", "264", "265", "266", "267", "268", "270", "271", "273", "274", "294",
            "276", "279", "280", "281", "282", "283", "284", "285", "286", "288", "289", "290"};


    /* 所有热门地区编号 组合方式需要 地区+行业+职能才少于5400页 */
    private static final String[] areaValueData_hot =
        {"jn", "zz", "wh", "cs", "sz", "bj", "sh", "gz", "cd", "hz", "nj", "cq", "hf"};
    /* 组合方式需要 地区+行业才少于5400页 */
    private static final String[] areaValueData = {"linyi", "wf", "qd", "zb", "weihai", "yt",
        "jining", "fz", "xm", "qz", "luoyang", "ny", "xx", "ts", "lf", "sjz", "bd", "hd", "xf",
        "haikou", "nc", "hrb", "gy", "xa", "xj", "xz", "wx", "szkunshan", "nt", "cz", "yz", "su",
        "wuhu", "cc", "lz", "yinchuan", "ty", "mianyang", "nn", "liuzhou", "jx", "nb", "wz", "yiwu",
        "km", "hu", "sy", "dl", "dg", "zh", "zs", "huizhou", "fs", "tj"};
    /* 所有冷门地区编号 地区+时间<5400页 */
    private static final String[] areaValueData_loney = {"dy", "shanxian", "feicheng", "zc", "kl",
        "lc", "juping", "dz", "xintai", "zaozhuang", "ta", "penglai", "rushan", "lw", "guangrao",
        "laizhou", "rongcheng", "heze", "qingzhou", "shouguang", "tengzhou", "zoucheng", "caoxian",
        "zhaoyuan", "rizhao", "huantaixian", "zhangqiu", "longkou", "bz", "gaomi", "juxian", "ly",
        "zhangzhou", "sm", "longhai", "shishi", "np", "jinjiangshi", "nananshi", "pt", "nd",
        "wuyishan", "ay", "yanling", "xc", "lingbaoshi", "kaifeng", "yuzhou", "puyang", "hb",
        "jiyuan", "changyuan", "sq", "pds", "xiangchengshi", "luohe", "ruzhou", "mg", "qixianqu",
        "changge", "yanshiqu", "zmd", "zk", "smx", "jiaozuo", "xy", "qhd", "xionganxinqu", "hs",
        "gt", "sanhe", "wuan", "dingzhou", "zhuozhou", "chengde", "renqiu", "cangzhou", "zd", "zx",
        "lfyanjiao", "zhangbei", "zjk", "qianan", "xt", "xianning", "xiantao", "shiyan", "jingzhou",
        "xiaogan", "snj", "yidou", "ez", "qianjiang", "jingmen", "tm", "hg", "es", "hanchuan", "yc",
        "hshi", "suizhou", "zaoyang", "hy", "zhuzhou", "zjj", "yiyang", "xiangxi", "yy", "yongzhou",
        "shaoyang", "liling", "changde", "xiangtan", "ld", "chenzhou", "hh", "da", "wenchang", "cm",
        "wzs", "lingshui", "baish", "wanning", "df", "qionghai", "tunchang", "sanya", "sansha",
        "baoting", "danzhou", "qiongzhong", "yichun", "px", "xinyu", "sr", "fuzhou", "ganzhou",
        "ja", "lepingshi", "jj", "yingtan", "jdz", "yxx", "qqhr", "hegang", "qth", "jms", "jixi",
        "mdj", "heihe", "yich", "dxal", "dq", "suihua", "sys", "qn", "bijie", "qdn", "zunyi",
        "renhuaishi", "tr", "lps", "qxn", "anshun", "xianyang", "tc", "ankang", "shenmu", "wn",
        "yanan", "hanzhong", "yl", "baoji", "sl", "alt", "betl", "ks", "ale", "wjq", "ht",
        "changji", "bygl", "tac", "hami", "klmy", "tlf", "kzls", "yili", "aks", "tmsk", "shz",
        "kel", "rugao", "guannan", "qidong", "guanyun", "taixing", "xzpeixian", "yangzhong",
        "danyang", "rudong", "donghai", "ha", "jianhu", "pizhou", "jingjiang", "haimen", "dongtai",
        "xinyishi", "jiangyan", "liyang", "dafeng", "taizhou", "xinghuashi", "suqian", "zj",
        "shuyang", "baoyingx", "yancheng", "siyang", "lyg", "jintan", "sihong", "haian",
        "xuancheng", "bozhou", "bengbu", "hn", "huaibei", "fy", "mas", "tongling", "chuzhou",
        "tongcheng", "chizhou", "suzhou", "anqing", "la", "hq", "ningguo", "hexian", "tianchang",
        "huangshan", "ch", "lasa", "rituxian", "rkz", "linzhi", "gaizexian", "sn", "changdu", "nq",
        "al", "sp", "liaoyuan", "jl", "gongzhuling", "baishan", "songyuan", "th", "yanbian", "bc",
        "tianshui", "qingyang", "ln", "jq", "jinchang", "dx", "gn", "linxia", "wuwei", "by", "jyg",
        "pl", "zhangye", "wuzhong", "zw", "szs", "guyuan", "linfen", "yq", "lvliang", "shuozhou",
        "qingxu", "xinzhou", "linyixian", "dt", "jz", "changzhi", "yuncheng", "jincheng",
        "anyuexian", "liangshan", "nanchong", "ms", "ganzi", "deyang", "renshouxian", "ga",
        "guanghanshi", "zg", "ls", "yb", "luzhou", "ab", "suining", "guangyuan", "scnj", "dazhou",
        "bazhong", "ya", "panzhihua", "jianyangshi", "zy", "tw", "hk", "cn", "am", "quanguo",
        "yulin", "hc", "bh", "qinzhou", "gg", "fcg", "chongzuo", "lb", "hezhou", "gl", "wuzhou",
        "baise", "huzhou", "changxing", "sx", "yuhuan", "linhai", "jh", "jiashanx", "tongxiang",
        "dongyang", "lishui", "zhuji", "xiangshanxian", "zhoushan", "deqing", "tz", "ruiancity",
        "haining", "yuyao", "wenling", "anji", "quzhou", "cixi", "yongkang", "cangnanxian",
        "yueqingcity", "zt", "nujiang", "ws", "cx", "qj", "lj", "diqing", "yx", "pe", "bn", "dh",
        "dali", "lincang", "honghe", "bs", "wuhai", "hlbe", "bt", "hlr", "erds", "alsm", "bycem",
        "tongliao", "xam", "xl", "wlcb", "chifeng", "pj", "hld", "pld", "fx", "liaoyang", "jinzhou",
        "dandong", "fushun", "yk", "benxi", "as", "cy", "wfd", "tl", "yangchun", "sw", "taishan",
        "zq", "haifengxian", "mm", "yj", "jm", "zhanjiang", "jy", "sg", "sd", "heyuan", "huidong",
        "st", "mz", "yf", "boluo", "qingyuan", "chaozhou", "huangnan", "xn", "guoluo", "hx", "ys",
        "haidong", "haibei", "hainan"};


    /* 智联招聘 所有工作职能编号 */
    private static final String[] funTypeData = {"yewu", "kefu", "renli", "zplvyoujiudian",
        "zpjiudian", "jiudianzp", "chaoshishangye", "meirongjianshen", "zpanmo", "zpjianshen",
        "zpshengchankaifa", "zpshengchan", "zpqiche", "zpfangchanjianzhu", "zpwuye", "zpfangchan",
        "jiazhengbaojiexin", "siji", "zpshangwumaoyi", "zpwuliucangchu", "zptaobao", "zpmeishu",
        "shichang", "zpguanggao", "zpwentiyingshi", "zhuanye", "zpcaiwushenji", "zpfalvzixun",
        "fanyizhaopin", "zpxiezuochuban", "tech", "zpjixieyiqi", "zpjixie", "jinrongtouzi",
        "zpjinrongbaoxian", "zpyiyuanyiliao", "zpzhiyao", "xiaofeipin", "huanbao", "huagonggy",
        "zhikonganfang", "zpguanli", "nonglinmuyu", "zhaopin", "feiyingli", "zhaopinhui"};
    // </editor-fold>

    private static List<String> areaValueData_hot_list =
        CollectionUtils.arrayToList(areaValueData_hot);
    private static List<String> areaValueData_loney_list =
        CollectionUtils.arrayToList(areaValueData_loney);
    private static List<String> industryData_hot_list =
        CollectionUtils.arrayToList(industryData_hot);
    // 智联所有城市数据字典
    private static List<String> allAreaValueData = new ArrayList<>();
    private static List<String> allIndustryValueData = new ArrayList<>();
    static {
      CollectionUtils.mergeArrayIntoCollection(areaValueData_hot, allAreaValueData);
      CollectionUtils.mergeArrayIntoCollection(areaValueData_loney, allAreaValueData);
      CollectionUtils.mergeArrayIntoCollection(areaValueData, allAreaValueData);
      CollectionUtils.mergeArrayIntoCollection(industryData_hot, allIndustryValueData);
      CollectionUtils.mergeArrayIntoCollection(industryData, allIndustryValueData);
      Increment.firstArea = allAreaValueData.get(0);
      LOGGER.info("*************************************************************");
      LOGGER.info("58同城  热门城市数量: {} 普通城市数量:{} 冷门城市数量:{} 所有城市数量:{},行业数量:{},职能数量:{}",
          areaValueData_hot.length, areaValueData.length, areaValueData_loney.length,
          allAreaValueData.size(), industryData.length, funTypeData.length);
      LOGGER.info("热门行业数量: {} " + ":{} 所有行业数量:{}", industryData_hot.length, industryData.length,
          allIndustryValueData.size());
      LOGGER.info("职能数量:{}", funTypeData.length);
      LOGGER.info("*************************************************************");
    }


  }
}
