package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.tianji.learning.domian.po.PointsRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface PointsRecordMapper extends BaseMapper<PointsRecord> {

    @Select("SELECT SUM(points) FROM points_record ${ew.customSqlSegment}")
    Integer queryUserPointsByTypeAndDate(@Param(Constants.WRAPPER) QueryWrapper<PointsRecord> wrapper);

    @Select("SELECT type, SUM(points) AS points FROM points_record ${ew.customSqlSegment} GROUP BY type")
    List<PointsRecord> queryUserPointsByDate(@Param(Constants.WRAPPER) QueryWrapper<PointsRecord> wrapper);

    /**
     * 创建赛季积分明细表
     */
    void createPointsRecordTable(@Param("tableName") String tableName);

    /**
     * 迁移积分明细到赛季表
     */
    void migratePointsRecord(
            @Param("targetTable") String targetTable,
            @Param("begin") LocalDateTime begin,
            @Param("end") LocalDateTime end);

    /**
     * 删除已迁移的积分明细
     */
    void deleteMigratedPointsRecord(
            @Param("begin") LocalDateTime begin,
            @Param("end") LocalDateTime end);

    /**
     * 统计表数据量
     */
    Long countTable(@Param("tableName") String tableName);
}