-- Add concurrency-safe business code sequence allocation.
CREATE TABLE IF NOT EXISTS `code_sequence` (
  `sequence_key` varchar(128) NOT NULL,
  `current_value` int(11) NOT NULL DEFAULT '0',
  `description` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sequence_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
