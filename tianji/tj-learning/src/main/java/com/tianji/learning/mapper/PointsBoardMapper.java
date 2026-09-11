package com.tianji.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianji.learning.domian.po.PointsBoard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PointsBoardMapper extends BaseMapper<PointsBoard> {

    void createPointsBoardTable(@Param("tableName") String tableName);

    /**
     * 查询历史赛季榜单列表
     */
    List<PointsBoard> queryHistoryBoardList(
            @Param("tableName") String tableName,
            @Param("from") int from,
            @Param("pageSize") int pageSize);

    /**
     * 查询我的历史赛季排名
     */
    PointsBoard queryMyHistoryBoard(
            @Param("tableName") String tableName,
            @Param("userId") Long userId);
}