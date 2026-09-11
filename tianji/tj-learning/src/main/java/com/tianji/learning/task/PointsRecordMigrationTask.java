package com.tianji.learning.task;

import com.tianji.common.utils.CollUtils;
import com.tianji.learning.domian.po.PointsBoardSeason;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointsRecordMigrationTask {

    private static final String POINTS_RECORD_TABLE_PREFIX = "points_record_";

    private final IPointsBoardSeasonService seasonService;
    private final PointsRecordMapper recordMapper;

    /**
     * 每月1日凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 1 * ?")
    public void migrate() {
        // 1. 查询所有已结束的历史赛季
        List<PointsBoardSeason> seasons = seasonService.lambdaQuery()
                .lt(PointsBoardSeason::getEndTime, LocalDate.now())
                .list();

        if (CollUtils.isEmpty(seasons)) {
            return;
        }

        for (PointsBoardSeason season : seasons) {
            migrateSeason(season);
        }
    }

    private void migrateSeason(PointsBoardSeason season) {
        String tableName = POINTS_RECORD_TABLE_PREFIX + season.getId();
        LocalDateTime begin = season.getBeginTime().atStartOfDay();
        LocalDateTime end = season.getEndTime().atTime(LocalTime.MAX);

        try {
            // 1. 创建表
            recordMapper.createPointsRecordTable(tableName);

            // 2. 判断是否已经迁移过：目标表有数据且源表无数据则跳过
            Long targetCount = recordMapper.countTable(tableName);
            Long sourceCount = Long.valueOf(recordMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.tianji.learning.domian.po.PointsRecord>()
                            .between(com.tianji.learning.domian.po.PointsRecord::getCreateTime, begin, end)
            ));
            if ((targetCount != null && targetCount > 0) && (sourceCount == null || sourceCount == 0)) {
                log.debug("赛季{}积分明细已迁移，跳过", season.getId());
                return;
            }

            // 3. 迁移数据
            recordMapper.migratePointsRecord(tableName, begin, end);
            log.debug("赛季{}积分明细迁移完成，表名：{}", season.getId(), tableName);

            // 4. 删除原表数据
            recordMapper.deleteMigratedPointsRecord(begin, end);
            log.debug("赛季{}积分明细从 points_record 删除完成", season.getId());
        } catch (Exception e) {
            log.error("赛季{}积分明细迁移失败", season.getId(), e);
        }
    }
}