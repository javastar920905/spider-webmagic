package com.javastar920905.spider.util;

import com.javastar920905.spider.util.api.dama.DamaOpsUtil;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import javax.imageio.ImageIO;

/**
 * Created by ouzhx on 2017/8/7. 图片下载 (视频下载地址 一般都隐藏了)
 */
public class ImageSpider {
  // 当前项目的根目录下会自动生成
  private static final String defaultSavePath = "images";

  /**
   * 从网络Url中下载文件
   * 
   * @param urlStr
   * @param fileName 文件名不能有/等路径,否则报错 (正确名称为 xx.png)
   * @param savePath 保存文件路径不能为空
   * @throws IOException
   */
  public static String downLoadFromUrl(String urlStr, String fileName, String savePath) {
    URL url = null;
    String base64String = null;
    if (savePath == null) {
      savePath = defaultSavePath;
    }
    try {
      url = new URL(urlStr);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      // 设置超时间为3秒
      conn.setConnectTimeout(3 * 1000);
      // 防止屏蔽程序抓取而返回403错误
      conn.setRequestProperty("User-Agent",
          "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

      // 得到输入流
      InputStream inputStream = conn.getInputStream();
      // 获取字节数组
      byte[] getData = readInputStream(inputStream);


      // 文件保存位置
      File saveDir = new File(savePath);
      if (!saveDir.exists()) {
        saveDir.mkdir();
      }
      File file = new File(saveDir + File.separator + fileName);
      FileOutputStream fos = new FileOutputStream(file);
      fos.write(getData);
      if (fos != null) {
        fos.close();
      }
      if (inputStream != null) {
        inputStream.close();
      }
      base64String = DamaOpsUtil.convertImageToByte(file.getPath(), "png");
    } catch (IOException e) {
      e.printStackTrace();
    }
    // System.out.println("info:" + url + " download success");
    return base64String;
  }

  // 获取网络图片字节数组
  public static byte[] downLoadFromUrl(String urlStr, String fileName) {
    URL url = null;
    byte[] imageByteData = null;
    try {
      url = new URL(urlStr);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      // 设置超时间为3秒
      conn.setConnectTimeout(3 * 1000);
      // 防止屏蔽程序抓取而返回403错误
      conn.setRequestProperty("User-Agent",
          "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

      // 得到输入流
      InputStream inputStream = conn.getInputStream();
      // 获取字节数组
      imageByteData = readInputStream(inputStream);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return imageByteData;
  }



  /**
   * 从输入流中获取字节数组
   * 
   * @param inputStream
   * @return
   * @throws IOException
   */
  private static byte[] readInputStream(InputStream inputStream) throws IOException {
    byte[] buffer = new byte[1024];
    int len = 0;
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    while ((len = inputStream.read(buffer)) != -1) {
      bos.write(buffer, 0, len);
    }
    bos.close();
    return bos.toByteArray();
  }



}
