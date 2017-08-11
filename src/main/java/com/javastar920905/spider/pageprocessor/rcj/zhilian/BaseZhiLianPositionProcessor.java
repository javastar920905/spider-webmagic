package com.javastar920905.spider.pageprocessor.rcj.zhilian;

import static com.javastar920905.spider.util.SpiderConstantKey.COMPANY_JSON;
import static com.javastar920905.spider.util.SpiderConstantKey.ID;
import static com.javastar920905.spider.util.SpiderConstantKey.POSITION_JSON;
import static com.javastar920905.spider.util.SpiderConstantKey.SOURCE;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.COMPANY_ADDRESS;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.LOGO;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.NAME;
import static com.javastar920905.spider.util.SpiderConstantKey.Company.PROFILE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.CITY;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.COMPANY_LINK;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.COMPANY_NAME;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.DEGREE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.JOB_DESC;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.JOB_NATURE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_LINK;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_NAME;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.POSITION_TITLE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.PUBLISHED_DATE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.SALARY;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.WELFARE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.WORKPLACE;
import static com.javastar920905.spider.util.SpiderConstantKey.Position.WORK_EXPERIENCE;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.javastar920905.spider.util.DateUtil;
import com.javastar920905.spider.util.RedisOpsUtil;
import com.javastar920905.spider.util.SpiderConstantKey;
import com.javastar920905.spider.util.SpiderUtil;
import com.javastar920905.spider.util.StringUtil;

import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

/**
 * Created by ouzhx on 2017/7/6.
 * <p>
 * 51job 职位列表url
 */
