package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.api.WxApiClient;
import com.tencent.wxcloudrun.dao.UserMapper;
import com.tencent.wxcloudrun.dto.LoginResult;
import com.tencent.wxcloudrun.dto.WxLoginRequest;
import com.tencent.wxcloudrun.exception.BizException;
import com.tencent.wxcloudrun.model.User;
import com.tencent.wxcloudrun.service.impl.UserServiceImpl;
import com.tencent.wxcloudrun.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock UserMapper userMapper;
  @Mock WxApiClient wxApiClient;
  @Mock JwtUtil jwtUtil;
  @InjectMocks UserServiceImpl service;

  @Test
  void 首次登录_自动注册并返回token() {
    WxLoginRequest req = new WxLoginRequest();
    req.setCode("code-1");

    when(wxApiClient.code2Session("code-1")).thenReturn("openid-1");
    when(userMapper.selectByOpenid("openid-1")).thenReturn(null);
    when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
      User u = inv.getArgument(0);
      u.setId(99L); // useGeneratedKeys 回填
      return 1;
    });
    when(jwtUtil.generate(99L, "openid-1")).thenReturn("jwt-token-1");

    LoginResult r = service.login(req, null);

    assertThat(r.getToken()).isEqualTo("jwt-token-1");
    assertThat(r.isNewUser()).isTrue();
    assertThat(r.getUser().getOpenid()).isEqualTo("openid-1");
    verify(userMapper, times(1)).insert(any(User.class));
    verify(userMapper, never()).updateProfile(any());
  }

  @Test
  void 老用户登录_复用不重复注册() {
    WxLoginRequest req = new WxLoginRequest();
    req.setCode("code-2");

    User existing = new User();
    existing.setId(5L);
    existing.setOpenid("openid-2");

    when(wxApiClient.code2Session("code-2")).thenReturn("openid-2");
    when(userMapper.selectByOpenid("openid-2")).thenReturn(existing);
    when(jwtUtil.generate(5L, "openid-2")).thenReturn("jwt-token-2");

    LoginResult r = service.login(req, null);

    assertThat(r.getToken()).isEqualTo("jwt-token-2");
    assertThat(r.isNewUser()).isFalse();
    verify(userMapper, never()).insert(any());
  }

  @Test
  void 云托管请求头_openid优先于code() {
    WxLoginRequest req = new WxLoginRequest();
    req.setCode("ignored"); // 有 header，不应触发微信接口

    User u = new User();
    u.setId(7L);
    u.setOpenid("hdr-openid");

    when(userMapper.selectByOpenid("hdr-openid")).thenReturn(u);
    when(jwtUtil.generate(7L, "hdr-openid")).thenReturn("t");

    service.login(req, "hdr-openid");

    verify(wxApiClient, never()).code2Session(anyString());
  }

  @Test
  void code无效_抛出业务异常() {
    WxLoginRequest req = new WxLoginRequest();
    req.setCode("bad-code");

    when(wxApiClient.code2Session("bad-code"))
        .thenThrow(new BizException(1001, "微信登录失败"));

    assertThatThrownBy(() -> service.login(req, null))
        .isInstanceOf(BizException.class)
        .hasMessage("微信登录失败");
  }

  @Test
  void 带上昵称头像时_更新老用户资料() {
    WxLoginRequest req = new WxLoginRequest();
    req.setCode("code-3");
    req.setNickname("小明");
    req.setAvatar("https://x/1.png");

    User existing = new User();
    existing.setId(8L);
    existing.setOpenid("openid-3");

    when(wxApiClient.code2Session("code-3")).thenReturn("openid-3");
    when(userMapper.selectByOpenid("openid-3")).thenReturn(existing);
    when(userMapper.updateProfile(any(User.class))).thenReturn(1);
    when(jwtUtil.generate(8L, "openid-3")).thenReturn("t3");

    service.login(req, null);

    assertThat(existing.getNickname()).isEqualTo("小明");
    assertThat(existing.getAvatar()).isEqualTo("https://x/1.png");
    verify(userMapper, times(1)).updateProfile(existing);
  }
}