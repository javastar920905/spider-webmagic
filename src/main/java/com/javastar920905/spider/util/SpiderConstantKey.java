package com.javastar920905.spider.util;

/**
 * 爬虫常量类(对应<k,v>结构中的k)
 * Created by ouzhx on 2017/7/26.
 */
public interface SpiderConstantKey {
  String COMPANY_JSON = "companyJson";
  String POSITION_JSON = "positionJson";
  String ID = "id";
  String SOURCE = "source";

  interface Position {
    // 职位链接
    String POSITION_LINK = "positionLink";
    // 工作职能
    String POSITION_TITLE = "positionTitle";
    // 公司名称
    String COMPANY_NAME = "companyName";
    // 公司链接
    String COMPANY_LINK = "companyLink";

    // 职位名称
    String POSITION_NAME = "positionName";
    // 职位描述
    String JOB_DESC = "jobDesc";
    // 标签列表(对应51job关键字列表)
    String LABEL_LIST = "labelList";
    // 工作类型(全职/兼职)
    String JOB_NATURE = "jobNature";
    // 学历要求
    String DEGREE = "degree";
    // 是否统招
    String RECRUITMENT = "recruitment";
    // 工作年限
    String WORK_EXPERIENCE = "workExperience";
    // 薪资待遇
    String SALARY = "salary";
    // 年龄要求
    String AGE_RANGE = "ageRange";
    // 语言要求
    String POSITION_LANGUAGE_REQUIREMENTS = "positionLanguageRequirements";
    // 专业要求
    String POSITION_MAJOR_REQUIREMENT = "positionMajorRequirement";
    // 执业证书要求
    String POSITION_CERTIFICATE = "positionCertificate";
    // 身高
    String HEIGHT = "height";
    // 性别
    String SEX = "sex";
    // 岗位职责
    String POSITION_RESPONSIBILITY = "positionResponsibility";
    // 任职要求
    String POSITION_REQUIREMENTS = "positionRequirements";
    // 公司福利
    String WELFARE = "welfare";
    // 工作地点
    String WORKPLACE = "workPlace";
    //所在城市
    String CITY = "city";
    // 接收简历邮箱
    String EMAIL = "email";
    // 发布时间
    String PUBLISHED_DATE = "publishedDate";
    // 行业
    String INDUSTRY = "industry";
  }

  interface Company {
    // 企业名称
    String NAME = "name";
    // 企业头像
    String LOGO = "logo";
    // 公司行业
    String INDUSTRY = "industry";
    // 公司阶段 (不需要融资/初创公司/...)
    String COMPANY_STAGE = "companyStage";
    // 公司类型 (民营公司/上市公司/...)
    String COMPANY_NATURE = "companyNature";
    // 公司规模(50-100人)
    String COMPANY_SCALE = "companyScale";
    // 公司电话
    String COMPANY_PHONE = "companyPhone";
    // 办公环境
    String OFFICE_ENVIRONMENT = "officeEnvironment";
    // 公司官网
    String WEBSITE = "website";
    // 公司地址
    String COMPANY_ADDRESS = "companyAddress";
    // 公司简介
    String PROFILE = "profile";
    // 数据来源
    String SOURCE = "source";
  }
}
