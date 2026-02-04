package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 统计指定时间区间内的营业额数据
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 当前集合用于存放从begin到end的每天日期
        List<LocalDate> dateList = new ArrayList<>();

        // 检查begin和end是否有误
        if (begin.isAfter(end)) {
            throw new OrderBusinessException("开始时间不能大于结束时间");
        }
        // 循环到end，将每天的日期添加到dateList中
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 获取日期对应的营业额数据，营业额是指：状态为“已完成”的订单金额合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的用户数据
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        if (begin.isAfter(end)) {
            throw new OrderBusinessException("开始时间不能大于结束时间");
        }
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 存放每天新增的用户数量
        List<Integer> newUserList = new ArrayList<>();
        // 存放每天的总用户数量
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            // 获取日期对应的用户数据，用户数据是指：日期对应的新用户数和总用户数
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map<String, Object> map = new HashMap<>();

            // 获取总用户数
            map.put("end", endTime);
            Integer totalUser = userMapper.countByMap(map);
            totalUser = totalUser == null ? 0 : totalUser;
            totalUserList.add(totalUser);

            // 获取新增用户数
            map.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(map);
            newUser = newUser == null ? 0 : newUser;
            newUserList.add(newUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的订单数据
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        if (begin.isAfter(end)) {
            throw new OrderBusinessException("开始时间不能大于结束时间");
        }
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Integer orderCount = getOrderCount(beginTime, endTime, null);
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }
        // 计算时间区间内的订单总数量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        // 计算时间区间内的有效订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        // 计算订单完成率
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : validOrderCount.doubleValue() / totalOrderCount;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 统计指定时间区间内的销量排名top10
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> name = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(name, ",");

        List<Integer> number = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(number, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) throws IOException {
        // 查询数据库，获取营业数据--查询最近30天的运营数据
        LocalDateTime begin = LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = LocalDate.now().minusDays(1).atTime(23, 59, 59);

        // 查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(begin, end);

        // 通过POI将数据写入Excel文件中
        String templateUrl = "templates/运营数据报表模板.xlsx";
        InputStream template = this.getClass()
                .getClassLoader()
                .getResourceAsStream(templateUrl);
        
        // 空值检查
        if (template == null) {
            throw new RuntimeException("无法找到Excel模板文件: " + templateUrl);
        }
        
        // 基于模板文件创建一个新的Excel文件
        XSSFWorkbook excel = new XSSFWorkbook(template);

        // 获取第一个sheet
        XSSFSheet sheet = excel.getSheet("Sheet1");

        // 填充概览数据--时间(2,2)
        sheet.getRow(1).getCell(1).setCellValue("时间: " + begin + " 至 " + end);

        // 填充概览数据--营业额(4,3)--订单完成率(4,5)--新增用户数(4,7)
        XSSFRow row = sheet.getRow(3); // 获取第4行数据
        row.getCell(2).setCellValue(businessDataVO.getTurnover());
        row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
        row.getCell(6).setCellValue(businessDataVO.getNewUsers());

        // 填充概览数据--有效订单数(5,3)--平均客单价(5,5)
        row = sheet.getRow(4);
        row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
        row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

        // 填充明细数据
        for (int i = 0; i < 30; i++){
            LocalDateTime date = begin.plusDays(i);
            BusinessDataVO businessData = workspaceService.getBusinessData(date, date.plusDays(1).plusSeconds(-1));

            // 获取某一行
            row = sheet.getRow(7 + i);
            row.getCell(1).setCellValue(date.toLocalDate().toString());
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(3).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(5).setCellValue(businessData.getUnitPrice());
            row.getCell(6).setCellValue(businessData.getNewUsers());
        }

        // 通过输出流将Excel文件下载到客户端浏览器
        try (OutputStream out = response.getOutputStream()) {
            excel.write(out);
        } finally {
            // 关闭资源
            template.close();
            excel.close();
        }
    }

    /**
     * 根据条件统计订单数量
     *
     * @param beginTime
     * @param endTime
     * @param status
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", beginTime);
        map.put("end", endTime);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }
}
