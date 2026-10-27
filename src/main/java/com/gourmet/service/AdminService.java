package com.gourmet.service;

import com.gourmet.common.result.PageResult;
import com.gourmet.dto.AdminLoginDTO;
import com.gourmet.dto.AdminMerchantQuery;
import com.gourmet.dto.AdminUserQuery;
import com.gourmet.dto.AuditDTO;
import com.gourmet.dto.NoteQuery;
import com.gourmet.dto.ReviewQuery;
import com.gourmet.entity.Merchant;
import com.gourmet.entity.Note;
import com.gourmet.entity.Review;
import com.gourmet.entity.User;
import com.gourmet.vo.LoginVO;

/**
 * 平台管理员业务：账号、商家审核、内容审核。
 */
public interface AdminService {

    /** 管理员登录 */
    LoginVO login(AdminLoginDTO dto);

    /** 用户账号分页 */
    PageResult<User> pageUsers(AdminUserQuery query);

    /** 启用 / 禁用用户 */
    void updateUserStatus(Long userId, Integer status);

    /** 商家分页（含待审核） */
    PageResult<Merchant> pageMerchants(AdminMerchantQuery query);

    /** 审核商家入驻申请 */
    void auditMerchant(Long merchantId, AuditDTO dto);

    /** 管控商家营业状态 */
    void updateMerchantStatus(Long merchantId, Integer status);

    /** 评价分页（含待审核） */
    PageResult<Review> pageReviews(ReviewQuery query);

    /** 审核评价 */
    void auditReview(Long reviewId, AuditDTO dto);

    /** 笔记分页（含待审核） */
    PageResult<Note> pageNotes(NoteQuery query);

    /** 审核笔记 */
    void auditNote(Long noteId, AuditDTO dto);
}
