package com.javastar920905.spider.util.api.dama;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * Created by ouzhx on 2017/8/25.
 */
public class DamaOpsUtil {
  private static Base64.Encoder encoder = Base64.getEncoder();
  private static Base64.Decoder decoder = Base64.getDecoder();

  /**
   * 识别图片验证码(4位字母数字混编的)
   *
   * @param base64Data base64编码的图片
   */

  public static String fourAlphanumbericalRecognize(String base64Data) {
    ByteArrayInputStream is =
        new ByteArrayInputStream(Base64.getDecoder().decode(base64Data.getBytes()));
    byte[] data = new byte[is.available()];
    try {
      is.read(data);
    } catch (IOException e) {
      System.out.println("要识别的图片格式有误");
      e.printStackTrace();
    }
    return getValidateCode(data, 42);
  }



  /**
   * 根据文件名称解析验证码
   *
   * @param bytes 验证码图片的字节数组
   * @return 解析成功后的验证码
   */
  public static String getValidateCode(byte[] bytes, Integer type) {
    /*
     * 人才加申请的账号密码 uname = "rencaijia"; upwd = "1qaz2wsx";
     */
    String key = "4f35f7cc1cea88aab7695fcb3d0439a1";
    String uname = "rencaijia";
    String upwd = "1qaz2wsx";
    int idApp = 45223;
    try {
      Dama web = new Dama(idApp, key, uname, upwd);
      // 三个参数分别表示验证码类型（42是4位中英文混合）,超时时间,bytes
      Dama.DecodeResult deResult = web.decodeAndGetResult(type, 5, bytes);
      return deResult.result;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * 将图片转换为BASE64加密字符串.
   *
   * @param imagePath 图片路径.
   * @param format 图片格式.(png/jpg)
   * @return
   */
  public static String convertImageToByte(String imagePath, String format) {
    File file = new File(imagePath);
    String result = null;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      BufferedImage bi = ImageIO.read(file);
      ImageIO.write(bi, format == null ? "jpg" : format, baos);
      byte[] bytes = baos.toByteArray();
      result = encoder.encodeToString(bytes); // encoder.encodeBuffer(bytes).trim();
      // System.out.println("将图片转换为BASE64加密字符串成功！");
    } catch (IOException e) {
      System.err.println("将图片转换为 BASE64加密字符串失败: " + e);
    }
    return result;
  }

  /**
   * 将图片流转换为BASE64加密字符串.
   *
   * @param imageInputStream
   * @param format 图片格式.
   * @return
   */
  public static String convertImageStreamToByte(InputStream imageInputStream, String format) {
    String result = null;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      BufferedImage bi = ImageIO.read(imageInputStream);
      ImageIO.write(bi, format == null ? "jpg" : format, baos);
      byte[] bytes = baos.toByteArray();
      result = encoder.encodeToString(bytes).trim();
      System.out.println("将图片流转换为BASE64加密字符串成功！");
    } catch (IOException e) {
      System.err.println("将图片流转换为 BASE64加密字符串失败: " + e);
    }
    return result;
  }

  /**
   * 将BASE64加密字符串转换为图片.
   *
   * @param base64String
   * @param imagePath 图片生成路径.
   * @param format 图片格式.
   */
  public static void convertByteToImage(String base64String, String imagePath, String format) {
    byte[] bytes = null;
    ByteArrayInputStream bais = null;
    BufferedImage bi = null;
    File file = null;
    try {
      bytes = decoder.decode(base64String);
      bais = new ByteArrayInputStream(bytes);
      bi = ImageIO.read(bais);
      file = new File(imagePath);
      ImageIO.write(bi, format == null ? "jpg" : format, file);
      System.out.println("将BASE64加密字符串转换为图片成功！");
    } catch (IOException e) {
      System.out.println("将BASE64加密字符串转换为图片失败: " + e);
    } finally {
      try {
        if (bais != null) {
          bais.close();
        }
      } catch (Exception e) {
        System.err.println("关闭文件流发生异常: " + e);
      }
    }
  }
}
