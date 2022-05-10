package com.wlznsb.iossupersign.util;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

import okhttp3.OkHttpClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

@Slf4j
public class MyUtil {



    public static OkHttpClient okHttpClient;

    /*
     * 随机生成国内IP地址
     */
    public static String getRandomIp(){

        //ip范围
        int[][] range = {{607649792,608174079},//36.56.0.0-36.63.255.255
                {1038614528,1039007743},//61.232.0.0-61.237.255.255
                {1783627776,1784676351},//106.80.0.0-106.95.255.255
                {2035023872,2035154943},//121.76.0.0-121.77.255.255
                {2078801920,2079064063},//123.232.0.0-123.235.255.255
                {-1950089216,-1948778497},//139.196.0.0-139.215.255.255
                {-1425539072,-1425014785},//171.8.0.0-171.15.255.255
                {-1236271104,-1235419137},//182.80.0.0-182.92.255.255
                {-770113536,-768606209},//210.25.0.0-210.47.255.255
                {-569376768,-564133889}, //222.16.0.0-222.95.255.255
        };

        Random rdint = new Random();
        int index = rdint.nextInt(10);
        String ip = num2ip(range[index][0]+new Random().nextInt(range[index][1]-range[index][0]));
        return ip;
    }

    /*
     * 将十进制转换成ip地址
     */
    public static String num2ip(int ip) {
        int [] b=new int[4] ;
        String x = "";

        b[0] = (int)((ip >> 24) & 0xff);
        b[1] = (int)((ip >> 16) & 0xff);
        b[2] = (int)((ip >> 8) & 0xff);
        b[3] = (int)(ip & 0xff);
        x=Integer.toString(b[0])+"."+Integer.toString(b[1])+"."+Integer.toString(b[2])+"."+Integer.toString(b[3]);

        return x;
    }

    /**
     * ipa里面的图片转换
     * @param inputPath ./data/superdown.html.png
     * @param outPath ./data/1.png
     * @return
     * @throws IOException
     */
    public static void getIpaImg(String inputPath,String outPath) throws IOException {


        File file = new File(inputPath);
        BufferedInputStream inputStream = FileUtil.getInputStream(file);
        BufferedInputStream inputStream1 = FileUtil.getInputStream(file);

        //如果图片损坏则处理
        if(!GetIpaInfoUtil.verifyImage(inputStream)){
            new IPngConverter(file, new File(outPath)).convert();
        }else {
            FileUtil.writeBytes(inputStream1.readAllBytes(),new File(outPath));
        }
        inputStream.close();
        inputStream1.close();

    };



    /**
     * 获取okttp
     * @return
     */
    public static OkHttpClient getOkHttpClient() {
        if(null == okHttpClient){
            okHttpClient = new OkHttpClient().newBuilder().build();
        }
        return okHttpClient;
    }

    /**
     * MultipartFile 文件写入 目录不存在自动创建文件夹
     * @param multipartFile
     * @return
     */
    public static void MultipartFileWrite(MultipartFile multipartFile,String path) throws IOException {

        cn.hutool.core.io.FileUtil.writeBytes(multipartFile.getBytes(),new File(path).getAbsolutePath());

    }

    /**
     * 获取uuid
     * @return
     */
    public static String getUuid(){
        String uuid = UUID.randomUUID().toString();
        uuid = uuid.replace("-", "");
        return uuid;
    }


    /**
     * MultipartFile获取文件后缀
     *
     *
     */
    public static String getMultipartFileSuffix(MultipartFile multipartFile){
        return multipartFile.getOriginalFilename().substring(multipartFile.getOriginalFilename().lastIndexOf(".") + 1);
    }



    /**
     * 传入执行的命令
     * 返回map,有两个key,一个是status一个是info
     * status返回执行状态码 0.执行成功 1.命令执行失败 -1.系统无法执行命令或命令不存在
     * info返回结果信息
     * 没有采用多线程,会导致线程阻塞
     * @param cmd
     * @return
     */
    public static  Map<String,Object> runtimeExec(String cmd) {
        Map<String,Object> map = new HashMap<String, Object>();

        try {
            Process p;
            //执行命令
            p = Runtime.getRuntime().exec(cmd);
            InputStream fis;
            //判断运行结果
            if(p.waitFor() == 0){
                //取得命令结果的输出流
                fis=p.getInputStream();
                map.put("status", 0);
            }else {
                //取得命令结果的输出流
                fis=p.getErrorStream();
                map.put("status", 1);
            }
            //用一个读输出流类去读
            InputStreamReader isr=new InputStreamReader(fis);
            //用缓冲器读行
            BufferedReader br=new BufferedReader(isr);
            String line = "";
            String result = "";
            //直到读完为止
            while((line=br.readLine())!=null)
            {
                result = result + line;
            }
            map.put("info", result);

            fis.close();
        }  catch (Exception e)
        {
            map.put("status", -1);
            map.put("info", e.toString());
        }finally {
            return map;
        }
    }


    /**
     * 清空某个目录
     * @param path
     * @return
     */
    public static boolean deleteDir(String path) {
        File file = new File(path);
        if (!file.exists()) {//判断是否待删除目录是否存在
            System.err.println("The dir are not exists!");
            return false;
        }
        String[] content = file.list();//取得当前目录下所有文件和文件夹
        for (String name : content) {
            File temp = new File(path, name);
            if (temp.isDirectory()) {//判断是否是目录
                deleteDir(temp.getAbsolutePath());//递归调用，删除目录里的内容
                temp.delete();//删除空目录
            } else {
                if (!temp.delete()) {//直接删除文件
                    System.err.println("Failed to delete " + name);
                }
            }
        }
        return true;
    }

    /**
     * 移动文件到某个目录
     * @param filePath 文件名
     * @param dirPath 要移到的目录
     */
    public static void moveFile(String filePath,String dirPath) {

        File file = new File(filePath);
        File dir = new File(dirPath);
        file.renameTo(new File(dir + "/" + file.getName()));
    }

    /**
     * 复制文件到某个目录
     * @param filePath 文件路径
     * @param dirPath  目标文件路径
     * @throws IOException
     */
    public static void copyFile(String filePath,String dirPath) throws IOException {

        File file = new File(filePath);
        File dir = new File(dirPath);
        Files.copy(file.toPath(), dir.toPath());

    }


    /**
     * 获取精确到秒的时间戳
     * @return
     */
    public static Long getSecondTimestamp(Date date){
        String timestamp = String.valueOf(date.getTime());
        int length = timestamp.length();
        return Long.valueOf(timestamp.substring(0,length-3));
    }


}
