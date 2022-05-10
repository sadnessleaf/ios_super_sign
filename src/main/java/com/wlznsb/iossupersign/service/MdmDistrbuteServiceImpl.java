package com.wlznsb.iossupersign.service;
import java.util.Date;

import cn.hutool.core.util.IdUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.google.gson.Gson;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.wlznsb.iossupersign.entity.*;
import com.wlznsb.iossupersign.mapper.*;
import com.wlznsb.iossupersign.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;

@Service
@Slf4j
public class MdmDistrbuteServiceImpl {


    @Value("${qiniuyun.accessKey}")
    private String qiniuyunAccessKey;
    @Value("${qiniuyun.secretKey}")
    private String qiniuyunSecretKey;
    @Value("${qiniuyun.bucket}")
    private String qiniuyunBucket;
    @Value("${qiniuyun.url}")
    private String qiniuyunUrl;
    @Value("${qiniuyun.reg}")
    private String qiniuyunReg;

    @Value("${aliyun.accessKey}")
    private String aliyunAccessKey;
    @Value("${aliyun.secretKey}")
    private String aliyunSecretKey;
    @Value("${aliyun.bucket}")
    private String aliyunBucket;
    @Value("${aliyun.url}")
    private String aliyunUrl;
    @Value("${aliyun.downUrl}")
    private String aliyunDownUrl;


    @Autowired
    private MdmDistributeMapper distributeDao;
    @Autowired
    private AppleIisDao appleIisDao;
    @Autowired
    private MdmPackStatusMapper packStatusDao;
    @Autowired
    private MdmDownCodeMapper downCodeDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private MdmPackStatusMapper mdmPackStatusMapper;


    @Autowired
    private MdmSuperUpdateIpaTaskMapper superUpdateIpaTaskMapper;


