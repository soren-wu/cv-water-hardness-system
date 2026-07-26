package com.example.titration.module.threshold.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.titration.common.exception.BusinessException;
import com.example.titration.module.threshold.entity.ThresholdTemplate;
import com.example.titration.module.threshold.mapper.ThresholdTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThresholdService {

    private final ThresholdTemplateMapper templateMapper;

    public List<ThresholdTemplate> listTemplates() {
        LambdaQueryWrapper<ThresholdTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ThresholdTemplate::getStatus, "ENABLED");
        wrapper.orderByDesc(ThresholdTemplate::getIsDefault)
               .orderByDesc(ThresholdTemplate::getCreatedAt);
        return templateMapper.selectList(wrapper);
    }

    public ThresholdTemplate getDefaultTemplate() {
        ThresholdTemplate template = templateMapper.selectOne(
                new LambdaQueryWrapper<ThresholdTemplate>()
                        .eq(ThresholdTemplate::getIsDefault, 1)
                        .eq(ThresholdTemplate::getStatus, "ENABLED"));
        if (template == null) {
            throw new BusinessException(404, "没有可用的默认阈值模板");
        }
        return template;
    }

    public ThresholdTemplate getTemplateById(Long id) {
        ThresholdTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(404, "阈值模板不存在");
        }
        return template;
    }

    public ThresholdTemplate createTemplate(ThresholdTemplate template) {
        templateMapper.insert(template);
        return template;
    }

    public ThresholdTemplate updateTemplate(Long id, ThresholdTemplate template) {
        getTemplateById(id);
        template.setId(id);
        templateMapper.updateById(template);
        return templateMapper.selectById(id);
    }

    public void deleteTemplate(Long id) {
        if (templateMapper.deleteById(id) == 0) {
            throw new BusinessException(404, "阈值模板不存在");
        }
    }
}
