package top.zsj.zhihu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import top.zsj.zhihu.entity.Special;
import top.zsj.zhihu.mapper.SpecialMapper;
import top.zsj.zhihu.service.SpecialService;

import java.util.List;

/**
 * 专题业务实现
 *
 * @author zsj
 * @date 2026/10/19
 * @description 专题业务实现
 */
@Service
public class SpecialServiceImpl implements SpecialService {

    @Resource
    private SpecialMapper specialMapper;

    @Override
    public List<Special> getAll() {
        LambdaQueryWrapper<Special> wrapper = new LambdaQueryWrapper<>();
        // 根据更新时间降序排列
        wrapper.orderByDesc(Special::getUpdated);
        return specialMapper.selectList(wrapper);
    }
}
