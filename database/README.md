# Database Scripts

当前 `database/` 根目录只保留基于现有生产化库结构生成的基线脚本。

## 根目录脚本

- `00_current_schema.sql`
  - 当前数据库的表结构基线
  - 来源：当前 `salesmanagement` 库的真实表结构导出
  - 不包含视图

- `01_current_seed_data.sql`
  - 当前数据库保留的字典、配置、权限、默认用户与默认仓库种子数据

- `02_current_views.sql`
  - 当前数据库使用中的标准命名视图

- `03_production_data_cleanup.sql`
  - 生产清库脚本
  - 清空业务数据和日志，只保留字典、配置、权限、`System` 用户和默认仓库

- `04_auto_increment_repair.sql`
  - 自增修复脚本
  - 在清库后把业务表和保留表的自增值修正到合理范围

- `05_code_sequence.sql`
  - 现有数据库补丁脚本
  - 增加并发安全的业务编号序列表 `code_sequence`

## 推荐使用顺序

### 初始化空库

1. 执行 `00_current_schema.sql`
2. 执行 `01_current_seed_data.sql`
3. 执行 `02_current_views.sql`

### 生产数据清理

1. 执行 `03_production_data_cleanup.sql`
2. 执行 `04_auto_increment_repair.sql`

### 现有库增量补丁

1. 执行 `05_code_sequence.sql`

## 历史脚本

旧的分模块建表、迁移、删表脚本已经统一归档到 `database/legacy/`。
这些脚本仅用于历史追溯，不再作为当前建库基线。
