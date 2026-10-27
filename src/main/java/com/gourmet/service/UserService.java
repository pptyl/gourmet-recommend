package com.gourmet.service;

import com.gourmet.dto.LoginDTO;
import com.gourmet.dto.PasswordUpdateDTO;
import com.gourmet.dto.ProfileUpdateDTO;
import com.gourmet.dto.RegisterDTO;
import com.gourmet.dto.TastePreferenceDTO;
import com.gourmet.entity.User;
import com.gourmet.vo.LoginVO;

/**
 * 用户账号与个人资料。
 */
public interface UserService {

    /** 注册并直接返回登录令牌 */
    LoginVO register(RegisterDTO dto);

    /** 手机号 + 密码登录 */
    LoginVO login(LoginDTO dto);

    /** 查询个人资料 */
    User getProfile(Long userId);

    /** 修改昵称 / 头像 / 联系电话 */
    User updateProfile(Long userId, ProfileUpdateDTO dto);

    /** 修改登录密码 */
    void updatePassword(Long userId, PasswordUpdateDTO dto);

    /** 设置口味偏好 */
    void updateTaste(Long userId, TastePreferenceDTO dto);

    /** 内部使用：按 ID 查询用户 */
    User getById(Long userId);

    /** 内部使用：按手机号查询用户 */
    User getByPhone(String phone);
}
