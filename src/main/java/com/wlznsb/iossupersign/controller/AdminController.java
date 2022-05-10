package com.wlznsb.iossupersign.controller;

import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wlznsb.iossupersign.annotation.PxCheckAdmin;
import com.wlznsb.iossupersign.entity.CertInfoEntity;
import com.wlznsb.iossupersign.entity.SystemctlSettingsEntity;
import com.wlznsb.iossupersign.execption.ResRunException;
import com.wlznsb.iossupersign.mapper.*;
import com.wlznsb.iossupersign.entity.Domain;
import com.wlznsb.iossupersign.entity.User;
import com.wlznsb.iossupersign.service.AppleIisServiceImpl;
import com.wlznsb.iossupersign.service.UserServiceImpl;
import com.wlznsb.iossupersign.util.MyUtil;
import com.wlznsb.iossupersign.util.SettingUtil;
import okhttp3.*;
import okhttp3.RequestBody;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/admin")
@Validated
@CrossOrigin(allowCredentials="true")

public class AdminController {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private AppleIisServiceImpl appleIisService;

    @Autowired
    private DomainDao domainDao;

    @Resource
    private CertInfoMapper certInfoMapper;

    @Resource
    private DeviceCommandTaskMapper deviceCommandTaskMapper;

    @Resource
    private DeviceInfoMapper deviceInfoMapper;



    @Autowired
    private SystemctlSettingsMapper settingsMapper;

    /**
     * 系统设置
     * @param request
     * @return
     */
    @RequestMapping(value = "/system_settings",method = RequestMethod.POST)
    public Map<String,Object> system_settings(@org.springframework.web.bind.annotation.RequestBody SystemctlSettingsEntity req, HttpServletRequest request){
        Map<String,Object> map = new HashMap<String, Object>();
        try {
            settingsMapper.update(req,null);
            map.put("code", 0);
            map.put("message", "操作成功");
        }catch (Exception e){
            map.put("code", 1);
            map.put("message", "参数不合法可能是超出范围");
        }

        return map;
    }


    /**
     * 系统设置
     * @param request
     * @return
     */
    @RequestMapping(value = "/system_settings_query",method = RequestMethod.POST)
    public Map<String,Object> system_settings_query(HttpServletRequest request){
        Map<String,Object> map = new HashMap<String, Object>();
        SystemctlSettingsEntity systemctlSettingsEntity = settingsMapper.selectOne(null);
        map.put("code", 0);
        map.put("message", "操作成功");
        map.put("data", systemctlSettingsEntity);

        return map;
    }

    /**
     * 查询所有的iis证书
     * @param request
     * @return
     */
    @RequestMapping(value = "/queryIisAll",method = RequestMethod.GET)
    public Map<String,Object> queryIisAll(HttpServletRequest request,@RequestParam  Integer pageNum,@RequestParam  Integer pageSize){
        Map<String,Object> map = new HashMap<String, Object>();
        PageHelper.startPage(pageNum,pageSize);
        Page<User> page =  (Page) appleIisService.queryAll();
        map.put("code", 0);
        map.put("message", "查询成功");
        map.put("data", page.getResult());
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        return map;
    }




    @Autowired
    private SystemctlSettingsMapper systemctlSettingsMapper;

