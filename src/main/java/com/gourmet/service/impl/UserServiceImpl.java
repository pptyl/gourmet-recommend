package com.gourmet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.model.LoginUser;
import com.gourmet.common.result.ResultCode;
import com.gourmet.common.util.JwtUtil;
import com.gourmet.dto.LoginDTO;
import com.gourmet.dto.PasswordUpdateDTO;
import com.gourmet.dto.ProfileUpdateDTO;
import com.gourmet.dto.RegisterDTO;
import com.gourmet.dto.TastePreferenceDTO;
import com.gourmet.entity.User;
import com.gourmet.mapper.UserMapper;
import com.gourmet.service.MessageService;
import com.gourmet.service.UserService;
import com.gourmet.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户账号服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MessageService messageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterDTO dto) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, dto.getPhone()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该手机号已注册，请直接登录");
        }

        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname())
                ? dto.getNickname()
                : "美食家" + dto.getPhone().substring(dto.getPhone().length() - 4));
        user.setTasteCuisine(dto.getTasteCuisine());
        user.setTasteFlavor(dto.getTasteFlavor());
        user.setTaboo(dto.getTaboo());
        user.setStatus(BizConstants.ENABLED);
        userMapper.insert(user);

        messageService.push(user.getId(), BizConstants.RECEIVER_USER, BizConstants.MSG_SYSTEM,
                "欢迎加入美食推荐", "完善口味偏好后，系统会为你推送更精准的美食商家。", null);

        log.info("新用户注册成功：id={}, phone={}", user.getId(), user.getPhone());
        return buildLoginVO(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, dto.getPhone()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "手机号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == BizConstants.DISABLED) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用，请联系平台客服");
        }
        return buildLoginVO(user);
    }

    @Override
    public User getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateProfile(Long userId, ProfileUpdateDTO dto) {
        User user = getProfile(userId);
        if (StringUtils.hasText(dto.getNickname())) {
            user.setNickname(dto.getNickname());
        }
        if (StringUtils.hasText(dto.getAvatar())) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getContactPhone() != null) {
            user.setContactPhone(dto.getContactPhone());
        }
        userMapper.updateById(user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, PasswordUpdateDTO dto) {
        User user = getProfile(userId);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaste(Long userId, TastePreferenceDTO dto) {
        User user = getProfile(userId);
        user.setTasteCuisine(dto.getTasteCuisine());
        user.setTasteFlavor(dto.getTasteFlavor());
        user.setTaboo(dto.getTaboo());
        userMapper.updateById(user);
    }

    @Override
    public User getById(Long userId) {
        return userId == null ? null : userMapper.selectById(userId);
    }

    @Override
    public User getByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    private LoginVO buildLoginVO(User user) {
        LoginUser loginUser = new LoginUser(user.getId(), BizConstants.ROLE_USER, user.getNickname());
        String token = jwtUtil.createToken(loginUser);
        return LoginVO.of(token, BizConstants.ROLE_USER, user.getId(),
                user.getNickname(), user.getAvatar(), jwtUtil.getExpireMinutes());
    }
}
