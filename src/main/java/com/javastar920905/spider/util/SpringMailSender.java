package com.javastar920905.spider.util;


import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Created by ouzhx on 2017/3/15.
 */
public class SpringMailSender {
  // Spring的邮件工具类，实现了MailSender和JavaMailSender接口
  private static final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
  private static final Logger LOGGER = LoggerFactory.getLogger(SpringMailSender.class);

  static {
    // 设置为企业邮箱和发件人信息
    mailSender.setHost("");
    mailSender.setUsername("");
    mailSender.setPassword("");
  }

  /**
   * 简单邮件发送
   *
   */
  public static synchronized void simpleSend(String to, String subject, String content) {
    SimpleMailMessage smm = new SimpleMailMessage();
    try {
      smm.setFrom(mailSender.getUsername());
      smm.setTo(to);
      smm.setSubject(subject);
      smm.setText(content);
      mailSender.send(smm);
      LOGGER.info("发送邮件成功! to:{} 发送内容为:{}", to, content == null ? "" : content.toString());
    } catch (MailException e) {
      try {
        // 发送失败的情况下,10秒后重试 调度程序无关乎性能在乎稳定性
        Thread.sleep(10000);
        mailSender.send(smm);
        LOGGER.info("发送邮件成功! to:{} 发送内容为:{}", to, content == null ? "" : content.toString());
      } catch (Exception e2) {
        LOGGER.error("发送邮件程序出错!", e2);
      }

    }
  }


  /**
   * 发送带附件的邮件
   *
   */
  public static synchronized void sendAttach(String to, String subject, String content,
      String fileName, InputStreamSource file) {
    // 建立邮件消息,发送简单邮件和html邮件的区别 需要javax.mail依赖
    MimeMessage mailMessage = mailSender.createMimeMessage();
    try {
      // multipart模式 为true时发送附件 可以设置html格式
      MimeMessageHelper messageHelper = new MimeMessageHelper(mailMessage, true, "utf-8");
      messageHelper.setTo(to);
      messageHelper.setFrom(mailSender.getUsername());
      messageHelper.setSubject(subject);
      // true 表示启动HTML格式的邮件
      messageHelper.setText("<html><head></head><body><h1>" + content + "</h1></body></html>",
          true);
      // 这里的方法调用和插入图片是不同的。
      messageHelper.addAttachment(fileName, file);
      // 发送邮件
      mailSender.send(mailMessage);
    } catch (MessagingException ex) {
      try {
        Thread.sleep(10000);
        mailSender.send(mailMessage);
      } catch (Exception e) {
        LOGGER.error("发送" + subject + "邮件出错!", e);
      }
    }

  }


}