    /**
     * mdm证书上传
     * @param
     * @return
     */
    @RequestMapping(value = "/uploadMdmCert",method = RequestMethod.POST)
    public Map<String,Object> uploadMdmCert(@RequestParam MultipartFile p12,MultipartFile pem,MultipartFile key, @RequestParam  String password,@RequestParam  String remark) throws IOException {
        Map<String,Object> map = new HashMap<String, Object>();


        boolean isPem = false;
        OkHttpClient client = MyUtil.getOkHttpClient();

        File filePath = new File("./sign/mdm/" + MyUtil.getUuid() + ".p12");

        MyUtil.MultipartFileWrite(p12,filePath.getAbsolutePath());
        File filePemPath = null;
        File fileKeyPath = null;
        if(null != pem && pem.getBytes().length > 0 && null != key && key.getBytes().length > 0){
            filePemPath = new File("./sign/mdm/" + MyUtil.getUuid() + ".pem");
            fileKeyPath = new File("./sign/mdm/" + MyUtil.getUuid() + ".key");
            MyUtil.MultipartFileWrite(pem,filePemPath.getAbsolutePath());
            MyUtil.MultipartFileWrite(key,fileKeyPath.getAbsolutePath());
            isPem = true;
        }

        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body;
        if(isPem){
            body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("p12",filePath.getAbsolutePath(),
                            RequestBody.create(MediaType.parse("application/octet-stream"),
                                    filePath))
                    .addFormDataPart("password",password)
                    .addFormDataPart("remark",remark)
                    .addFormDataPart("pem",filePath.getAbsolutePath(),
                            RequestBody.create(MediaType.parse("application/octet-stream"),
                                    filePemPath))
                    .addFormDataPart("key",filePath.getAbsolutePath(),
                            RequestBody.create(MediaType.parse("application/octet-stream"),
                                    fileKeyPath))
                    .build();
        }else {
            body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("p12",filePath.getAbsolutePath(),
                            RequestBody.create(MediaType.parse("application/octet-stream"),
                                    filePath))
                    .addFormDataPart("password",password)
                    .addFormDataPart("remark",remark)
                    .build();
        }

        SystemctlSettingsEntity systemctlSettingsEntity = systemctlSettingsMapper.selectOne(null);

        if(systemctlSettingsEntity.getMdmDomain().equals("www.xxx.com")){
            map.put("code", 1);
            map.put("message", "请先前往系统设置-常规设置-填写mdm域名");
            return map;
        }

        Request request = new Request.Builder()
                .url("https://" + systemctlSettingsEntity.getMdmDomain() + "/cert/upload_cert")
                .method("POST", body)
                .build();
        Response response = client.newCall(request).execute();


        //序列化返回
        JsonNode jsonNode = new ObjectMapper().readTree(response.body().string());

        if(jsonNode.get("code").asText().equals("200")){
            map.put("code", 0);
            map.put("message", "上传成功");
        }else {
            throw new RuntimeException("操作失败:" + jsonNode.get("msg").asText());
        }

        return map;
    }

    /**
     * 删除mdm证书
     * @param
     * @return
     */
    @RequestMapping(value = "/deleteMdmCert",method = RequestMethod.POST)
    public Map<String,Object> deleteMdmCert(@RequestParam  String certId) throws IOException {
        Map<String,Object> map = new HashMap<String, Object>();

        OkHttpClient client = MyUtil.getOkHttpClient();

        SystemctlSettingsEntity systemctlSettingsEntity = systemctlSettingsMapper.selectOne(null);

        if(systemctlSettingsEntity.getMdmDomain().equals("www.xxx.com")){
            map.put("code", 1);
            map.put("message", "请先前往系统设置-常规设置-填写mdm域名");
            return map;
        }

        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody requestBody = RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
                .url("https://" + systemctlSettingsEntity.getMdmDomain() + "/cert/deleteCert?certId=" + certId)
                .method("POST", requestBody)
                .build();
        Response response = client.newCall(request).execute();


        //序列化返回
        JsonNode jsonNode = new ObjectMapper().readTree(response.body().string());


        if(jsonNode.get("code").asText().equals("200")){
            map.put("code", 0);
            map.put("message", "删除成功");
        }else {
            throw new RuntimeException("操作失败:" + jsonNode.get("msg").asText());
        }

        return map;
    }

    /**
     * 查询mdm证书
     * @param request
     * @return
     */
    @RequestMapping(value = "/queryMdmCert",method = RequestMethod.GET)
    public Map<String,Object> queryMdmCert(HttpServletRequest request,@RequestParam  Integer pageNum,@RequestParam  Integer pageSize){
        Map<String,Object> map = new HashMap<String, Object>();
        PageHelper.startPage(pageNum,pageSize);
        Page<CertInfoEntity> page = (Page) certInfoMapper.selectAll();
        map.put("code", 0);
        map.put("message", "查询成功");
        map.put("data", page);
        map.put("data", page.getResult());
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        return map;
    }


