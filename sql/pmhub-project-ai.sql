SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `pmhub-project`;

CREATE TABLE IF NOT EXISTS `project_ai_analysis_task` (
  `id` varchar(64) NOT NULL,
  `project_id` varchar(64) NOT NULL,
  `trigger_type` varchar(32) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  `started_time` datetime DEFAULT NULL,
  `finished_time` datetime DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_project_status_time` (`project_id`, `status`, `created_time`),
  KEY `idx_project_time` (`project_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='project AI analysis task';

CREATE TABLE IF NOT EXISTS `project_health_snapshot` (
  `id` varchar(64) NOT NULL,
  `project_id` varchar(64) NOT NULL,
  `analysis_task_id` varchar(64) DEFAULT NULL,
  `health_score` int(11) DEFAULT NULL,
  `health_level` varchar(32) DEFAULT NULL,
  `deduction_detail` varchar(2000) DEFAULT NULL,
  `risk_count` int(11) DEFAULT '0',
  `high_risk_count` int(11) DEFAULT '0',
  `snapshot_time` datetime DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_project_snapshot_time` (`project_id`, `snapshot_time`, `created_time`),
  KEY `idx_analysis_task` (`analysis_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='project health snapshot';

CREATE TABLE IF NOT EXISTS `project_risk_record` (
  `id` varchar(64) NOT NULL,
  `project_id` varchar(64) NOT NULL,
  `analysis_task_id` varchar(64) DEFAULT NULL,
  `risk_type` varchar(64) NOT NULL,
  `risk_level` varchar(32) NOT NULL,
  `source_type` varchar(64) DEFAULT NULL,
  `source_id` varchar(128) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `reason` varchar(1000) DEFAULT NULL,
  `suggestion` varchar(1000) DEFAULT NULL,
  `status` varchar(32) DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_project_time` (`project_id`, `created_time`),
  KEY `idx_analysis_task_time` (`analysis_task_id`, `created_time`),
  KEY `idx_project_type_level` (`project_id`, `risk_type`, `risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='project risk record';

CREATE TABLE IF NOT EXISTS `project_weekly_report` (
  `id` varchar(64) NOT NULL,
  `project_id` varchar(64) NOT NULL,
  `analysis_task_id` varchar(64) DEFAULT NULL,
  `week_start` date NOT NULL,
  `week_end` date NOT NULL,
  `content` text,
  `structured_content` longtext,
  `version` int(11) NOT NULL DEFAULT '1',
  `status` varchar(32) NOT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  `created_by` varchar(100) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_project_week_version` (`project_id`, `week_start`, `week_end`, `version`),
  KEY `idx_project_created_time` (`project_id`, `created_time`),
  KEY `idx_analysis_task` (`analysis_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='project weekly report';

SET FOREIGN_KEY_CHECKS = 1;
