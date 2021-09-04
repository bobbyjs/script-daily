#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#
# Create by tuke on 2020/11/12
#
import os
import os.path as path
import sys
from datetime import datetime

ctrl_str = """
package {package_name}.controller;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @description TODO
 * @author tuke
 * @modified tuke
 * @date {date_time_str}
 * @version v1.0
 */
@RestController
@RequestMapping(path = "")
public class {base_name}Controller {left_bracket}
    
    @Autowired
    private {base_name}Service service;
{method_body}
{right_bracket}
"""

ctrl_query_method_str = """
    /**
     * 
     */
    @{http_method_name}Mapping(value = "/")
    public BaseResponse<{base_name}{method_name_cap}DTO, ?> {method_name}(@Validated @RequestBody {base_name}{method_name_cap}Param param) {left_bracket}
        String tenantId = LoginInfoUtils.getTenantId();
        return new BaseResponse<>({base_name}Convert.INSTANCE.vo2dto(
            service.{method_name}({base_name}Convert.INSTANCE.param2bo(param), tenantId)));
    {right_bracket}
"""

ctrl_get_method_str = """
    /**
     * 
     */
    @GetMapping(value = "/")
    public BaseResponse<{base_name}{method_name_cap}DTO, ?> {method_name}(@RequestParam("{query}") {query_type} {query}) {left_bracket}
        String tenantId = LoginInfoUtils.getTenantId();
        return new BaseResponse<>({base_name}Convert.INSTANCE.vo2dto(
            service.{method_name}({query}, tenantId)));
    {right_bracket}
"""

ctrl_modify_method_str = """
    /**
     * 
     */
    @{http_method_name}Mapping(value = "/")
    public BaseResponse<?, ?> {method_name}(@Validated @RequestBody {base_name}{method_name_cap}Param param) {left_bracket}
        String tenantId = LoginInfoUtils.getTenantId();
        service.{method_name}({base_name}Convert.INSTANCE.param2bo(param), tenantId);
        return BaseResponse.success();
    {right_bracket}
"""

pojo_param_dto_str = """
package {package_name}.controller.{type_lower}.{base_name_uncap};

import lombok.Data;

/**
 * @description TODO
 * @author tuke
 * @modified tuke
 * @date {date_time_str}
 * @version v1.0
 */
@Data
public class {base_name}{method_name_cap}{type} {left_bracket}

{right_bracket}
"""
pojo_bo_vo_str = """
package {package_name}.model.{type_lower}.{base_name_uncap};

import lombok.Data;

/**
 * @description TODO
 * @author tuke
 * @modified tuke
 * @date {date_time_str}
 * @version v1.0
 */
@Data
public class {base_name}{method_name_cap}{type} {left_bracket}

{right_bracket}
"""
svc_str = """
package {package_name}.service;

/**
 * @description TODO
 * @author tuke
 * @modified tuke
 * @date {date_time_str}
 * @version v1.0
 */
public interface {base_name}Service {left_bracket}
{method_body}
{right_bracket}
"""

svc_query_method_str = """
    /**
     * 
     */
    {base_name}{method_name_cap}VO {method_name}({base_name}{method_name_cap}BO bo, String tenantId);
"""
svc_get_method_str = """
    /**
     * 
     */
    {base_name}{method_name_cap}VO {method_name}({query_type} {query}, String tenantId);
"""
svc_modify_method_str = """
    /**
     * 
     */
    void {method_name}({base_name}{method_name_cap}BO bo, String tenantId);
"""

svc_impl_str = """
package {package_name}.service.impl;

import org.springframework.stereotype.Service;

/**
 * @description TODO
 * @author tuke
 * @modified tuke
 * @date {date_time_str}
 * @version v1.0
 */
@Service
public class {base_name}ServiceImpl implements {base_name}Service {left_bracket}
{method_body}
{right_bracket}
"""
svc_impl_query_method_str = """
    @Override
    public {base_name}{method_name_cap}VO {method_name}({base_name}{method_name_cap}BO bo, String tenantId) {left_bracket}
        // todo impl
        return null;
    {right_bracket}
"""
svc_impl_get_method_str = """
    @Override
    public {base_name}{method_name_cap}VO {method_name}({query_type} {query}, String tenantId) {left_bracket}
        // todo impl
        return null;
    {right_bracket}
"""
svc_impl_modify_method_str = """
    @Override
    public void {method_name}({base_name}{method_name_cap}BO bo, String tenantId) {left_bracket}
        // todo impl
    {right_bracket}
"""