    //修改类型
    @RequestMapping(value = "/updateType",method = RequestMethod.POST)
    public Map<String,Object> updateType(@RequestParam @NotEmpty String account, @RequestParam @NotEmpty @Range(max = 1,min = 0) int type, HttpServletRequest request){
        Map<String,Object> map = new HashMap<String, Object>();
        userService.updateType(account, type);
        map.put("code", 0);
        map.put("message", "修改成功");
        return map;
    }

    //修改设置
    @RequestMapping(value = "/updateSetting",method = RequestMethod.POST)
    public Map<String,Object> updateIpaDownUrl(@RequestParam  String  settingJson, HttpServletRequest request) throws JsonProcessingException {
        Map<String,Object> map = new HashMap<String, Object>();
        JsonNode jsonNode = new ObjectMapper().readTree(settingJson);
        SettingUtil.ipaDownUrl = jsonNode.get("IpaDownUrl").asText();
        System.out.println(SettingUtil.ipaDownUrl);
        map.put("code", 0);
        map.put("message", "修改成功");
        return map;
    }


    //查询设置
    @RequestMapping(value = "/querySetting",method = RequestMethod.GET)
    public Map<String,Object> querySetting(HttpServletRequest request){
        Map<String,Object> map = new HashMap<String, Object>();
        Map<String,Object> setting = new HashMap<String, Object>();
        setting.put("IpaDownUrl",SettingUtil.ipaDownUrl);
        map.put("code", 0);
        map.put("message", "查询成功");
        map.put("data", setting);
        return map;
    }


    //添加域名
    @RequestMapping(value = "/addDomain",method = RequestMethod.POST)
    public Map<String,Object> addDomain(@RequestParam @NotEmpty String domain, HttpServletRequest request){
        Map<String,Object> map = new HashMap<String, Object>();
        domainDao.add(new Domain(null,domain,new Date(),1));
        map.put("code", 0);
        map.put("message", "添加成功");
        return map;
    }

    //删除域名
    @RequestMapping(value = "/deleteDomain",method = RequestMethod.POST)
    public Map<String,Object> deleteDomain(@RequestParam  Integer id, HttpServletRequest request){
        Map<String,Object> map = new HashMap<String, Object>();
        domainDao.dele(id);
        map.put("code", 0);
        map.put("message", "删除成功");
        return map;
    }


    //删除用户
    @RequestMapping(value = "/dele",method = RequestMethod.POST)
    public Map<String,Object> dele(@RequestParam @NotEmpty String account){
        Map<String,Object> map = new HashMap<String, Object>();
        userService.dele(account);
        map.put("code", 0);
        map.put("message", "删除成功");
        return map;
    }




    /**
     * 添加共有池
     * @param request
     * @param account
     * @param count 添加数量
     * @return
     */
    @RequestMapping(value = "/addUserCount",method = RequestMethod.POST)
    public Map<String,Object> addUserCount(HttpServletRequest request,@RequestParam  String account,@RequestParam  Integer count){
        Map<String,Object> map = new HashMap<String, Object>();
        userDao.addCount(account, count);
        map.put("code", 0);
        map.put("message", "操作成功");
        return map;
    }



    /**
     * 查询所有用户没用业务
     * @param request
     * @return
     */
    @RequestMapping(value = "/queryAll",method = RequestMethod.GET)
    public Map<String,Object> queryAll(HttpServletRequest request,@RequestParam  Integer pageNum,@RequestParam  Integer pageSize){
        Map<String,Object> map = new HashMap<String, Object>();
        PageHelper.startPage(pageNum,pageSize);
        Page<User> page =  (Page) userDao.queryAll();
        map.put("code", 0);
        map.put("message", "查询成功");
        map.put("data", page.getResult());
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        return map;
    }

}