public class BaseZhiLianPositionProcessor extends SpiderUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(BaseZhiLianPositionProcessor.class);
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
      String pageSizeStr = html.xpath("/html/body/div[3]/div[3]/div[2]/span[1]/em/text()").get();
      if (pageSizeStr != null && !pageSizeStr.equals("0")) {
        // 截取"共"和"页"之间的数值
        int totalSize = Integer.valueOf(pageSizeStr);
        int rest = totalSize % 60;
        int pageSize = totalSize / 60;
        if (pageSize > 90) {
          return 90;// 智联分页最多显示90条记录
        }
        pageSize = rest > 0 ? (pageSize + 1) : pageSize;
        LOGGER.debug("当前url有 {} 页数据,url为: {}", pageSize, url);
        return pageSize;
      }
      return 0;
    }

    /**
     * 获取职位增量url
     */
    public static class Increment {
      public static String firstArea = "北京";
      public static String firstIndustry = "210500";// 计算机软件行业
      public static String firstfunc = "4010200";// 第一个工作职能
      public static String timeNumber = "1";// 24小时内
      // 从xx地区+计算机软件行业 开始扒取 指定时间内新增url 根据发布时间排序
      public static String pageUrl = "http://sou.zhaopin.com/jobs/searchresult.ashx?bj=" + firstfunc
          + "&in=" + firstIndustry + "&jl=" + firstArea + "&isadv=0&isfilter=1&p=1&pd=1";
      public static final String fistPage = pageUrl;

      /**
       * 生成指定时间段内的增量url (省份+行业)排列組合 *pageSize 并保存到(消息队列或者redis)
       */
      public static void generateIncreUrls() {
        int areaIndex = 0;
        int cityDatalen = allAreaValueData.size();

        try {
          RedisConnection connection = RedisOpsUtil.getRedisConnection();
          for (; areaIndex < cityDatalen; areaIndex++) {
            String provinceNumber = allAreaValueData.get(areaIndex);
            for (String industryNum : allIndustryValueData) {
              // 北京 上海 等热门城市的热门行业 需要+职能单独处理 热门行业14*热门城市13*职能59 =10738 *页码
              if (areaValueData_hot_list.contains(provinceNumber)
                  && industryData_hot_list.contains(industryNum)) {
                for (String func : funTypeData) {
                  // 组合方式: 地区+行业+职能
                  String url = "http://sou.zhaopin.com/jobs/searchresult.ashx?bj=" + func + "&in="
                      + industryNum + "&jl=" + provinceNumber + "&isadv=0&isfilter=1&pd=1";
                  // 获取分页次数 热门行业13*职能59=767 耗时10min
                  int pageSize = getPageSize(url + "&p=1");
                  int pageNum = 1;
                  for (; pageNum <= pageSize; pageNum++) {
                    connection.lPush(RedisOpsUtil.KEY_ZHILIAN_LIST_URLS,
                        (url + "&p=" + pageNum).getBytes());
                  }
                }
              } else {
                // 组合方式:地区+行业
                String url = "http://sou.zhaopin.com/jobs/searchresult.ashx?in=" + industryNum
                    + "&jl=" + provinceNumber + "&isadv=0&isfilter=1&pd=1";
                // 获取分页次数 非热门行业数目38
                int pageSize = getPageSize(url + "&p=1");
                int pageNum = 1;
                // 非热门行业38*城市390=14820 *页码
                for (; pageNum <= pageSize; pageNum++) {
                  connection.lPush(RedisOpsUtil.KEY_ZHILIAN_LIST_URLS,
                      (url + "&p=" + pageNum).getBytes());
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
        List<byte[]> positionUrlList = connection.lRange(RedisOpsUtil.KEY_ZHILIAN_LIST_URLS, 0, 49);
        // 删除前50条url
        connection.lTrim(RedisOpsUtil.KEY_ZHILIAN_LIST_URLS, 49, -1);
        connection.close();
        List<String> urls = RedisOpsUtil.getStringList(positionUrlList);
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
          publishDate = html.xpath("//*[@id=\"span4freshdate\"]/text()").get();

      publishDate = dealPublishDate(publishDate);
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
          json.put(SOURCE, "zhilian");
          return json;
        }
      } catch (Exception e) {
        LOGGER.error("获取页面失败 {}", url, e);
      }
      return null;
    }

    // 发布日期处理,由于增量爬取今天的职位 所以应该只包含当天数据
    private static String dealPublishDate(String rowDateStr) {
      // 日期转换 刚刚 前天,昨天,今天
      // 2小时前 15天前 else
      // 07-21
      if (rowDateStr != null) {
        if ("刚刚".equals(rowDateStr) || "今天".equals(rowDateStr) || rowDateStr.contains("小时前")) {
          return DateUtil.dateFormat(new Date(), date_format);
        } else if ("昨天".equals(rowDateStr)) {
          Date date = DateUtil.getLastFewDays(new Date(), 1);
          return DateUtil.dateFormat(date, date_format);
        } else if ("前天".equals(rowDateStr)) {
          Date date = DateUtil.getLastFewDays(new Date(), 2);
          return DateUtil.dateFormat(date, date_format);
        } else if (rowDateStr.contains("天前")) {
          int range = Integer.valueOf(rowDateStr.substring(0, rowDateStr.indexOf("天前")));
          Date date = DateUtil.getLastFewDays(new Date(), range);
          return DateUtil.dateFormat(date, date_format);
        } else {
          return rowDateStr;
        }
      }
      return null;
    }


    private static JSONObject dealCompanyJson(String url) {
      Html html = SpiderUtil.captureHtml(url, UTF8_CHARSET);
      if (html == null) {
        return null;
      }
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
        json.put(SOURCE, "zhilian");
        return json;
      }
      return null;
    }



    // <editor-fold desc="定义地区 行业 工作职能等数据字典">
    /* 智联招聘 热门行业编号 */
    private static final String[] industryData_hot =
        {"210500", "160400", "160000", "160500", "160200", "180000", "140000", "200300", "200302",
            "170500", "201100", "121300", "210300", "990000"};

    private static final String[] industryData = {"201300", "100000", "100100", "200100", "120600",
        "201200", "130100", "120500", "200700", "130000", "200800", "200600", "121400", "301100",
        "150000", "121500", "300000", "120700", "210600", "121200", "121100", "129900", "121000",
        "120800", "300700", "170000", "120200", "120400", "300300", "201400", "140200", "140100",
        "300900", "300500", "180100", "160600", "160100", "300100"};

    /* 智联招聘 所有省份和直辖市 */
    private static final String[] provinceValueData = {"广东", "湖北", "陕西", "四川", "辽宁", "吉林", "江苏",
        "山东", "浙江", "广西", "安徽", "河北", "山西", "内蒙", "黑龙江", "福建", "江西", "河南", "湖南", "海南", "贵州", "云南",
        "西藏", "甘肃", "青海", "宁夏", "新疆", "香港", "澳门", "台湾省"};


    /* 智联所有热门地区编号 */
    private static final String[] areaValueData_hot =
        {"北京", "上海", "广州", "深圳", "天津", "武汉", "西安", "成都", "南京", "济南", "杭州", "郑州", "惠州"};


    private static final String[] areaValueData = {"韶关", "珠海", "汕头", "佛山", "江门", "湛江", "茂名", "肇庆",
        "梅州", "汕尾", "河源", "阳江", "清远", "东莞", "中山", "潮州", "揭阳", "云浮", "黄石", "十堰", "宜昌", "襄阳", "鄂州",
        "荆门", "孝感", "荆州", "黄冈", "咸宁", "随州", "恩施", "公安", "武穴", "天门", "仙桃", "潜江", "宜城", "神农架", "铜川",
        "宝鸡", "咸阳", "渭南", "延安", "汉中", "榆林", "安康", "商洛", "兴平", "杨凌", "西咸新区", "自贡", "攀枝花", "泸州", "德阳",
        "绵阳", "广元", "遂宁", "内江", "乐山", "南充", "眉山", "宜宾", "广安", "达州", "雅安", "巴中", "资阳", "阿坝", "甘孜",
        "凉山", "峨眉", "西昌", "简阳", "大连", "沈阳", "鞍山", "抚顺", "本溪", "丹东", "锦州", "营口", "阜新", "辽阳", "盘锦",
        "铁岭", "朝阳", "葫芦岛", "兴城", "海城", "昌图", "开原", "东港", "长春", "珲春", "吉林市", "四平", "辽源", "通化", "白山",
        "松原", "白城", "延边", "公主岭", "苏州", "昆山", "常熟", "张家港", "无锡", "江阴", "徐州", "常州", "南通", "连云港", "淮安",
        "盐城", "扬州", "镇江", "泰州", "宿迁", "太仓市", "宜兴", "青岛", "淄博", "枣庄", "东营", "烟台", "潍坊", "济宁", "泰安",
        "威海", "日照", "莱芜", "临沂", "德州", "聊城", "滨州", "菏泽", "宁波", "温州", "嘉兴", "湖州", "绍兴", "金华", "衢州",
        "舟山", "台州", "丽水", "方家山", "南宁", "柳州", "桂林", "梧州", "北海", "防城港", "钦州", "贵港", "玉林", "百色", "贺州",
        "河池", "来宾", "崇左", "合肥", "芜湖", "蚌埠", "淮南", "马鞍山", "淮北", "铜陵", "安庆", "黄山", "滁州", "阜阳", "宿州",
        "六安", "亳州", "池州", "宣城", "凤阳", "广德", "宿松", "石家庄", "唐山", "秦皇岛", "邯郸", "邢台", "保定", "张家口", "承德",
        "沧州", "廊坊", "衡水", "遵化", "太原", "大同", "阳泉", "长治", "晋城", "朔州", "晋中", "运城", "忻州", "临汾", "吕梁",
        "永济市", "呼和浩特", "包头", "乌海", "赤峰", "通辽", "鄂尔多斯", "呼伦贝尔", "兴安盟", "锡林郭勒盟", "乌兰察布", "巴彦淖尔",
        "阿拉善盟", "乌审旗", "满洲里", "哈尔滨", "齐齐哈尔", "鸡西", "鹤岗", "双鸭山", "大庆", "伊春", "佳木斯", "七台河", "牡丹江",
        "黑河", "绥化", "大兴安岭", "安达", "双城", "尚志", "绥芬河", "肇东市", "福州", "厦门", "莆田", "三明", "泉州", "漳州",
        "南平", "龙岩", "宁德", "南昌", "景德镇", "萍乡", "九江", "新余", "鹰潭", "赣州", "吉安", "宜春", "抚州", "上饶", "开封",
        "洛阳", "平顶山", "安阳", "鹤壁", "新乡", "焦作", "濮阳", "许昌", "漯河", "三门峡", "南阳", "商丘", "信阳", "周口", "驻马店",
        "济源", "西平", "长沙", "株洲", "湘潭", "衡阳", "邵阳", "岳阳", "常德", "张家界", "益阳", "郴州", "永州", "怀化", "娄底",
        "湘西", "海口", "三亚", "洋浦市/洋浦经济开发区", "琼海", "儋州", "五指山", "文昌", "万宁", "东方", "定安", "屯昌", "澄迈",
        "临高", "琼中", "保亭", "白沙", "昌江", "乐东", "陵水", "贵阳", "六盘水", "遵义", "安顺", "铜仁", "黔西南", "毕节", "黔东南",
        "黔南", "昆明", "曲靖", "玉溪", "保山", "昭通", "楚雄", "红河", "文山", "西双版纳", "大理", "德宏", "丽江", "怒江", "迪庆",
        "临沧", "普洱", "拉萨", "昌都", "山南", "日喀则", "那曲", "阿里", "林芝", "兰州", "嘉峪关", "金昌", "白银", "天水", "武威",
        "张掖", "平凉", "酒泉", "庆阳", "定西", "陇南", "临夏", "甘南", "西宁", "海东", "海北", "黄南", "海南州", "果洛", "玉树",
        "海西", "银川", "石嘴山", "吴忠", "固原", "中卫", "乌鲁木齐", "克拉玛依", "吐鲁番", "哈密", "昌吉", "博尔塔拉", "巴音郭楞",
        "阿克苏", "克孜勒苏", "喀什", "和田", "伊犁", "塔城", "阿勒泰", "石河子", "奎屯市", "乌苏", "阿拉尔", "图木舒克", "五家渠",
        "北屯市", "香港", "澳门", "台湾省"};

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

    private static List<String> areaValueData_hot_list =
        CollectionUtils.arrayToList(areaValueData_hot);
    private static List<String> industryData_hot_list =
        CollectionUtils.arrayToList(industryData_hot);
    // 智联所有城市数据字典
    private static List<String> allAreaValueData = new ArrayList<>();
    private static List<String> allIndustryValueData = new ArrayList<>();

    static {
      CollectionUtils.mergeArrayIntoCollection(areaValueData_hot, allAreaValueData);
      CollectionUtils.mergeArrayIntoCollection(areaValueData, allAreaValueData);
      CollectionUtils.mergeArrayIntoCollection(industryData_hot, allIndustryValueData);
      CollectionUtils.mergeArrayIntoCollection(industryData, allIndustryValueData);
      Increment.firstArea = allAreaValueData.get(0);
      LOGGER.info("*************************************************************");
      LOGGER.info("热门城市数量: {} 普通城市数量:{} 所有城市数量:{},行业数量:{},职能数量:{}", areaValueData_hot.length,
          areaValueData.length, allAreaValueData.size(), industryData.length, funTypeData.length);
      LOGGER.info("热门行业数量: {} 普通行业数量:{} 所有行业数量:{}", industryData_hot.length, industryData.length,
          allIndustryValueData.size());
      LOGGER.info("职能数量:{}", funTypeData.length);
      LOGGER.info("*************************************************************");
    }


  }
}
