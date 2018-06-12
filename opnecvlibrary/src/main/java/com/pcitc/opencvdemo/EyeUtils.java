package com.pcitc.opencvdemo;

import java.util.LinkedList;

/**
 * Created by lcj on 2017/3/18.
 * 检测眨眼的工具
 */

public class EyeUtils {

    private static LinkedList<Integer> eyeList=new LinkedList<>();

    /**
     * 将每次检测到的眼睛数量存入集合
     * @param eyeCount
     */
    public static void put(int eyeCount){
        int size=eyeList.size();
        if(size==0||eyeList.getLast()!=eyeCount){
            eyeList.add(eyeCount);
        }
        if(size>3){
            eyeList.removeFirst();
        }
    }

    /**
     * 检查是否完成两次眨眼
     * @return
     */
    public static boolean check(){
        int size=eyeList.size();
        return size==3&&eyeList.getFirst()>0&&eyeList.get(1)==0&&eyeList.getLast()>0;
    }

    /**
     * 将缓存清空，方便下次使用
     */
    public static void clearEyeCount(){
        eyeList.clear();
    }

}
