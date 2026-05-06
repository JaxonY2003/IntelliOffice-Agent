package com.jaxon.back_end.identity.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jaxon.back_end.identity.entity.Employee;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee>{
    Optional<Employee> findByUsername(@Param("username") String username);
}
