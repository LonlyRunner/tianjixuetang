-- 分组实战：学习笔记（tj_learning）与考试评测（tj_exam）

USE `tj_learning`;

CREATE TABLE IF NOT EXISTS `note` (
  `id` bigint NOT NULL COMMENT '笔记id',
  `user_id` bigint NOT NULL COMMENT '笔记所属用户id',
  `course_id` bigint NOT NULL COMMENT '课程id',
  `chapter_id` bigint NOT NULL COMMENT '章id',
  `section_id` bigint NOT NULL COMMENT '小节id',
  `note_moment` int NOT NULL COMMENT '视频点位（秒）',
  `content` varchar(1000) NOT NULL COMMENT '笔记内容',
  `gathered_times` int NOT NULL DEFAULT 0 COMMENT '被采集次数',
  `liked_times` int NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `is_private` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否私密',
  `hidden` bit(1) NOT NULL DEFAULT b'0' COMMENT '管理端是否隐藏',
  `hidden_reason` varchar(255) DEFAULT NULL COMMENT '隐藏原因',
  `author_id` bigint NOT NULL COMMENT '原作者id',
  `gathered_note_id` bigint DEFAULT NULL COMMENT '原笔记id',
  `is_gathered` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否采集副本',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_gathered_note` (`user_id`, `gathered_note_id`),
  KEY `idx_course_section` (`course_id`, `section_id`),
  KEY `idx_public_notes` (`is_private`, `is_gathered`, `hidden`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习笔记';

USE `tj_exam`;

CREATE TABLE IF NOT EXISTS `exam_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评测记录id',
  `type` tinyint NOT NULL COMMENT '1-练习，2-考试',
  `course_id` bigint NOT NULL,
  `section_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `score` int NOT NULL DEFAULT 0,
  `correct_questions` int NOT NULL DEFAULT 0,
  `duration` int NOT NULL DEFAULT 0 COMMENT '用时（秒）',
  `finished` bit(1) NOT NULL DEFAULT b'0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finish_time` datetime DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_finish_time` (`user_id`, `finished`, `finish_time`),
  KEY `idx_user_section_type` (`user_id`, `section_id`, `type`),
  KEY `idx_user_section_type_finished` (`user_id`, `section_id`, `type`, `finished`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试/练习记录';

CREATE TABLE IF NOT EXISTS `exam_record_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exam_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `correct` bit(1) NOT NULL DEFAULT b'0',
  `score` int NOT NULL DEFAULT 0,
  `answer` varchar(1000) DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_question` (`exam_id`, `question_id`),
  KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试/练习答案明细';

USE `tj_learning`;
ALTER TABLE `points_record`
    MODIFY COLUMN `type` TINYINT NOT NULL COMMENT '积分方式：1-课程学习，2-每日签到，3-课程问答，4-课程笔记，5-课程评价，6-考试评测';
