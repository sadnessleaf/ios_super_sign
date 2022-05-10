package com.wlznsb.iossupersign.mapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Param;

import com.wlznsb.iossupersign.entity.DeviceInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author Administrator
* @description 针对表【device_info(注册的设备信息)】的数据库操作Mapper
* @createDate 2022-04-17 21:19:38
* @Entity com.wlznsb.iossupersign.entity.DeviceInfoEntity
*/
@DS("mdm")
public interface DeviceInfoMapper extends BaseMapper<DeviceInfoEntity> {
    DeviceInfoEntity selectOneByDeviceId(@Param("deviceId") String deviceId);

}




