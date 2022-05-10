## ios_super_sign

###### 超级签名 免签封装 安卓打包 企业签名 超级签名MDM 企业签名MDM 自助分发多合一系统

最近准备重构本套代码 开发成一套完整的商业分发系统 缺少一名前端 有意者请联系 微 x2524931333

最新部署教程 https://github.com/xujimu/ios_super_sign_docker


### 主要功能

1. 基本操作

   - 修改密码
   - 用户信息最近准备重构本套代码 开发成一套完整的商业分发系统 缺少一名前端 有意者请联系 微 x2524931333
   - 共有池
   - 退出

2. 超级签名

   - 应用列表

     - 应用信息

     - 安卓合并
     - 简介编辑
     - 轮播图编辑
     - 分发地址复制
     - 下载码启动
     - 下载码购买地址
     - 删除

   - 上传应用

     - 拖动ipa上传

   - 下载密码

     - 下载码生成
     - 下载码信息显示
     - 删除

3. 证书管理

   - 证书列表
     - 证书信息
     - 剩余设备数
     - 删除
   - 上传证书
     - p8模式证书上传

4. 用户管理

   - 用户列表
     - 用户信息
     - 添加共有池

5. 下载管理

   - 下载记录
   - 对应设备签名的证书下载

6. 免签封装

   - 打包支持自定义信息
     - 应用名称
     - 打包网址
     - 描述文件名称
     - 描述文件机构
     - 描述文件描述信息
     - 描述文件统一信息
     - 应用图标
     - 启动图(仅支持安卓)
     - 苹果图标是否可删除
     - 安卓动态网址
     - 安卓包名
     - 版本
     - 自定义描述文件绿标证书
   - 打包记录
     - 分发地址
     - 分发源码下载地址

7. 企业签名

   - 开始签名
     - 开始签名
     - 编辑签名证书所需共有池
     - 删除
     - 证书备注修改
   - 签名记录
     - 签名记录信息
     - 签名后的ipa包下载
   - 上传证书

8. 自助分发

   - 开始分发
   - 分发记录
     - 分发记录显示
     - 安卓合并更新
     - 苹果更新
     - 简介编辑
     - 分发地址复制
     - 删除

9.超级签mdm

10.企业签名mdm

本次更新内容:
增加多语言 修复项目运行一段时间java异常结束问题 新增超级签名MDM 企业签名MDM 增加下载次数统计
修复部分IPA无法读取问题 增加系统扣量设置

注意: docker本意可以理解为虚拟机 方便环境移植 本系统启动后会衍生出一个子系统 此系统的ssh服务器是开启的 端口为 1818 密码为Mysql666.. 此子系统安装了一些本系统需要的环境 包括mysql redis他们的密码都是Mysql666.. 不过mysql和redis的密码并不需要修改 并没有对外暴露端口 请务必在系统启动后 修改 子系统密码 否则会有被入侵的风险 

2022/5/10同步支持最新ios系统

### 架构

---

- 后端 spring boot
- 前端 vue
- 数据库 mysql
- redis
- 其他 redis python openssl androidsdk python unzip java8 java11 zsign

### 部署

参考docker项目

**安装**

最新安装视频教程：https://www.bilibili.com/video/BV1UB4y117N2#reply111920410592

### 鸣谢
https://github.com/zhlynn/zsign

### 展示

演示站 https://test.wlznsb.cn

账号密码都是super
