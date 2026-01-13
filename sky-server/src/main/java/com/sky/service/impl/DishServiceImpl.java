package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    /**
     * 新增菜品和对应的口味
     *
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //向菜品表插入1条数据
        dishMapper.insert(dish);

        //获取菜品的口味列表
        List<DishFlavor> flavors = dishDTO.getFlavors();
        //获取插入后的id
        Long dishId = dish.getId();

        //向口味表插入n条数据
        if (flavors != null && !flavors.isEmpty()) {
            //把id赋给每一个dishFlavor
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 根据id查询菜品和对应的口味
     *
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        return dishMapper.getByIdWithFlavor(id);
    }

    /**
     * 根据分类id查询菜品列表
     *
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> list(Long categoryId) {
        return dishMapper.list(categoryId);
    }

    /**
     * 启售禁售菜品
     *
     * @param status
     * @return
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder().id(id).status(status).build();
        dishMapper.update(dish);
    }

    /**
     * 修改菜品
     *
     * @param dishDTO
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //更新菜品表
        dishMapper.update(dish);

        //更新口味表
        List<DishFlavor> flavors = dishDTO.getFlavors();

        //获取菜品id
        Long dishId = dish.getId();

        //不论flavors是否为空，先删除后插入
        List<Long> ids = new java.util.ArrayList<>();
        ids.add(dishId);
        dishFlavorMapper.deleteBatchByDishId(ids);
        if (flavors != null && !flavors.isEmpty()) {
            //把id赋给每一个dishFlavor
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 根据id批量删除菜品
     *
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatchById(List<Long> ids) {
        //启售中的菜品不能删除
        // TODO 逻辑待完善
        //被套餐关联的菜品不能删除

        //先删除菜品表
        dishMapper.deleteBatchById(ids);
        //再删除口味表
        dishFlavorMapper.deleteBatchByDishId(ids);
    }
}
