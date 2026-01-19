package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    /**
     * 批量插入口味数据
     * @param flavors
     */
    void insertBatch(List<DishFlavor> flavors);

    /**
     * 批量根据菜品id删除对应的口味数据
     * @param ids
     */
    void deleteBatchByDishId(List<Long> ids);

    /**
     * 根据菜品id查询对应的口味数据
     * @param id
     * @return
     */
    List<DishFlavor> getByDishId(Long id);
}
