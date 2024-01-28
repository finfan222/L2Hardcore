/*
 Navicat Premium Data Transfer

 Source Server         : MariaDB
 Source Server Type    : MariaDB
 Source Server Version : 101105
 Source Host           : localhost:3306
 Source Schema         : acis

 Target Server Type    : MariaDB
 Target Server Version : 101105
 File Encoding         : 65001

 Date: 28/01/2024 14:59:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for items
-- ----------------------------
DROP TABLE IF EXISTS `items`;
CREATE TABLE `items`  (
  `owner_id` int(10) UNSIGNED DEFAULT 0,
  `object_id` int(10) UNSIGNED NOT NULL,
  `item_id` int(10) UNSIGNED NOT NULL,
  `count` int(10) UNSIGNED NOT NULL,
  `enchant_level` int(10) UNSIGNED NOT NULL DEFAULT 0,
  `location` varchar(255) NOT NULL,
  `slot` int(10) UNSIGNED NOT NULL DEFAULT 0,
  `custom_type1` tinyint UNSIGNED NOT NULL DEFAULT 0,
  `custom_type2` tinyint UNSIGNED NOT NULL DEFAULT 0,
  `durability` int(5) NOT NULL DEFAULT -1,
  `time` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`owner_id`, `object_id`) USING BTREE,
  UNIQUE INDEX `owner_id`(`owner_id`, `object_id`, `item_id`) USING BTREE,
  INDEX `object_id`(`object_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for item_augmentations
-- ----------------------------
DROP TABLE IF EXISTS `item_augmentations`;
CREATE TABLE `item_augmentations`  (
  `object_id` int(10) UNSIGNED NOT NULL,
  `attributes` int(10) UNSIGNED NOT NULL,
  `skill_id` int(10) NOT NULL DEFAULT -1,
  `skill_level` int(10) NOT NULL DEFAULT -1,
  PRIMARY KEY (`object_id`) USING BTREE,
  CONSTRAINT `item_augmentations_ibfk_1` FOREIGN KEY (`object_id`) REFERENCES `items` (`object_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for item_pets
-- ----------------------------
DROP TABLE IF EXISTS `item_pets`;
CREATE TABLE `item_pets`  (
  `object_id` int(10) UNSIGNED NOT NULL,
  `name` varchar(16),
  `level` int(10) UNSIGNED NOT NULL DEFAULT 1,
  `current_hp` decimal UNSIGNED NOT NULL,
  `current_mp` decimal UNSIGNED NOT NULL,
  `exp` bigint(20) UNSIGNED NOT NULL DEFAULT 0,
  `sp` bigint(20) UNSIGNED NOT NULL DEFAULT 0,
  `hunger` int(10) UNSIGNED NOT NULL,
  PRIMARY KEY (`object_id`) USING BTREE,
  CONSTRAINT `item_pets_ibfk_1` FOREIGN KEY (`object_id`) REFERENCES `items` (`object_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
