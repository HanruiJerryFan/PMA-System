-- Incremental patch script for V1.2.3 warehouse document print config
-- Seeds default company name used on printed / exported warehouse documents (Aisino APD-E05 layout)

INSERT INTO `system_config` (`config_key`, `config_value`, `description`, `created_at`, `updated_at`)
SELECT 'warehouse.document.company_name', '敦力（南京）科技有限公司', 'Default company name printed on warehouse document footers', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'warehouse.document.company_name');
