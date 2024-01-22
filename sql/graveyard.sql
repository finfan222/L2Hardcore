-- ----------------------------
-- Table structure for graveyard
-- ----------------------------
DROP TABLE IF EXISTS `graveyard`;
CREATE TABLE `graveyard`  (
  `name` varchar(16) CHARACTER NOT NULL,
  `message` varchar(512) CHARACTER DEFAULT NULL,
  `reason` varchar(16) CHARACTER NULL,
  `x` int(10) NOT NULL,
  `y` int(10) NOT NULL,
  `z` int(10) NOT NULL,
  `heading` int(10) NOT NULL,
  `date` date NOT NULL DEFAULT CURRENT_DATE,
  `is_eternal` tinyint(1) NOT NULL DEFAULT FALSE,
  PRIMARY KEY (`name`) USING BTREE
) ENGINE = InnoDB;