convert_str = """
package {package_name}.model.convert;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @description TODO
 * @author tuke
 * @modified tuke
 * @date {date_time_str}
 * @version v1.0
 */
@Mapper
public interface {base_name}Convert {left_bracket}

    {base_name}Convert INSTANCE = Mappers.getMapper({base_name}Convert.class);
{method_body}    
{right_bracket}
"""

convert_method_str = """
    {base_name}{method_name_cap}{target_type} {source_type_lower}2{target_type_lower}({base_name}{method_name_cap}{source_type} {source_type_lower});
"""


def get_date_time_str():
    return datetime.today().strftime('%Y/%m/%d %H:%M')


left_bracket = '{'
right_bracket = '}'
date_time_str = get_date_time_str()


def mkdirs(fn):
    if not fn or fn == '/' or os.path.exists(fn):
        return
    pardir, _ = os.path.split(fn)
    if not os.path.exists(pardir):
        mkdirs(pardir)
    os.mkdir(fn)


def write_to(fn, content):
    with open(fn, 'w+') as f:
        f.write(content)


def generate(base_path, package_name, base_name, method_names):
    base_name_uncap = base_name[0].lower() + base_name[1:]

    mkdirs(path.join(base_path, f'controller/dto/{base_name_uncap}'))
    mkdirs(path.join(base_path, f'controller/param/{base_name_uncap}'))
    mkdirs(path.join(base_path, f'service/impl'))
    mkdirs(path.join(base_path, f'model/bo/{base_name_uncap}'))
    mkdirs(path.join(base_path, f'model/vo/{base_name_uncap}'))
    mkdirs(path.join(base_path, f'model/convert'))

    #
    ctrl_fn = path.join(base_path, f'controller/{base_name}Controller.java')
    ctrl_method_body = []
    svc_method_body = []
    svc_impl_method_body = []

    for method_name in method_names:
        keys = method_name.split(':')
        flag, http_method_name, method_name = keys[:3]
        query, query_type = None, None
        if len(keys) == 4:
            query_type, query = keys[3].split('_')

        method_name_cap = method_name[0].upper() + method_name[1:]

        if query:
            ctrl_method_str = ctrl_get_method_str
            svc_method_str = svc_get_method_str
            svc_impl_method_str = svc_impl_get_method_str
        elif flag == 'Q':
            ctrl_method_str = ctrl_query_method_str
            svc_method_str = svc_query_method_str
            svc_impl_method_str = svc_impl_query_method_str
        else:
            ctrl_method_str = ctrl_modify_method_str
            svc_method_str = svc_modify_method_str
            svc_impl_method_str = svc_impl_modify_method_str

        ctrl_method_content = ctrl_method_str.format(
                left_bracket=left_bracket, right_bracket=right_bracket,
                http_method_name=http_method_name,
                base_name=base_name,
                query=query, query_type=query_type,
                method_name=method_name,
                method_name_cap=method_name_cap)
        ctrl_method_body.append(ctrl_method_content.rstrip())

        svc_method_content = svc_method_str.format(
                base_name=base_name,
                method_name=method_name,
                query=query, query_type=query_type,
                method_name_cap=method_name_cap)
        svc_method_body.append(svc_method_content.rstrip())

        svc_impl_method_content = svc_impl_method_str.format(
                left_bracket=left_bracket, right_bracket=right_bracket,
                base_name=base_name,
                method_name=method_name,
                query=query, query_type=query_type,
                method_name_cap=method_name_cap)
        svc_impl_method_body.append(svc_impl_method_content.rstrip())

    ctrl_content = ctrl_str.format(
            left_bracket=left_bracket, right_bracket=right_bracket,
            date_time_str=date_time_str,
            package_name=package_name, base_name=base_name,
            method_body='\n'.join(ctrl_method_body))
    write_to(ctrl_fn, ctrl_content.strip())

    #
    svc_fn = path.join(base_path, f'service/{base_name}Service.java')
    svc_impl_fn = path.join(base_path,
                            f'service/impl/{base_name}ServiceImpl.java')

    svc_content = svc_str.format(
            left_bracket=left_bracket, right_bracket=right_bracket,
            date_time_str=date_time_str,
            package_name=package_name, base_name=base_name,
            method_body='\n'.join(svc_method_body))
    svc_impl_content = svc_impl_str.format(
            left_bracket=left_bracket, right_bracket=right_bracket,
            date_time_str=date_time_str,
            package_name=package_name, base_name=base_name,
            method_body='\n'.join(svc_impl_method_body))
    write_to(svc_fn, svc_content.strip())
    write_to(svc_impl_fn, svc_impl_content.strip())

    # pojo
    convert_fn = path.join(base_path, f'model/convert/{base_name}Convert.java')
    convert_method_body = []
    for method_name in method_names:
        keys = method_name.split(':')
        flag, http_method_name, method_name = keys[:3]
        query, query_type = None, None
        if len(keys) == 4:
            query_type, query = keys[3].split('_')

        method_name_cap = method_name[0].upper() + method_name[1:]
        if not query:
            convert_method_body.append(convert_method_str.format(
                    base_name=base_name, method_name_cap=method_name_cap,
                    source_type='Param', source_type_lower='param',
                    target_type='BO', target_type_lower='bo'
            ).rstrip())

            param_fn = path.join(base_path,
                                 f'controller/param/{base_name_uncap}/{base_name}{method_name_cap}Param.java')
            bo_fn = path.join(base_path,
                              f'model/bo/{base_name_uncap}/{base_name}{method_name_cap}BO.java')

            param_content = pojo_param_dto_str.format(
                    left_bracket=left_bracket, right_bracket=right_bracket,
                    date_time_str=date_time_str,
                    package_name=package_name,
                    base_name=base_name, base_name_uncap=base_name_uncap,
                    method_name_cap=method_name_cap,
                    type='Param', type_lower='param')
            bo_content = pojo_bo_vo_str.format(
                    left_bracket=left_bracket, right_bracket=right_bracket,
                    date_time_str=date_time_str,
                    package_name=package_name,
                    base_name=base_name, base_name_uncap=base_name_uncap,
                    method_name_cap=method_name_cap,
                    type='BO', type_lower='bo')

            write_to(param_fn, param_content.strip())
            write_to(bo_fn, bo_content.strip())

        if flag == 'Q':
            convert_method_body.append(convert_method_str.format(
                    base_name=base_name, method_name_cap=method_name_cap,
                    source_type='VO', source_type_lower='vo',
                    target_type='DTO', target_type_lower='dto'
            ).rstrip())

            vo_fn = path.join(base_path,
                              f'model/vo/{base_name_uncap}/{base_name}{method_name_cap}VO.java')
            dto_fn = path.join(base_path,
                               f'controller/dto/{base_name_uncap}/{base_name}{method_name_cap}DTO.java')

            vo_content = pojo_bo_vo_str.format(
                    left_bracket=left_bracket, right_bracket=right_bracket,
                    date_time_str=date_time_str,
                    package_name=package_name,
                    base_name=base_name, base_name_uncap=base_name_uncap,
                    method_name_cap=method_name_cap,
                    type='VO', type_lower='vo')
            dto_content = pojo_param_dto_str.format(
                    left_bracket=left_bracket, right_bracket=right_bracket,
                    date_time_str=date_time_str,
                    package_name=package_name,
                    base_name=base_name, base_name_uncap=base_name_uncap,
                    method_name_cap=method_name_cap,
                    type='DTO', type_lower='dto')
            write_to(dto_fn, dto_content.strip())
            write_to(vo_fn, vo_content.strip())

    convert_content = convert_str.format(
            left_bracket=left_bracket, right_bracket=right_bracket,
            date_time_str=date_time_str,
            package_name=package_name, base_name=base_name,
            method_body='\n'.join(convert_method_body))
    write_to(convert_fn, convert_content.strip())


if __name__ == '__main__':
    """
    ./generate-spring-ctrl-svc-param-bo-vo-dto.py /Users/tuke/newcore/metadata-server/src/main/java/com/xinheyun/metadata \
    com.xinheyun.metadata Template \
    M:Put:modifyStatus \
    Q:Get:getBasic:Long_id \
    M:Put:modifyBasic \
    Q:Get:getTransfer:Long_id \
    M:Post:saveTransfer \
    M:Delete:deleteTransfer \
    Q:Get:getObjectTransfer \
    Q:Get:getDataFilter:Long_id \
    M:Post:saveDataFilter    
    """
    args = sys.argv
    if len(args) <= 4:
        print("Usage: generate-spring-ctrl-svc-param-bo-vo-dto.py <path> "
              "<package_name> <base_name> <method_names>")
        sys.exit(1)
    generate(base_path=args[1], package_name=args[2], base_name=args[3],
             method_names=args[4:])
