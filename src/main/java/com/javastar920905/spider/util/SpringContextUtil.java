package com.javastar920905.spider.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Created by chnejun on 2016/3/14.
 */

@Component
public class SpringContextUtil implements ApplicationContextAware {

  // Spring应用上下文环境
  private static ApplicationContext applicationContext;

  public static void printIocContextBeanNames() {
    if (applicationContext == null) {
      System.err.println("spring 容器加载失败");
    }
    for (String beanName : applicationContext.getBeanDefinitionNames()) {
      System.out.println(beanName);
    }

  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    SpringContextUtil.applicationContext = applicationContext;
  }

  /**
   * 获取Spring ApplicationContext
   * 
   * @return 返回ApplicationContext实例
   */
  public static ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  /**
   * 获取对象
   * 
   * @param name bean注册名
   * @return Object 一个以所给名字注册的bean的实例
   * @throws BeansException
   */
  public static Object getBean(String name) throws BeansException {
    return applicationContext.getBean(name);
  }

  /**
   * 获取类型为requiredType的对象 如果bean不能被类型转换，相应的异常将会被抛出（BeanNotOfRequiredTypeException）
   * 
   * @param name bean注册名
   * @param requiredType 返回对象类型
   * @return Object 返回requiredType类型对象
   * @throws BeansException
   */
  public static <T> T getBean(String name, Class requiredType) throws BeansException {
    return (T) applicationContext.getBean(name, requiredType);
  }

  /**
   * 根据类型获取bean
   * 
   * @param requiredType
   * @param <T>
   * @return
   * @throws BeansException
   */
  public static <T> T getBean(Class requiredType) throws BeansException {
    return (T) applicationContext.getBean(requiredType);
  }

  /**
   * 如果BeanFactory包含一个与所给名称匹配的bean定义，则返回true
   * 
   * @param name
   * @return boolean
   */
  public static boolean containsBean(String name) {
    return applicationContext.containsBean(name);
  }

  /**
   * 判断以给定名字注册的bean定义是一个singleton还是一个prototype。
   * 如果与给定名字相应的bean定义没有被找到，将会抛出一个异常（NoSuchBeanDefinitionException）
   * 
   * @param name
   * @return boolean
   * @throws NoSuchBeanDefinitionException
   */
  public static boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
    return applicationContext.isSingleton(name);
  }

  /**
   * @param name
   * @return Class 注册对象的类型
   * @throws NoSuchBeanDefinitionException
   */
  public static Class getType(String name) throws NoSuchBeanDefinitionException {
    return applicationContext.getType(name);
  }

  /**
   * 如果给定的bean名字在bean定义中有别名，则返回这些别名
   * 
   * @param name
   * @return
   * @throws NoSuchBeanDefinitionException
   */
  public static String[] getAliases(String name) throws NoSuchBeanDefinitionException {
    return applicationContext.getAliases(name);
  }

}
