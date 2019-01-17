-- --------------------------------------------------------
-- Host:                         otula.pori.tut.fi
-- Server version:               5.5.50-0+deb8u1 - (Debian)
-- Server OS:                    debian-linux-gnu
-- HeidiSQL Version:             9.3.0.4984
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

-- Dumping database structure for ca_frontend
CREATE DATABASE IF NOT EXISTS `ca_frontend` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `ca_frontend`;


-- Dumping structure for table ca_frontend.uh_alerts
CREATE TABLE IF NOT EXISTS `uh_alerts` (
  `alert_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `status` int(11) NOT NULL,
  `type` int(11) NOT NULL,
  `gauge_value_id` bigint(20) NOT NULL,
  `id` varchar(50) NOT NULL,
  PRIMARY KEY (`alert_id`),
  KEY `index_tag_id` (`id`),
  KEY `index_gauge_value_id` (`gauge_value_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- Data exporting was unselected.


-- Dumping structure for table ca_frontend.uh_gauges
CREATE TABLE IF NOT EXISTS `uh_gauges` (
  `gauge_id` varchar(50) NOT NULL,
  `meter_id` bigint(20) NOT NULL,
  `gauge_index` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text NOT NULL,
  `data_type` varchar(50) NOT NULL,
  `options` varchar(50) NOT NULL,
  `unit` varchar(50) DEFAULT NULL,
  `min` double DEFAULT NULL,
  `max` double DEFAULT NULL,
  `min_increase` double DEFAULT NULL,
  `max_increase` double DEFAULT NULL,
  `cumulative` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`gauge_id`),
  KEY `meter_id` (`meter_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- Data exporting was unselected.


-- Dumping structure for table ca_frontend.uh_gauge_values
CREATE TABLE IF NOT EXISTS `uh_gauge_values` (
  `gauge_value_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `gauge_id` varchar(50) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `row_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`gauge_value_id`),
  UNIQUE KEY `unique` (`gauge_id`,`row_created`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- Data exporting was unselected.


-- Dumping structure for table ca_frontend.uh_locations
CREATE TABLE IF NOT EXISTS `uh_locations` (
  `location_id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `floor_plan_url` varchar(2000) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`location_id`),
  KEY `index_USER_ID` (`user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- Data exporting was unselected.


-- Dumping structure for table ca_frontend.uh_meters
CREATE TABLE IF NOT EXISTS `uh_meters` (
  `meter_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` varchar(50) NOT NULL,
  `name` varchar(255) NOT NULL,
  `location_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) NOT NULL,
  `location_x` double DEFAULT NULL,
  `location_y` double DEFAULT NULL,
  PRIMARY KEY (`meter_id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `user_id` (`user_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COMMENT='location column can be ignored for now (it should determine where the meter actually is or something, we''ll skip that in this demo)';

-- Data exporting was unselected.
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
