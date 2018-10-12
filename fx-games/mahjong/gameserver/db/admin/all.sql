-- 用户表
DROP TABLE IF EXISTS user;
CREATE TABLE user(
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(32) NOT NULL,
  nick_name VARCHAR(32) NOT NULL, 
  password VARCHAR(64) NOT NULL,
  email VARCHAR(50),
  last_pid VARCHAR(32) NOT NULL,
  last_area_id INT(11) NOT NULL,
  role_id INT(11) NOT NULL,
  privileges TEXT NOT NULL,
  login_failed INT(11) DEFAULT 0,
  add_time TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
  modify_time TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
  last_login_ip varchar(50) not null default '127.0.0.1',
  UNIQUE INDEX IDX_NE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

-- 角色表
DROP TABLE IF EXISTS role;
CREATE TABLE role(
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(32) NOT NULL,
  description VARCHAR(64) DEFAULT NULL,
  privileges VARCHAR(1024)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

-- 平台表
DROP TABLE IF EXISTS platform;
CREATE TABLE platform(
  id VARCHAR(32) PRIMARY KEY,
  name VARCHAR(32) NOT NULL,
  description VARCHAR(64) DEFAULT NULL,
  add_time TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
  modify_time TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
  `exchange_rate` float(11,4) NOT NULL DEFAULT '1.0000' COMMENT '汇率',
  UNIQUE INDEX IDX_NE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

-- 用户表游戏区表的中间表
DROP TABLE IF EXISTS user_platform;
CREATE TABLE user_platform(
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  pid VARCHAR(32) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

-- 区服表
DROP TABLE IF EXISTS `game_area`;
CREATE TABLE `game_area` (
  `world_id` int(11) NOT NULL,
  `area_id` int(11),
  `world_name` varchar(255) NOT NULL,
  `area_name` varchar(255) NOT NULL,
  `area_type` int(11) NOT NULL,
  `external_ip` varchar(255) NOT NULL,
  `tcp_port` int(11) NOT NULL,
  `ip` varchar(255) NOT NULL,
  `port` int(11) NOT NULL,
  `pid` VARCHAR(32) NOT NULL,
  `status` int(11) NOT NULL,
  `follower_id` int(11) NOT NULL,
  `add_time` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
  `combine_time` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
  `restart_time` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
  PRIMARY KEY (`world_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 管理员操作记录
DROP TABLE IF EXISTS operation_log;
CREATE TABLE operation_log(
  id INT(11) NOT NULL AUTO_INCREMENT,
  type INT NOT NULL, 
  user_id INT NOT NULL,
  user_name VARCHAR(32) NOT NULL, 
  fun VARCHAR(128) NOT NULL, -- 动作描述
  `value` text NOT NULL, -- 动作内容
  add_time TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
  last_login_ip varchar(50) not null default '127.0.0.1',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;


INSERT INTO user VALUES (1, 'admin','超级管理员', 'c3284d0f94606de1fd2af172aba15bf3', 'admin@lingyuwangluo.com',0,1,1,'[10001,10002,10003,10004,10005,10006,10007,10008,20001,20002,30001,30002,30003,30004,30005,40001,40002,40003,40004,40005,50001,60001,60002,60003,60004,60005,60006,60007,70001,70002,70003,70004,70005,70006,70007,70008,70009,70010,70011,80001,80003,80004,80005,80006,80007]',0,'2011-01-01 11:11:00', '2011-01-01 11:11:00','127.0.0.1');
INSERT INTO platform VALUES (0, 'DEV','desc', '2011-01-01 11:11:00', '2011-01-01 11:11:00', 1.0);
INSERT INTO user_platform VALUES (1, 1, 0);
INSERT INTO role VALUES (1, '超级管理员权限', '', '[10001,10002,10003,10004,10005,20001,20002,30001,30002,30003,30004,30005,40001,40002,50001,60001,60002,60003,60004,60005,60006,60007,70001,70002,70003,70004,70005,70006,70007,70008,70009,80001,80002,80003,80004,80005,80006
]');


-- 公告
DROP TABLE IF EXISTS `announce`;
CREATE TABLE `announce` (
  `id` int(11) AUTO_INCREMENT,
  `content` text DEFAULT NULL,
  `interval_gap` int(11) NOT NULL,
  `begin_time` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
  `end_time` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
  `area_ids` varchar(255) NOT NULL,
  `pid` VARCHAR(32) DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `is_exists` tinyint(1) NOT NULL DEFAULT 1,
  `pf` varchar(255) NOT NULL DEFAULT 'all' comment '支持的平台',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 公告模板
DROP TABLE IF EXISTS announce_template;
CREATE TABLE `announce_template` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `contet` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 邮件模板
DROP TABLE IF EXISTS `mail_template`;
CREATE TABLE `mail_template` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `add_time` datetime DEFAULT NULL,
  `content` varchar(255) DEFAULT NULL,
  `modify_time` datetime DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 邮件补偿记录
DROP TABLE IF EXISTS `redeem_mail_record`;
CREATE TABLE `redeem_mail_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `add_time` datetime DEFAULT NULL,
  `admin_id` int(11) DEFAULT NULL,
  `admin_name` varchar(255) DEFAULT NULL,
  `all_area` tinyint(1) DEFAULT NULL,
  `area_id_list` varchar(255) DEFAULT NULL,
  `areas` varchar(255) DEFAULT NULL,
  `coin` bigint(20) DEFAULT NULL,
  `diamond` int(11) DEFAULT NULL,
  `ip` varchar(255) DEFAULT NULL,
  `is_all` tinyint(1) DEFAULT NULL,
  `players` varchar(255) DEFAULT NULL,
  `redeem_msg` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 邮件补偿
DROP TABLE IF EXISTS `redeem_record`;
CREATE TABLE `redeem_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `add_time` datetime DEFAULT NULL,
  `admin_id` int(11) DEFAULT NULL,
  `admin_name` varchar(255) DEFAULT NULL,
  `all_area` tinyint(1) DEFAULT NULL,
  `area_id_list` varchar(255) DEFAULT NULL,
  `areas` varchar(255) DEFAULT NULL,
  `check_admin_id` int(11) DEFAULT NULL,
  `check_admin_name` varchar(255) DEFAULT NULL,
  `check_time` datetime DEFAULT NULL,
  `coin` bigint(20) DEFAULT NULL,
  `diamond` int(11) DEFAULT NULL,
  `ip` varchar(255) DEFAULT NULL,
  `is_all` tinyint(1) DEFAULT NULL,
  `items` varchar(255) DEFAULT NULL,
  `players` varchar(255) DEFAULT NULL,
  `redeem_msg` varchar(255) DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 在线人数
DROP TABLE IF EXISTS stat_online_num;
CREATE TABLE stat_online_num(
  `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  pid VARCHAR(32) NOT NULL,
  area_id INT NOT NULL,
  num INT(11) NOT NULL,
  add_time TIMESTAMP NOT NULL DEFAULT '2000-01-01 00:00:00',
  PRIMARY KEY (`id`),
  KEY `INDEX_PD_AD_NM_AE` (`pid`,area_id,num,add_time) USING BTREE,
  KEY `INDEX_PD_AD_AE` (`pid`,area_id,add_time) USING BTREE,
  KEY `INDEX_AD` (area_id) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;