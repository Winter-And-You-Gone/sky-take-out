package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 菜品管理
 */
@Slf4j
@RestController
@RequestMapping("/admin/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    /**
     * 新增菜品和对应的口味
     *
     * @param dishDTO
     * @return
     */
    @PostMapping
    public Result<Object> save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }
    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询：{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }
    /**
     * 根据id查询菜品和对应的口味
     */
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id) {
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 根据分类id查询菜品列表
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    public Result<List<Dish>> list(Long categoryId) {
        log.info("根据分类id查询菜品：{}", categoryId);
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    /**
     * 启售禁售菜品
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public Result<Object> startOrStop(@PathVariable Integer status, Long id) {
        log.info("启售禁售菜品：status({}) id({})", status, id);
        dishService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    @PutMapping
    public Result<Object> update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品信息：{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
        return Result.success();
    }

    /**
     * 根据id批量删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result<Object> delete(@RequestParam List<Long> ids) {
        log.info("批量删除菜品：{}", ids);
        dishService.deleteBatchById(ids);
        return Result.success();
    }
}
