package com.gourmet.controller;

import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.context.UserContext;
import com.gourmet.common.result.Result;
import com.gourmet.dto.LoginDTO;
import com.gourmet.dto.PasswordUpdateDTO;
import com.gourmet.dto.ProfileUpdateDTO;
import com.gourmet.dto.RegisterDTO;
import com.gourmet.dto.TastePreferenceDTO;
import com.gourmet.entity.User;
import com.gourmet.service.MessageService;
import com.gourmet.service.StatisticService;
import com.gourmet.service.UserService;
import com.gourmet.vo.LoginVO;
import com.gourmet.vo.UserCenterVO;
import com.gourmet.vo.UserStatisticVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户账号管理。
 */
@Tag(name = "01-用户账号", description = "注册登录、个人资料、口味偏好、个人中心")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final StatisticService statisticService;
    private final MessageService messageService;

    @Operation(summary = "注册（注册成功直接返回登录令牌）")
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok("注册成功", userService.register(dto));
    }

    @Operation(summary = "手机号 + 密码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok("登录成功", userService.login(dto));
    }

    @Operation(summary = "退出登录（JWT 无状态，客户端丢弃令牌即可）")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok("已退出登录", null);
    }

    @Operation(summary = "查看个人资料")
    @GetMapping("/profile")
    public Result<User> profile() {
        return Result.ok(userService.getProfile(UserContext.getUserId()));
    }

    @Operation(summary = "修改个人资料：昵称、头像、联系电话")
    @PutMapping("/profile")
    public Result<User> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        return Result.ok("资料已更新", userService.updateProfile(UserContext.getUserId(), dto));
    }

    @Operation(summary = "修改登录密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        userService.updatePassword(UserContext.getUserId(), dto);
        return Result.ok("密码修改成功，请重新登录", null);
    }

    @Operation(summary = "查看口味偏好")
    @GetMapping("/taste")
    public Result<User> taste() {
        return Result.ok(userService.getProfile(UserContext.getUserId()));
    }

    @Operation(summary = "设置口味偏好：菜系、口味轻重、忌口食材")
    @PutMapping("/taste")
    public Result<Void> updateTaste(@Valid @RequestBody TastePreferenceDTO dto) {
        userService.updateTaste(UserContext.getUserId(), dto);
        return Result.ok("口味偏好已保存", null);
    }

    @Operation(summary = "个人中心：资料 + 统计 + 未读消息")
    @GetMapping("/center")
    public Result<UserCenterVO> center() {
        Long userId = UserContext.getUserId();
        UserCenterVO vo = new UserCenterVO();
        vo.setUser(userService.getProfile(userId));
        UserStatisticVO statistic = statisticService.userStatistic(userId);
        vo.setStatistic(statistic);
        vo.setUnreadMessageCount(messageService.unreadCount(userId, BizConstants.RECEIVER_USER));
        return Result.ok(vo);
    }
}
