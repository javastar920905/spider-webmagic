package com.javastar920905.spider.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ouzhx on 2017/7/8.
 */
public class CollectionUtil {
  // byte[] 集合转String 集合
  public static List<String> getStringList(Collection<byte[]> collection) {
    List<String> list = new LinkedList<>();
    Iterator<byte[]> it = collection.iterator();
    it.forEachRemaining(item -> {
      list.add(new String(item));
    });
    return list;
  }
}
