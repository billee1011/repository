CREATE TABLE `server_info` (
`id` INT(11) UNIQUE NOT NULL,
`name` VARCHAR(255),
`status` INT(11),
`times` INT(11),
`cq_time` TIMESTAMP NULL,
`start_time` TIMESTAMP NULL,
`maintain_time` TIMESTAMP NULL,
`combine_time` TIMESTAMP NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `user` (
`id` BIGINT(20) UNIQUE NOT NULL,
`pid` VARCHAR(20) COMMENT '平台 ios or android',
`user_id` VARCHAR(128) COMMENT '用户账号',
`type` INT(11) NOT NULL DEFAULT 0 COMMENT '用户类型',
`access_token` VARCHAR(128) COMMENT '微信的token',
`refresh_token` VARCHAR(128) COMMENT '微信的刷新token(续期accessToken用)',
`maching_id` VARCHAR(64) COMMENT '设备码',
`token_end_time` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00' COMMENT 'token的过期时间',
`add_time` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00' COMMENT '创建时间',
`modify_time` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00' COMMENT '修改时间',
PRIMARY KEY (`id`),
UNIQUE INDEX INDEX_PD_UD (pid,user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `role` (
`id` BIGINT(20) UNIQUE NOT NULL,
`pid` VARCHAR(20) COMMENT '平台 ios or android',
`user_id` VARCHAR(128) COMMENT '用户账号',
`name` VARCHAR(1024) COMMENT '角色名',
`gender` INT(11) NOT NULL DEFAULT 0 COMMENT '性別',
`province` VARCHAR(128) COMMENT '省份',
`city` VARCHAR(64) COMMENT '城市',
`country` VARCHAR(64) COMMENT 'country',
`headimgurl` VARCHAR(64) COMMENT '头像名',
`diamond` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '钻石',
`state` INT(11) NOT NULL DEFAULT 0 COMMENT '状态',
`ip` VARCHAR(30) COMMENT 'ip',
`total_login_days` INT(11) DEFAULT 0 COMMENT '累计登陆天数',
`online_millis` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '角色在线时长总和',
`last_login_time` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00' COMMENT '最后登录时间',
`last_logout_time` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00' COMMENT '最后登出时间',
`log_ids` TEXT COMMENT '所有战绩的集合',
`add_time` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00' COMMENT '创建时间',
`modify_time` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00' COMMENT '修改时间',
PRIMARY KEY (`id`),
KEY `INDEX_PD_UD` (`pid`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 麻将结果
CREATE TABLE `mahjong_result_log` (
  `id` bigint(20) NOT NULL,
  `room_num` int(11) NOT NULL DEFAULT '0' COMMENT '房间号',
  `all_info` text COMMENT '对战数据记录',
  `add_time` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' COMMENT '创建时间',
  `modify_time` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `INDEX_AT` (`add_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- 公告
CREATE TABLE `announce` (
`id` BIGINT(20) UNIQUE NOT NULL,
`announce_id` INT(11),
`content` VARCHAR(255),
`interval_gap` INT(11),
`begin_time` TIMESTAMP NULL,
`end_time` TIMESTAMP NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 玩家邮件
CREATE TABLE `mail` (
`id` BIGINT(20) UNIQUE NOT NULL,
`role_id` BIGINT(20),
`title` VARCHAR(128) COMMENT '标题',
`content` VARCHAR(1024) COMMENT '内容',
`sender_id` BIGINT(20) COMMENT '发件人ID',
`sender_name` VARCHAR(36) COMMENT '发件人名称',
`status` INT(11) NOT NULL DEFAULT 0 COMMENT '邮件状态',
`attachment` TEXT COMMENT '附件',
`diamond` INT(11) NOT NULL DEFAULT 0 COMMENT '钻石数',
`add_time` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00' COMMENT '创建时间',
`modify_time` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00' COMMENT '修改时间',
`mail_type` INT(11) NOT NULL DEFAULT 0 COMMENT '邮件类型',
PRIMARY KEY (`id`),
INDEX INDEX_RD USING BTREE (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 系统邮件
CREATE TABLE `system_mail` (
`id` INT(11) UNIQUE NOT NULL AUTO_INCREMENT,
`title` VARCHAR(255),
`content` VARCHAR(255),
`attachment` TEXT COMMENT '附件',
`coin` INT(11),
`diamond` INT(11),
`add_time` TIMESTAMP NULL,
`modify_time` TIMESTAMP NULL,
`currency_opt_type` INT(11) NOT NULL DEFAULT 1091,
`serial_id` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '序列号',
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 是否发送过系统邮件的记录表
CREATE TABLE `role_redeem_info` (
`role_id` BIGINT(20) UNIQUE NOT NULL,
`redeem_time` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 货币流
CREATE TABLE `money_flow_log` (
`id` BIGINT(20) UNIQUE NOT NULL,
`pid` VARCHAR(64) COMMENT '平台',
`area_id` INT(11) NOT NULL DEFAULT 0 COMMENT '区id',
`world_id` INT(11) NOT NULL DEFAULT 0 COMMENT 'world_id',
`user_id` VARCHAR(1024) COMMENT '平台',
`role_id` BIGINT(20),
`currency_type` INT(11) NOT NULL DEFAULT 0 COMMENT '货币类型',
`use_type` INT(11) NOT NULL DEFAULT 0 COMMENT '增加或者消耗',
`before_value` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '变化前数量',
`value` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '变更数量',
`after_value` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '变化后数量',
`operate_type` INT(11) NOT NULL DEFAULT 0 COMMENT '操作类型',
`add_time` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00' COMMENT '创建时间',
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 版本公告
CREATE TABLE `version_notice` (
`id` INT(11) UNIQUE NOT NULL COMMENT '公告类型 1=版本 2=官方',
`content` VARCHAR(1024) COMMENT '内容',
`version` VARCHAR(64) COMMENT '版本号',
`add_time` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00' COMMENT '创建时间',
`modify_time` TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00' COMMENT '修改时间',
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;