package com.wlznsb.iossupersign.mapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Param;
import java.util.List;

import com.wlznsb.iossupersign.entity.CertInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author Administrator
* @description 针对表【cert_info(mdm证书表)】的数据库操作Mapper
* @createDate 2022-04-17 21:19:38
* @Entity com.wlznsb.iossupersign.entity.CertInfoEntity
*/
@DS("mdm")
public interface CertInfoMapper extends BaseMapper<CertInfoEntity> {
    List<CertInfoEntity> selectAll();

    CertInfoEntity selectOneByCertStatus(@Param("certStatus") Integer certStatus);
}




