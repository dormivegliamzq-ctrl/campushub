-- =============================================================
-- CampusHub 校园论坛社区 · 初始化脚本
-- 执行方式：DataGrip / 命令行 直接整文件执行（可重复执行）
-- 设计要点（面试讲解素材）：
--   1. uk_post_user 唯一索引是"点赞防重"的数据库最后防线
--   2. idx_post_id_deleted 联合索引配合软删除查询（deleted=0 恒在条件里）
--   3. idx_timeline (create_time, id) 支撑帖子时间线分页
-- =============================================================

CREATE DATABASE IF NOT EXISTS campushub
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE campushub;

-- -------------------------------------------------------------
-- 1. 用户表
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username    VARCHAR(32)   NOT NULL COMMENT '用户名（登录账号）',
  password    VARCHAR(100)  NOT NULL COMMENT '密码（BCrypt 密文，禁止明文）',
  nickname    VARCHAR(32)   DEFAULT NULL COMMENT '昵称',
  avatar      VARCHAR(255)  DEFAULT NULL COMMENT '头像URL',
  bio         VARCHAR(255)  DEFAULT NULL COMMENT '个性签名',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username) COMMENT '用户名唯一，注册防重'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- -------------------------------------------------------------
-- 2. 帖子表（deleted 为软删除标记，MP @TableLogic 自动附加 deleted=0）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS post;
CREATE TABLE post (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  user_id       BIGINT       NOT NULL COMMENT '作者用户ID',
  title         VARCHAR(64)  NOT NULL COMMENT '标题',
  content       TEXT         NOT NULL COMMENT '正文内容',
  view_count    INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  like_count    INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
  comment_count INT          NOT NULL DEFAULT 0 COMMENT '评论数',
  deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除：0正常 1已删除',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id) COMMENT '按作者查帖子',
  KEY idx_timeline (create_time, id) COMMENT '★时间线分页：按创建时间倒序+ID倒序，避免深分页回表'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '帖子表';

-- -------------------------------------------------------------
-- 3. 评论表（两级：parent_id=0 为一级评论，否则为对某评论的回复）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS comment;
CREATE TABLE comment (
  id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  post_id          BIGINT        NOT NULL COMMENT '所属帖子ID',
  user_id          BIGINT        NOT NULL COMMENT '评论人用户ID',
  parent_id        BIGINT        NOT NULL DEFAULT 0 COMMENT '父评论ID：0=一级评论，否则=回复的评论ID',
  reply_to_user_id BIGINT        DEFAULT NULL COMMENT '被回复的用户ID（仅回复时填）',
  content          VARCHAR(1000) NOT NULL COMMENT '评论内容',
  deleted          TINYINT       NOT NULL DEFAULT 0 COMMENT '软删除：0正常 1已删除',
  create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_post_id_deleted (post_id, deleted) COMMENT '★按帖子查评论：软删除查询恒带 deleted=0，联合索引避免回表'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评论表';

-- -------------------------------------------------------------
-- 4. 点赞表（一人一帖一条记录，防重靠唯一索引）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS post_like;
CREATE TABLE post_like (
  id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  post_id     BIGINT   NOT NULL COMMENT '帖子ID',
  user_id     BIGINT   NOT NULL COMMENT '点赞用户ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_post_user (post_id, user_id) COMMENT '★防重复点赞的数据库最后防线',
  KEY idx_user_id (user_id) COMMENT '查"我点赞过的帖子"'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '点赞表';

-- -------------------------------------------------------------
-- 5. 关注表（follower 关注 followee）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS follow;
CREATE TABLE follow (
  id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  follower_id BIGINT   NOT NULL COMMENT '粉丝（发起关注的人）',
  followee_id BIGINT   NOT NULL COMMENT '被关注的人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_follower_followee (follower_id, followee_id) COMMENT '★防重复关注',
  KEY idx_followee_id (followee_id) COMMENT '查粉丝列表'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '关注表';
