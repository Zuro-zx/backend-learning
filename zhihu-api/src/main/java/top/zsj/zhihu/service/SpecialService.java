package top.zsj.zhihu.service;

import top.zsj.zhihu.entity.Special;

import java.util.List;

/**
 * 专题业务接口
 *
 * @author zsj
 * @date 2026/10/19
 * @description 专题业务接口
 */
public interface SpecialService {

    /**
     * 获取所有专题，按更新时间倒序
     *
     * @return 专题列表
     */
    List<Special> getAll();
}
