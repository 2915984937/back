package com.tencent.wxcloudrun.user;

import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.common.util.JwtUtil;
import com.tencent.wxcloudrun.user.dao.UserMapper;
import com.tencent.wxcloudrun.user.dto.LoginResult;
import com.tencent.wxcloudrun.user.dto.WxLoginRequest;
import com.tencent.wxcloudrun.user.model.User;
import com.tencent.wxcloudrun.user.service.UserService;
import com.tencent.wxcloudrun.wx.WxApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

  @Mock UserMapper userMapper;
  @Mock WxApiClient wxApiClient;
  @Mock JwtUtil jwtUtil;

  @InjectMocks UserService service;

  private static final String OPENID = "openid_abc123xyz";
  private static final String TOKEN = "jwt.token.here";

  @BeforeEach
  void setUp() {
    when(jwtUtil.generate(anyLong(), anyString())).thenReturn(TOKEN);
  }

  // ========== login ==========

  @Nested
  class Login {

    @Test
    void 新用户_自动创建并签发token() {
      WxLoginRequest req = new WxLoginRequest();
      req.setCode("code-1");

      when(wxApiClient.code2Session("code-1")).thenReturn(OPENID);
      when(userMapper.selectByOpenid(OPENID)).thenReturn(null);
      when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
        User u = inv.getArgument(0);
        u.setId(10L);
        return 1;
      });

      LoginResult result = service.login(req);

      assertTrue(result.isNewUser());
      assertEquals(TOKEN, result.getToken());
      assertEquals(10L, result.getUser().getId());
      verify(userMapper).insert(any(User.class));
      verify(userMapper).updateLastLogin(10L);
    }

    @Test
    void 注销用户_重新注册_匿名化后openid已清空查不到_走新用户分支() {
      WxLoginRequest req = new WxLoginRequest();
      req.setCode("code-reuse");

      when(wxApiClient.code2Session("code-reuse")).thenReturn(OPENID);
      when(userMapper.selectByOpenid(OPENID)).thenReturn(null);  // 匿名化后查不到
      when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
        User u = inv.getArgument(0);
        u.setId(11L);
        return 1;
      });

      LoginResult result = service.login(req);

      assertTrue(result.isNewUser());
      verify(userMapper).insert(any(User.class));
    }

    @Test
    void 老用户_直接复用不更新时_跳过updateProfile() {
      User existing = new User();
      existing.setId(5L);
      existing.setOpenid(OPENID);
      existing.setNickname("老昵称");
      existing.setStatus(0);

      WxLoginRequest req = new WxLoginRequest();
      req.setCode("code-2");

      when(wxApiClient.code2Session("code-2")).thenReturn(OPENID);
      when(userMapper.selectByOpenid(OPENID)).thenReturn(existing);

      LoginResult result = service.login(req);

      assertFalse(result.isNewUser());
      assertEquals(5L, result.getUser().getId());
      verify(userMapper, never()).updateProfile(any());
      verify(userMapper).updateLastLogin(5L);
    }

    @Test
    void 老用户_永久封禁_status1_直接拒绝() {
      User permanent = new User();
      permanent.setId(7L);
      permanent.setOpenid(OPENID);
      permanent.setStatus(1);  // 永久封禁

      WxLoginRequest req = new WxLoginRequest();
      req.setCode("code-3");

      when(wxApiClient.code2Session("code-3")).thenReturn(OPENID);
      when(userMapper.selectByOpenid(OPENID)).thenReturn(permanent);

      BizException ex = assertThrows(BizException.class, () -> service.login(req));
      assertEquals(1006, ex.getCode());
      assertTrue(ex.getMessage().contains("永久封禁"));
    }

    @Test
    void 老用户_临时封禁未到期_拒绝登录() {
      User tempBanned = new User();
      tempBanned.setId(8L);
      tempBanned.setOpenid(OPENID);
      tempBanned.setStatus(0);
      tempBanned.setBanUntil(LocalDateTime.now().plusDays(3));  // 3天后才解封

      WxLoginRequest req = new WxLoginRequest();
      req.setCode("code-4");

      when(wxApiClient.code2Session("code-4")).thenReturn(OPENID);
      when(userMapper.selectByOpenid(OPENID)).thenReturn(tempBanned);

      BizException ex = assertThrows(BizException.class, () -> service.login(req));
      assertEquals(1006, ex.getCode());
      verify(userMapper).unbanIfExpired(8L);  // 调了但返回 0（没解禁），后续封禁判断仍会触发
    }

    @Test
    void 老用户_临时封禁已到期_懒加载解禁后放行() {
      User expired = new User();
      expired.setId(9L);
      expired.setOpenid(OPENID);
      expired.setStatus(0);
      expired.setBanUntil(LocalDateTime.now().minusDays(1));  // 昨天就到期了

      WxLoginRequest req = new WxLoginRequest();
      req.setCode("code-5");

      when(wxApiClient.code2Session("code-5")).thenReturn(OPENID);
      when(userMapper.selectByOpenid(OPENID)).thenReturn(expired);
      when(userMapper.unbanIfExpired(anyLong())).thenReturn(1);

      LoginResult result = service.login(req);

      assertFalse(result.isNewUser());
      verify(userMapper).unbanIfExpired(9L);
    }

    @Test
    void openid拿不到_抛BizException() {
      WxLoginRequest req = new WxLoginRequest();
      req.setCode("bad");
      when(wxApiClient.code2Session("bad")).thenReturn("");

      BizException ex = assertThrows(BizException.class, () -> service.login(req));
      assertEquals(1001, ex.getCode());
    }
  }

  // ========== updateProfile ==========

  @Nested
  class UpdateProfile {

    @Test
    void 正常更新昵称和头像() {
      User existing = new User();
      existing.setId(42L);
      existing.setOpenid(OPENID);
      existing.setStatus(0);

      when(userMapper.selectById(42L)).thenReturn(existing);

      User result = service.updateProfile(42L, "新昵称", "cloud://a.jpg");

      assertEquals("新昵称", result.getNickname());
      assertEquals("cloud://a.jpg", result.getAvatar());
      verify(userMapper).updateProfile(existing);
    }

    @Test
    void 只传昵称_只更新昵称() {
      User existing = new User();
      existing.setId(42L);
      existing.setAvatar("cloud://old.jpg");
      existing.setStatus(0);

      when(userMapper.selectById(42L)).thenReturn(existing);

      service.updateProfile(42L, "昵称", null);

      assertEquals("昵称", existing.getNickname());
      assertEquals("cloud://old.jpg", existing.getAvatar());
      verify(userMapper).updateProfile(existing);
    }

    @Test
    void 两个都空_不调用update() {
      User existing = new User();
      existing.setId(42L);
      existing.setStatus(0);

      when(userMapper.selectById(42L)).thenReturn(existing);

      service.updateProfile(42L, "", null);

      verify(userMapper, never()).updateProfile(any());
    }

    @Test
    void 用户不存在_抛BizException() {
      when(userMapper.selectById(99L)).thenReturn(null);

      BizException ex = assertThrows(BizException.class,
          () -> service.updateProfile(99L, "x", null));
      assertEquals(1004, ex.getCode());
    }

    @Test
    void 永久封禁_拒绝更新() {
      User banned = new User();
      banned.setId(88L);
      banned.setStatus(1);

      when(userMapper.selectById(88L)).thenReturn(banned);

      BizException ex = assertThrows(BizException.class,
          () -> service.updateProfile(88L, "新昵称", null));
      assertEquals(1006, ex.getCode());
      assertTrue(ex.getMessage().contains("永久"));
    }

    @Test
    void 临时封禁未到期_拒绝更新() {
      User banned = new User();
      banned.setId(89L);
      banned.setStatus(0);
      banned.setBanUntil(LocalDateTime.now().plusDays(1));

      when(userMapper.selectById(89L)).thenReturn(banned);

      BizException ex = assertThrows(BizException.class,
          () -> service.updateProfile(89L, "新昵称", null));
      assertEquals(1006, ex.getCode());
    }
  }

  // ========== anonymize 匿名化注销 ==========

  @Nested
  class Anonymize {

    @Test
    void 正常匿名化() {
      when(userMapper.anonymize(55L)).thenReturn(1);

      assertDoesNotThrow(() -> service.anonymize(55L));
      verify(userMapper).anonymize(55L);
    }

    @Test
    void 用户不存在或已注销_抛BizException() {
      when(userMapper.anonymize(99L)).thenReturn(0);

      BizException ex = assertThrows(BizException.class,
          () -> service.anonymize(99L));
      assertEquals(1004, ex.getCode());
    }
  }

  // ========== getCurrentUser ==========

  @Nested
  class GetCurrentUser {

    @Test
    void 正常用户_直接返回() {
      User u = new User();
      u.setId(1L);
      u.setStatus(0);
      when(userMapper.selectById(1L)).thenReturn(u);

      User result = service.getCurrentUser(1L);
      assertNotNull(result);
      assertEquals(1L, result.getId());
      verify(userMapper, never()).unbanIfExpired(anyLong());
    }

    @Test
    void 临时封禁已过期_懒加载解禁并清banUntil() {
      User expired = new User();
      expired.setId(2L);
      expired.setStatus(0);
      expired.setBanUntil(LocalDateTime.now().minusDays(1));

      when(userMapper.selectById(2L)).thenReturn(expired);

      User result = service.getCurrentUser(2L);
      assertNotNull(result);
      assertNull(result.getBanUntil());
      verify(userMapper).unbanIfExpired(2L);
    }

    @Test
    void 临时封禁未过期_不动banUntil() {
      User stillBanned = new User();
      stillBanned.setId(3L);
      stillBanned.setStatus(0);
      stillBanned.setBanUntil(LocalDateTime.now().plusDays(2));

      when(userMapper.selectById(3L)).thenReturn(stillBanned);

      User result = service.getCurrentUser(3L);
      assertNotNull(result);
      verify(userMapper, never()).unbanIfExpired(anyLong());
    }

    @Test
    void 永久封禁_不动() {
      User permanent = new User();
      permanent.setId(4L);
      permanent.setStatus(1);

      when(userMapper.selectById(4L)).thenReturn(permanent);

      User result = service.getCurrentUser(4L);
      assertNotNull(result);
      verify(userMapper, never()).unbanIfExpired(anyLong());
    }

    @Test
    void 不存在返回null() {
      when(userMapper.selectById(99L)).thenReturn(null);

      assertNull(service.getCurrentUser(99L));
    }
  }
}