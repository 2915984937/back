package com.tencent.wxcloudrun.user.dao;

import com.tencent.wxcloudrun.user.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

  User selectByOpenid(@Param("openid") String openid);

  User selectById(@Param("id") Long id);

  int insert(User user);

  int updateProfile(User user);

  /** 懒加载解禁：仅处理临时封禁到期 */
  int unbanIfExpired(@Param("id") Long id);

  /** 匿名化注销：清空敏感信息，openid 置 NULL 释放唯一索引，可重新注册 */
  int anonymize(@Param("id") Long id);

  /** 刷新最近登录时间 */
  int updateLastLogin(@Param("id") Long id);
}