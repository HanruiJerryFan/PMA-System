# 销售管理系统后端部署说明

## 目录说明

- 源码目录：`salesmanagement/src`
- MyBatis XML：`salesmanagement/src/main/resources/mapper`
- 默认配置文件：`salesmanagement/src/main/resources/application.properties`
- 构建产物目录：`salesmanagement/build/libs`

## 技术栈

- Java 17
- Spring Boot 3.3
- Spring Security
- MyBatis
- MySQL
- Gradle

## 环境要求

- JDK 17
- MySQL 5.7 或更高版本
- 可写附件目录

## 数据库初始化

数据库基线脚本位于项目根目录：

- `database/00_current_schema.sql`
- `database/01_current_seed_data.sql`
- `database/02_current_views.sql`

初始化顺序：

1. 执行 `00_current_schema.sql`
2. 执行 `01_current_seed_data.sql`
3. 执行 `02_current_views.sql`

如果是生产清库后重新校准自增：

1. 执行 `database/03_production_data_cleanup.sql`
2. 执行 `database/04_auto_increment_repair.sql`

补充说明：

- 当前数据库脚本已经以“现有生产化数据库结构”为准
- 旧迁移脚本已归档到 `database/legacy`

## 默认端口与配置

当前默认配置在：

- `salesmanagement/src/main/resources/application.properties`

默认值：

- `server.port=8081`
- `server.address=localhost`
- `spring.datasource.url=jdbc:mysql://localhost:3306/salesmanagement`
- `app.attachment.storage-root=uploads`

## 生产环境推荐做法

不要直接使用仓库内的本地默认配置。

建议通过启动参数或外部配置覆盖以下项：

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `server.address`
- `server.port`
- `app.attachment.storage-root`

### 启动示例

```bash
java -jar spring-boot.jar \
  --server.address=0.0.0.0 \
  --server.port=8081 \
  --spring.datasource.url=jdbc:mysql://127.0.0.1:3306/salesmanagement?useUnicode=true&characterEncoding=utf8 \
  --spring.datasource.username=your_db_user \
  --spring.datasource.password=your_db_password \
  --app.attachment.storage-root=/data/salesmanagement/uploads
```

Windows PowerShell 示例：

```powershell
java -jar .\build\libs\spring-boot.jar `
  --server.address=0.0.0.0 `
  --server.port=8081 `
  --spring.datasource.url=jdbc:mysql://127.0.0.1:3306/salesmanagement?useUnicode=true&characterEncoding=utf8 `
  --spring.datasource.username=your_db_user `
  --spring.datasource.password=your_db_password `
  --app.attachment.storage-root=D:\data\salesmanagement\uploads
```

## 构建

### 编译

```bash
./gradlew.bat compileJava
```

### 打包

```bash
./gradlew.bat bootJar
```

打包后产物：

- `salesmanagement/build/libs/spring-boot.jar`

## 运行方式

### 本地运行

```bash
./gradlew.bat bootRun
```

### 生产运行

```bash
java -jar build/libs/spring-boot.jar
```

建议使用：

- Windows 服务
- NSSM
- systemd
- supervisor

中的一种托管后端进程。

## 附件目录

后端附件上传会写入：

- `app.attachment.storage-root`

请确保：

1. 目录存在或可自动创建
2. 运行用户对该目录有读写权限
3. 部署清理时不要误删该目录中的有效附件

## 权限与账号说明

- 当前生产保留账号：`System`
- `System` 用户不写审计日志，也不写登录日志
- 权限、角色、字典、系统配置会保留在种子数据中

## 生产发布前检查

1. 数据库已按 `00 -> 01 -> 02` 初始化
2. 后端能正常连接数据库
3. `uploads` 目录可写
4. `GET /api/auth/me` 未登录返回 `401`
5. 前端静态站点能通过 `/api` 正常访问后端
6. 登录、权限控制、附件上传、打印/导出链路可用

## 配套部署资产

仓库内已补充 Linux 服务器部署模板：

- `../deploy/README.md`
- `../deploy/nginx/salesmanagement.conf`
- `../deploy/systemd/salesmanagement-backend.service`
- `../deploy/env/backend.env.example`

## 常见问题

### 数据库脚本很多，不知道该执行哪个

只执行根目录这 5 个基线脚本，不再使用 `database/legacy` 里的历史迁移脚本。

### 前端报 `Network error or backend unavailable`

通常是：

- 后端未启动
- 反向代理未配置
- API 基址配置错误

### 附件上传失败

通常是：

- `app.attachment.storage-root` 不可写
- 部署账号没有目录权限
- 磁盘空间不足