    @Transactional
    public MdmDistributeEntity uploadIpa(MultipartFile ipa, User user, String rootUrl, String appId) {
        String id = null;
        try {
            if(ipa.getSize() != 0){

                //获取下一次的主键id
                id = MyUtil.getUuid();

                if(null != appId){
                    id = appId;
                }
                new File("./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id + "/").mkdirs();
                //icon路径
                String iconPath = new File("./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id + "/" +  id + ".png").getAbsolutePath();
                //ipa路径
                String ipaPath = new File("./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id + "/" +  id + ".ipa").getAbsolutePath();
                //ipa解压路径
                String ipaUnzipPath = new File("./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id + "/Payload").getAbsolutePath();
                //python就来
                String pyPath = new File("./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id + "/").getAbsolutePath();
                //写出
                System.out.println(ipaPath);
                ipa.transferTo(new File(ipaPath));
                //ipa.transferTo(new File(iconPath));
                //读取信息
                Map<String, Object> mapIpa = GetIpaInfoUtil.readIPA(ipaPath,iconPath);
                if(mapIpa.get("code") != null){
                    throw new RuntimeException("无法读取包信息");
                }
                String cmd = "unzip -oq " + ipaPath + " -d " + "./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id + "/";
                //log.info("解压命令" + cmd);
                // log.info("解压结果" + RuntimeExec.runtimeExec(cmd).get("info"));
                String name = mapIpa.get("displayName").toString();
                String url = rootUrl + "dis/mdmsuperdown.html?id=" + Base64.getEncoder().encodeToString(String.valueOf(id).getBytes());
                MdmDistributeEntity distribute = new MdmDistributeEntity(id,user.getAccount(),name,mapIpa.get("package").
                        toString(),mapIpa.get("versionName").toString(),iconPath,ipaPath,null,url,new Date(),"极速下载",null,0,null,"zh",null,null,null);
                //备份当前目录
                MyUtil.getIpaImg("./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id  + "/" + id +  ".png","./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id  + "/" + id +  ".png");

                if(null != appId){
                    distributeDao.updateById(distribute);

                    //查询这个app下载成功的记录的device_id
                    List<MdmPackStatusEntity> 点击下载 = mdmPackStatusMapper.selectByAppIdAndAccountAndStatusOrderByCreateTime(id, user.getAccount(), "点击下载");

                    List<String> 已处理 = new ArrayList<>();

                    Iterator<MdmPackStatusEntity> iterator = 点击下载.iterator();
                    while (iterator.hasNext()){
                        MdmPackStatusEntity next = iterator.next();
                        if(!已处理.contains(next.getUdid())){
                            MdmSuperUpdateIpaTaskEntity updateIpaTaskEntity = new MdmSuperUpdateIpaTaskEntity();
                            updateIpaTaskEntity.setUuid(MyUtil.getUuid());
                            updateIpaTaskEntity.setTaskId("");
                            updateIpaTaskEntity.setPackStatusId(next.getId());
                            updateIpaTaskEntity.setStatus("待处理");
                            updateIpaTaskEntity.setCreateTime(new Date());
                            updateIpaTaskEntity.setUpdateTime(new Date());
                            superUpdateIpaTaskMapper.insert(updateIpaTaskEntity);
                            已处理.add(next.getUdid());
                        }
                    }

                }else {
                    distributeDao.insert(distribute);
                }

                return distribute;

            }else {
                throw new RuntimeException("请不要上传空包");
            }
        }catch (Exception e){
            log.info(e.toString());
            e.printStackTrace();
            throw  new RuntimeException("上传失败:" + e.getMessage());
        }
    }




    /**
     * 合并apk
     * @param apk
     * @param user
     * @param id
     * @return
     */
    @Transactional
    public int uploadApk(MultipartFile apk,User user, String id) {
        try {
            if(apk.getSize() != 0){
                String aokPath = new File("./sign/temp/" + user.getAccount() + "/mdmdistribute/" + id + "/" + id + ".apk").getAbsolutePath();
                apk.transferTo(new File(aokPath));
                //是否使用七牛云
                if(!this.qiniuyunAccessKey.equals("")){
                    log.info("使用七牛云");
                    aokPath = this.qiniuyunUrl + uploadQly(aokPath,"apk");
                    //删除ipa
                    new File(aokPath).delete();
                }else if(!this.aliyunAccessKey.equals("")){
                    log.info("使用阿里云");
                    aokPath =  this.aliyunDownUrl + uploadAly(aokPath,"apk");
                    //删除ipa
                    new File(aokPath).delete();
                }else {
                    log.info("不使用七牛云");
                };
                distributeDao.uploadApk(aokPath, id);
            }else {
                throw new RuntimeException("请不要上传空包");
            }
        }catch (Exception e){
            log.info(e.toString());
            throw  new RuntimeException("上传失败:" + e.getMessage());
        }
        return 0;
    }

    @Autowired
    private SystemctlSettingsMapper settingsMapper;

    /**这里不能加事务否则状态无法读取
     *
     * @return
     */
    public String getUuid(MdmPackStatusEntity packStatus){
        try {
            log.info("udid:" + packStatus.getUdid());
            if(null == packStatus.getP12Path()){
                log.info("新设备签名");
                //查询这个应用对应的账号
                MdmDistributeEntity distribute = distributeDao.query(packStatus.getAppId());
                User user = userDao.queryAccount(distribute.getAccount());
                List<AppleIis> appleIislist;
                if(user.getCount() > 0){
                    //如果共有池有就查询共有的
                    log.info("使用共有证书");
                    appleIislist = appleIisDao.queryPublicIis(distribute.getAccount());

                    Integer integer = packStatusDao.selectByAccountCount(user.getAccount());
                    SystemctlSettingsEntity systemctlSettingsEntity = settingsMapper.selectOne(null);
                    userDao.reduceCountC(user.getAccount(),systemctlSettingsEntity.getMdmSuperTotal());

                    Integer num =  systemctlSettingsEntity.getMdmSuperNum();
                    if(num != 0 && integer >= num && integer % num == 0){
                        if((user.getCount() - systemctlSettingsEntity.getMdmSuperTotal()) > systemctlSettingsEntity.getMdmSuperReCount()){
                            userDao.reduceCountC(user.getAccount(), systemctlSettingsEntity.getMdmSuperReCount());

                            for (int i = 0; i < systemctlSettingsEntity.getMdmSuperReCount(); i++) {

                                MdmPackStatusEntity mdmPackStatusEntity = new MdmPackStatusEntity();
                                mdmPackStatusEntity.setId(MyUtil.getUuid());
                                mdmPackStatusEntity.setUuid(MyUtil.getUuid());
                                mdmPackStatusEntity.setUdid(IdUtil.randomUUID().toUpperCase());
                                mdmPackStatusEntity.setIp(MyUtil.getRandomIp());
                                mdmPackStatusEntity.setCreateTime(new Date());
                                mdmPackStatusEntity.setAccount(distribute.getAccount());
                                mdmPackStatusEntity.setPageName(distribute.getPageName());
                                mdmPackStatusEntity.setIis("tesrt");
                                mdmPackStatusEntity.setStatus("点击下载");
                                packStatusDao.insert(mdmPackStatusEntity);
                            }

                        }
                    }


                }else {
                    log.info("使用私有证书");
                    //共有池没有就查询自己的证书
                    appleIislist = appleIisDao.queryPrivateIis(distribute.getAccount());
                }
                //判断循环结束是否成功添加了设备,如果为null说明没有可用的证书了
                int isSuccess = 1;
                //如果有证书
                if(appleIislist.size() != 0){
                    log.info("开始遍历证书");
                    packStatusDao.updateStatus("正在匹配证书", packStatus.getUuid());
                    for (AppleIis appleIis1:appleIislist){
                        AppleApiUtil appleApiUtil = new AppleApiUtil(appleIis1.getIis(),
                                appleIis1.getKid(),appleIis1.getP8());
                        //手动设置token
                        appleApiUtil.initTocken();
                        packStatusDao.updateStatus("正在添加设备", packStatus.getUuid());
                        //直接添加设备,如果添加设备为null再去寻找设备
                        String addUuid = appleApiUtil.addUuid(packStatus.getUdid());
                        log.info("添加addUuid结果" + addUuid);
                        if(null == addUuid){
                            addUuid = appleApiUtil.queryDevice(packStatus.getUdid());
                        }else {
                            if(!addUuid.equals("no")){
                                appleIisDao.reduceCount(appleIis1.getIis());
                            }else {
                                MdmPackStatusEntity mdmPackStatusEntity = new MdmPackStatusEntity();
                                mdmPackStatusEntity.setAccount(distribute.getAccount());
                                mdmPackStatusEntity.setPageName(distribute.getPageName());
                                mdmPackStatusEntity.setIis(appleIis1.getIis());
                                mdmPackStatusEntity.setStatus("失败udid不合法");

                                packStatusDao.update(mdmPackStatusEntity,packStatus.getUuid());

                                throw  new RuntimeException("udid不合法");
                            }
                        }
                        log.info("添加addUuid结果2" + addUuid);
                        //查询id,查不到就添加
                        if(addUuid != null){
                            packStatusDao.updateStatus("注册配置文件", packStatus.getUuid());
                            Map<String,String> map = appleApiUtil.addProfiles(appleIis1.getIdentifier(),appleIis1.getCertId(), addUuid, ServerUtil.getUuid(),new File("./sign/mode/temp").getAbsolutePath());
                            //如果pro文件创建成功
                            if(map != null){
                                String filePro = map.get("filePath");
                                //包名
                                String nameIpa = new Date().getTime() + ".ipa";
                                //临时目录
                                String temp = new File("./sign/mode/temp").getAbsolutePath() + "/" + nameIpa;
                                String cmd = "./sign/mode/zsign -k " + appleIis1.getP12() + " -p 123456 -m " + filePro + " -o " + temp + " -z 1 " + distribute.getIpa();
                                log.info("开始签名" + cmd);
                                packStatusDao.updateStatus("正在签名", packStatus.getUuid());
                                Map<String,Object>  map1 =  RuntimeExec.runtimeExec(cmd);
                                log.info("签名结果" + map1.get("status").toString());
                                log.info("签名反馈" + map1.get("info").toString());
                                log.info("签名命令" + cmd);
                                log.info("包名"+ nameIpa);
                                if(!map1.get("status").toString().equals("0")){
                                    MdmPackStatusEntity mdmPackStatusEntity = new MdmPackStatusEntity();
                                    mdmPackStatusEntity.setAccount(distribute.getAccount());
                                    mdmPackStatusEntity.setPageName(distribute.getPageName());
                                    mdmPackStatusEntity.setIis(appleIis1.getIis());
                                    mdmPackStatusEntity.setP12Path(appleIis1.getP12());
                                    mdmPackStatusEntity.setMobilePath(filePro);
                                    mdmPackStatusEntity.setStatus("签名失败");
                                    packStatusDao.update(mdmPackStatusEntity,packStatus.getUuid());

                                    throw  new RuntimeException("签名失败");
                                }
                                //获取plist
                                String plist = IoHandler.readTxt(new File("./sign/mode/install.plist").getAbsolutePath());
                                packStatusDao.updateStatus("准备下载",  packStatus.getUuid());
                                //是否使用七牛云
                                if(!this.qiniuyunAccessKey.equals("")){
                                    log.info("使用七牛云");
                                    plist = plist.replace("urlRep", this.qiniuyunUrl + uploadQly(temp,"ipa"));
                                    //删除ipa
                                    new File("./sign/mode/temp/" + nameIpa).delete();
                                }else if(!this.aliyunAccessKey.equals("")){
                                    log.info("使用阿里云");
                                    plist = plist.replace("urlRep", this.aliyunDownUrl + uploadAly(temp,"ipa"));
                                    //删除ipa
                                    new File("./sign/mode/temp/" + nameIpa).delete();
                                }else {
                                    log.info("不使用七牛云");
                                    if(SettingUtil.ipaDownUrl != null && !SettingUtil.ipaDownUrl.equals("")){
                                        plist = plist.replace("urlRep", SettingUtil.ipaDownUrl  + nameIpa);
                                    }else {
                                        plist = plist.replace("urlRep", packStatus.getUrl()  + nameIpa);
                                    }

                                    log.info("ipa路径:" + packStatus.getUrl()  + nameIpa);
                                }
                                //bundle要随机不然有时候没进度条
                                plist = plist.replace("bundleRep", packStatus.getPageName());
                                plist = plist.replace("versionRep", distribute.getVersion());
                                String iconPath = packStatus.getUrl() + distribute.getAccount() + "/mdmdistribute/" + packStatus.getAppId() + "/" + packStatus.getAppId() + ".png";
                                plist = plist.replace("iconRep", iconPath);
                                plist = plist.replace("appnameRep", distribute.getAppName());
                                String plistName = new Date().getTime() + ".plist";
                                IoHandler.writeTxt(new File("./sign/mode/temp").getAbsolutePath() + "/" + plistName, plist);
                                //如果没有指定下载地址就是默认的
                                String plistUrl;

                                plistUrl = "itms-services://?action=download-manifest&url=" +  packStatus.getUrl() + plistName;
                                packStatusDao.update(new MdmPackStatusEntity(null, distribute.getAccount(), distribute.getPageName(), null, null, appleIis1.getIis(),appleIis1.getP12(),filePro,null, nameIpa,plistUrl , "点击下载", null,null,null,null,null,null), packStatus.getUuid());
                                //删除配置文件
                                log.info("删除配置文件");
                                appleApiUtil.deleProfiles(map.get("id"));
                                log.info("打包完成");
                                log.info("plist名" + plistName);
                                isSuccess = 0;
                                return plistName;
                            }else {
                                log.info("创建配置文件失败");
                                appleIisDao.updateStatus(0, appleApiUtil.getIis());
                            }
                        }else {
                            log.info("添加指定设备失败,证书失效");
                            appleIisDao.updateStatus(0, appleApiUtil.getIis());
                        }
                    }
                    if(isSuccess == 1){
                        packStatusDao.update(new MdmPackStatusEntity(null, distribute.getAccount(), distribute.getPageName(), null, null,null,null, null, null, null,null , "没有可用的证书", null,null,null,null,null,null), packStatus.getUuid());
                    }
                }else {
                    packStatusDao.update(new MdmPackStatusEntity(null, distribute.getAccount(), distribute.getPageName(), null, null,null, null,null, null, null,null , "没有可用的证书", null,null,null,null,null,null), packStatus.getUuid());
                    throw  new RuntimeException("没有可用的证书");
                }
            }else {
                log.info("老设备签名");
                //查询这个应用对应的账号
                MdmDistributeEntity distribute = distributeDao.query(packStatus.getAppId());
                User user = userDao.queryAccount(distribute.getAccount());
                //包名
                String nameIpa = new Date().getTime() + ".ipa";
                //临时目录
                String temp = new File("./sign/mode/temp").getAbsolutePath() + "/" + nameIpa;
                String cmd = "./sign/mode/zsign -k " + packStatus.getP12Path() + " -p 123456 -m " + packStatus.getMobilePath() + " -o " + temp + " -z 1 " + distribute.getIpa();
                log.info("开始签名" + cmd);
                packStatusDao.updateStatus("正在签名", packStatus.getUuid());
                Map<String,Object>  map1 =  RuntimeExec.runtimeExec(cmd);
                log.info("签名结果" + map1.get("status").toString());
                log.info("签名反馈" + map1.get("info").toString());
                log.info("签名命令" + cmd);
                log.info("包名"+ nameIpa);
                if(!map1.get("status").toString().equals("0")){
                    packStatusDao.update(new MdmPackStatusEntity(null, distribute.getAccount(), distribute.getPageName(), null, null, packStatus.getIis(),packStatus.getP12Path(),packStatus.getMobilePath(),null, null,null , "签名失败", null,null,null,null,null,null), packStatus.getUuid());
                    throw  new RuntimeException("签名失败");
                }
                //获取plist
                String plist = IoHandler.readTxt(new File("./sign/mode/install.plist").getAbsolutePath());
                packStatusDao.updateStatus("准备下载",  packStatus.getUuid());
                //是否使用七牛云
                if(!this.qiniuyunAccessKey.equals("")){
                    log.info("使用七牛云");
                    plist = plist.replace("urlRep", this.qiniuyunUrl + uploadQly(temp,"ipa"));
                    //删除ipa
                    new File("./sign/mode/temp/" + nameIpa).delete();
                }else if(!this.aliyunAccessKey.equals("")){
                    log.info("使用阿里云");
                    plist = plist.replace("urlRep", this.aliyunDownUrl + uploadAly(temp,"ipa"));
                    //删除ipa
                    new File("./sign/mode/temp/" + nameIpa).delete();
                }else {
                    log.info("不使用七牛云");
                    if(SettingUtil.ipaDownUrl != null && !SettingUtil.ipaDownUrl.equals("")){
                        plist = plist.replace("urlRep", SettingUtil.ipaDownUrl  + nameIpa);
                    }else {
                        plist = plist.replace("urlRep", packStatus.getUrl()  + nameIpa);
                    }
                    log.info("ipa路径:" + packStatus.getUrl()  + nameIpa);
                }
                //bundle要随机不然有时候没进度条
                plist = plist.replace("bundleRep", packStatus.getPageName());
                plist = plist.replace("versionRep", distribute.getVersion());
                String iconPath = packStatus.getUrl() + distribute.getAccount() + "/mdmdistribute/" + packStatus.getAppId() + "/" + packStatus.getAppId() + ".png";
                plist = plist.replace("iconRep", iconPath);
                plist = plist.replace("appnameRep", distribute.getAppName());
                String plistName = new Date().getTime() + ".plist";
                IoHandler.writeTxt(new File("./sign/mode/temp").getAbsolutePath() + "/" + plistName, plist);
                String plistUrl;
                //如果没有指定下载地址就是默认的
                plistUrl = "itms-services://?action=download-manifest&url=" +  packStatus.getUrl() + plistName;
                packStatusDao.update(new MdmPackStatusEntity(null, distribute.getAccount(), distribute.getPageName(), null, null,  packStatus.getIis(),packStatus.getP12Path(),packStatus.getMobilePath(),null, nameIpa,plistUrl , "点击下载", null,null,null,null,null,null), packStatus.getUuid());
                //删除配置文件
                log.info("打包完成");
                log.info("plist名" + plistName);
                return plistName;
            }

        }catch (Exception e){
            log.info(e.toString());
            throw  new RuntimeException("失败" + e.getMessage());
        }
        return null;
    }

    public void addDownCode(User user,Integer num){
        Integer count = (userDao.queryAccount(user.getAccount()).getCount() + appleIisDao.queryIisCount(user.getAccount()) * 100) - downCodeDao.queryAccountCount(user.getAccount());
        List<MdmDownCodeEntity> downCodeList = new ArrayList<>();
        if(count >= num){
            for (int i = 0; i < num; i++) {
                MdmDownCodeEntity downCode = new MdmDownCodeEntity(MyUtil.getUuid(), user.getAccount(), ServerUtil.getUuid(), new Date(), null, 1);
                downCodeList.add(downCode);
            }
            downCodeDao.addDownCode(downCodeList);
        }else {
            throw  new RuntimeException("您最多还可以生成" + count + "个下载码");
        }
    }

    public int dele(User user,String id) {
        try {
            //如果是管理员就找到应用账号再删
            if(user.getType() == 1){
                MdmDistributeEntity distribute = distributeDao.query(id);
                if(distribute != null){
                    distributeDao.dele(distribute.getAccount(), id);
                    File file = new File("./sign/temp/" + distribute.getAccount() + "/distribute/" + id).getAbsoluteFile();
                    FileSystemUtils.deleteRecursively(file);
                }else {
                    throw  new RuntimeException("应用不存在");
                }
            }else {
                if(distributeDao.dele(user.getAccount(), id) == 1){
                    File file = new File("./sign/temp/" + user.getAccount() + "/distribute/" + id).getAbsoluteFile();
                    System.out.println(file.getAbsolutePath());
                    FileSystemUtils.deleteRecursively(file);
                }else {
                    throw  new RuntimeException("应用不存在");
                }
            }
        }catch (Exception e){
            throw  new RuntimeException("删除失败," + e.getMessage());
        }
        return 0;
    }

    @Autowired
    private HttpServletRequest request;

    public List<MdmDistributeEntity> queryAccountAll(String account) {
        try {
            List<MdmDistributeEntity> distributeList = distributeDao.queryAccountAll(account);

            Iterator<MdmDistributeEntity> iterator = distributeList.iterator();
            while (iterator.hasNext()){
                MdmDistributeEntity next = iterator.next();
                next.setIcon(ServerUtil.getRootUrl(request) + next.getAccount() + "/distribute/" + next.getId() + "/" + next.getId() + ".png");
                Integer sum = mdmPackStatusMapper.selectByUuidCount(next.getId(), null);
                Integer day = mdmPackStatusMapper.selectByUuidCount(next.getId(), "day");
                Integer lastDay = mdmPackStatusMapper.selectByUuidCount(next.getId(), "lastDay");
                next.setDayCount(day);
                next.setSumCount(sum);
                next.setLastDayCount(lastDay);
            }

            return distributeList;
        }catch (Exception e){
            throw  new RuntimeException("查询失败" + e.getMessage());
        }
    }


    public List<MdmDistributeEntity> queryAll() {
        try {

            List<MdmDistributeEntity> distributeList = distributeDao.querAll();

            Iterator<MdmDistributeEntity> iterator = distributeList.iterator();
            while (iterator.hasNext()){
                MdmDistributeEntity next = iterator.next();
                next.setIcon(ServerUtil.getRootUrl(request) + next.getAccount() + "/mdmdistribute/" + next.getId() + "/" + next.getId() + ".png");
                Integer sum = mdmPackStatusMapper.selectByUuidCount(next.getId(), null);
                Integer day = mdmPackStatusMapper.selectByUuidCount(next.getId(), "day");
                Integer lastDay = mdmPackStatusMapper.selectByUuidCount(next.getId(), "lastDay");
                next.setDayCount(day);
                next.setSumCount(sum);
                next.setLastDayCount(lastDay);
            }


            return distributeList;
        }catch (Exception e){
            throw  new RuntimeException("查询失败" + e.getMessage());
        }
    }

    /**
     * 上传七牛云
     * @return
     */
    public String uploadQly(String localFilePath,String suffix){
        Long time = System.currentTimeMillis();
        Configuration cfg;
        //内网
        if(this.qiniuyunReg.equals("huadong")){
             cfg = new Configuration(Region.qvmRegion0());
        }else if(this.qiniuyunReg.equals("huabei")){
             cfg = new Configuration(Region.qvmRegion1());
        }else {
            cfg = new Configuration(Region.qvmRegion1());
        }

        cfg.useHttpsDomains = false;

        UploadManager uploadManager = new UploadManager(cfg);
        String key = new Date().getTime() + "." + suffix;
        Auth auth = Auth.create(qiniuyunAccessKey, qiniuyunSecretKey);
        String upToken = auth.uploadToken(qiniuyunBucket);
        try {
            Response response = uploadManager.put(localFilePath, key, upToken);
            //解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            log.info("本次上传耗费:" + (System.currentTimeMillis() - time)/1000 + "秒");
            return putRet.key;
        } catch (Exception ex) {
            log.info("上传失败" + ex.toString());
            return null;
        }
    }

    /**
     * 阿里云上传
     * @param localFilePath
     * @return
     */
    public String uploadAly(String localFilePath,String  suffix){
        Long time = System.currentTimeMillis();
        try {
            String name = System.currentTimeMillis() + "." + suffix;
            String endpoint = aliyunUrl;
            String accessKeyId = aliyunAccessKey;
            String accessKeySecret = aliyunSecretKey;
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            PutObjectRequest putObjectRequest = new PutObjectRequest(aliyunBucket,name, new File(localFilePath));
            ossClient.putObject(putObjectRequest);
            ossClient.shutdown();
            log.info("阿里云上传时间:" + (System.currentTimeMillis() - time)/1000 + "秒");
            return name;
        }catch (Exception e){
            log.info("阿里云上传失败:" + e.toString());
            return null;
        }
    }

}
