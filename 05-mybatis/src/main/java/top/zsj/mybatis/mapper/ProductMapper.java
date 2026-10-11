package top.zsj.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.zsj.mybatis.entity.Product;

/**
 * 商品 Mapper
 *
 * @author zsj
 */
@Mapper
@Repository
public interface ProductMapper extends BaseMapper<Product> {
}
