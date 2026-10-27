package com.gourmet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gourmet.common.constant.BizConstants;
import com.gourmet.common.exception.BusinessException;
import com.gourmet.common.query.PageQuery;
import com.gourmet.common.result.PageResult;
import com.gourmet.common.result.ResultCode;
import com.gourmet.dto.FeedbackDTO;
import com.gourmet.entity.Merchant;
import com.gourmet.entity.RecommendFeedback;
import com.gourmet.entity.RecommendLog;
import com.gourmet.mapper.MerchantMapper;
import com.gourmet.mapper.RecommendFeedbackMapper;
import com.gourmet.mapper.RecommendLogMapper;
import com.gourmet.service.RecommendFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 推荐反馈与日志服务实现。
 */
@Service
@RequiredArgsConstructor
public class RecommendFeedbackServiceImpl implements RecommendFeedbackService {

    private final RecommendFeedbackMapper feedbackMapper;
    private final RecommendLogMapper recommendLogMapper;
    private final MerchantMapper merchantMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecommendFeedback submit(Long userId, FeedbackDTO dto) {
        Merchant merchant = merchantMapper.selectById(dto.getMerchantId());
        if (merchant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商家不存在");
        }

        RecommendFeedback feedback = feedbackMapper.selectOne(new LambdaQueryWrapper<RecommendFeedback>()
                .eq(RecommendFeedback::getUserId, userId)
                .eq(RecommendFeedback::getMerchantId, dto.getMerchantId()));

        if (feedback == null) {
            feedback = new RecommendFeedback();
            feedback.setUserId(userId);
            feedback.setMerchantId(dto.getMerchantId());
            feedback.setFeedbackType(dto.getFeedbackType());
            feedback.setSource(StringUtils.hasText(dto.getSource()) ? dto.getSource() : BizConstants.SOURCE_HOME);
            try {
                feedbackMapper.insert(feedback);
            } catch (DuplicateKeyException e) {
                // 并发重复提交，转为更新
                feedback = feedbackMapper.selectOne(new LambdaQueryWrapper<RecommendFeedback>()
                        .eq(RecommendFeedback::getUserId, userId)
                        .eq(RecommendFeedback::getMerchantId, dto.getMerchantId()));
                if (feedback != null) {
                    feedback.setFeedbackType(dto.getFeedbackType());
                    feedbackMapper.updateById(feedback);
                }
            }
        } else {
            feedback.setFeedbackType(dto.getFeedbackType());
            if (StringUtils.hasText(dto.getSource())) {
                feedback.setSource(dto.getSource());
            }
            feedbackMapper.updateById(feedback);
        }
        return feedback;
    }

    @Override
    public PageResult<RecommendFeedback> pageMine(Long userId, PageQuery query) {
        Page<RecommendFeedback> page = query.toPage();
        Page<RecommendFeedback> result = feedbackMapper.selectPage(page,
                new LambdaQueryWrapper<RecommendFeedback>()
                        .eq(RecommendFeedback::getUserId, userId)
                        .orderByDesc(RecommendFeedback::getCreateTime));
        fillMerchant(result.getRecords());
        return PageResult.of(result);
    }

    @Override
    public PageResult<RecommendLog> pageMyLogs(Long userId, PageQuery query) {
        Page<RecommendLog> page = query.toPage();
        Page<RecommendLog> result = recommendLogMapper.selectPage(page,
                new LambdaQueryWrapper<RecommendLog>()
                        .eq(RecommendLog::getUserId, userId)
                        .orderByDesc(RecommendLog::getCreateTime));
        fillLogMerchant(result.getRecords());
        return PageResult.of(result);
    }

    @Override
    public PageResult<RecommendLog> pageAllLogs(PageQuery query, String algorithm) {
        Page<RecommendLog> page = query.toPage();
        Page<RecommendLog> result = recommendLogMapper.selectPage(page,
                new LambdaQueryWrapper<RecommendLog>()
                        .eq(StringUtils.hasText(algorithm), RecommendLog::getAlgorithm, algorithm)
                        .orderByDesc(RecommendLog::getCreateTime));
        fillLogMerchant(result.getRecords());
        return PageResult.of(result);
    }

    private void fillMerchant(List<RecommendFeedback> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream().map(RecommendFeedback::getMerchantId).distinct().toList();
        Map<Long, Merchant> map = merchantMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Merchant::getId, Function.identity()));
        records.forEach(feedback -> {
            Merchant merchant = map.get(feedback.getMerchantId());
            if (merchant != null) {
                feedback.setMerchantName(merchant.getName());
                feedback.setCoverImage(merchant.getCoverImage());
            }
        });
    }

    private void fillLogMerchant(List<RecommendLog> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream().map(RecommendLog::getMerchantId).distinct().toList();
        Map<Long, Merchant> map = merchantMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Merchant::getId, Function.identity()));
        records.forEach(log -> {
            Merchant merchant = map.get(log.getMerchantId());
            if (merchant != null) {
                log.setMerchantName(merchant.getName());
            }
        });
    }
}
