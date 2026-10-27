package com.gourmet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.model.LoginUser;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.ResultCode;
import com.gourmet.common.util.JwtUtil;
import com.gourmet.dto.AdminLoginDTO;
import com.gourmet.dto.AuditDTO;
import com.gourmet.dto.AdminMerchantQuery;
import com.gourmet.dto.AdminUserQuery;
import com.gourmet.dto.BroadcastDTO;
import com.gourmet.dto.NoteQuery;
import com.gourmet.dto.ReviewQuery;
import com.gourmet.entity.Admin;
import com.gourmet.entity.Merchant;
import com.gourmet.entity.Note;
import com.gourmet.entity.Review;
import com.gourmet.entity.User;
import com.gourmet.mapper.AdminMapper;
import com.gourmet.mapper.MerchantMapper;
import com.gourmet.mapper.UserMapper;
import com.gourmet.service.AdminService;
import com.gourmet.service.MessageService;
import com.gourmet.service.NoteService;
import com.gourmet.service.ReviewService;
import com.gourmet.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 平台管理员业务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final ReviewService reviewService;
    private final NoteService noteService;
    private final MessageService messageService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginVO login(AdminLoginDTO dto) {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, dto.getUsername()));
        if (admin == null || !passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == BizConstants.DISABLED) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该管理员账号已被禁用");
        }
        LoginUser loginUser = new LoginUser(admin.getId(), BizConstants.ROLE_ADMIN, admin.getNickname());
        String token = jwtUtil.createToken(loginUser);
        return LoginVO.of(token, BizConstants.ROLE_ADMIN, admin.getId(),
                admin.getNickname(), null, jwtUtil.getExpireMinutes());
    }

    @Override
    public PageResult<User> pageUsers(AdminUserQuery query) {
        Page<User> page = query.toPage();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(query.getStatus() != null, User::getStatus, query.getStatus())
                .orderByDesc(User::getCreateTime);
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(User::getNickname, query.getKeyword())
                    .or().like(User::getPhone, query.getKeyword()));
        }
        return PageResult.of(userMapper.selectPage(page, wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);

        messageService.push(userId, BizConstants.RECEIVER_USER, BizConstants.MSG_SYSTEM,
                status == BizConstants.ENABLED ? "账号已恢复" : "账号已被禁用",
                status == BizConstants.ENABLED ? "你的账号已恢复正常使用。" : "你的账号因违反平台规范已被禁用，如有疑问请联系客服。",
                null);
    }

    @Override
    public PageResult<Merchant> pageMerchants(AdminMerchantQuery query) {
        Page<Merchant> page = query.toPage();
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .eq(query.getAuditStatus() != null, Merchant::getAuditStatus, query.getAuditStatus())
                .eq(query.getStatus() != null, Merchant::getStatus, query.getStatus())
                .orderByAsc(Merchant::getAuditStatus)
                .orderByDesc(Merchant::getCreateTime);
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Merchant::getName, query.getKeyword())
                    .or().like(Merchant::getPhone, query.getKeyword()));
        }
        return PageResult.of(merchantMapper.selectPage(page, wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditMerchant(Long merchantId, AuditDTO dto) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商家不存在");
        }
        boolean pass = Boolean.TRUE.equals(dto.getPass());
        merchant.setAuditStatus(pass ? BizConstants.MERCHANT_AUDIT_PASSED : BizConstants.MERCHANT_AUDIT_REJECTED);
        merchant.setAuditRemark(StringUtils.hasText(dto.getRemark())
                ? dto.getRemark()
                : (pass ? "资料审核通过" : "资料不完整，请补充后重新提交"));
        if (pass) {
            merchant.setStatus(BizConstants.MERCHANT_OPEN);
        }
        merchantMapper.updateById(merchant);

        messageService.push(merchantId, BizConstants.RECEIVER_MERCHANT, BizConstants.MSG_AUDIT,
                pass ? "入驻审核通过" : "入驻申请被驳回",
                pass ? "店铺「" + merchant.getName() + "」已通过平台审核，现在可以登录商家后台维护菜品与优惠券了。"
                        : "店铺「" + merchant.getName() + "」的入驻申请未通过：" + merchant.getAuditRemark(),
                merchantId);

        // 新店开业：向全平台用户推送开业消息
        if (pass) {
            BroadcastDTO broadcast = new BroadcastDTO();
            broadcast.setTitle("新店开业：" + merchant.getName());
            broadcast.setContent("「" + merchant.getName() + "」已入驻平台，位于" + merchant.getArea() + "，快去打卡吧！");
            broadcast.setReceiverType(BizConstants.RECEIVER_USER);
            broadcast.setType(BizConstants.MSG_NEW_SHOP);
            messageService.broadcast(broadcast);
        }
        log.info("商家审核完成：id={}, pass={}", merchantId, pass);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMerchantStatus(Long merchantId, Integer status) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商家不存在");
        }
        if (merchant.getAuditStatus() == null || merchant.getAuditStatus() != BizConstants.MERCHANT_AUDIT_PASSED) {
            throw new BusinessException("未通过审核的商家不能变更营业状态");
        }
        merchant.setStatus(status);
        merchantMapper.updateById(merchant);

        messageService.push(merchantId, BizConstants.RECEIVER_MERCHANT, BizConstants.MSG_AUDIT,
                status == BizConstants.MERCHANT_OPEN ? "店铺已恢复正常营业" : "店铺已被停业整顿",
                status == BizConstants.MERCHANT_OPEN ? "平台已解除对你店铺的限制，可以正常营业了。"
                        : "你的店铺因违规被停业整顿，请整改后联系平台恢复。",
                merchantId);
    }

    @Override
    public PageResult<Review> pageReviews(ReviewQuery query) {
        return reviewService.pageAll(query);
    }

    @Override
    public void auditReview(Long reviewId, AuditDTO dto) {
        reviewService.audit(reviewId, Boolean.TRUE.equals(dto.getPass()), dto.getRemark());
    }

    @Override
    public PageResult<Note> pageNotes(NoteQuery query) {
        return noteService.pageAll(query);
    }

    @Override
    public void auditNote(Long noteId, AuditDTO dto) {
        noteService.audit(noteId, Boolean.TRUE.equals(dto.getPass()), dto.getRemark());
    }
}
