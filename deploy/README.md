# 部署说明

当前仓库按以下生产部署模型准备：

- 前端：React 构建后的静态文件
- 后端：Spring Boot `jar`
- 反向代理：Nginx
- 进程托管：systemd
- 数据库：MySQL

## 推荐目录

```text
/opt/salesmanagement/
├── frontend/
│   └── build/
├── backend/
│   ├── spring-boot.jar
│   └── backend.env
└── uploads/
```

## 部署步骤

### 1. 初始化数据库

按以下顺序执行根目录脚本：

1. `database/00_current_schema.sql`
2. `database/01_current_seed_data.sql`
3. `database/02_current_views.sql`

### 2. 构建后端

在本地或服务器执行：

```bash
cd salesmanagement
./gradlew bootJar
```

生成产物：

- `salesmanagement/build/libs/spring-boot.jar`

### 3. 构建前端

在本地或服务器执行：

```bash
cd frontend
npm ci
npm run build
```

生成产物：

- `frontend/build`

### 4. 上传文件到服务器

上传以下内容：

- `salesmanagement/build/libs/spring-boot.jar` -> `/opt/salesmanagement/backend/spring-boot.jar`
- `frontend/build/` -> `/opt/salesmanagement/frontend/build/`
- `deploy/nginx/salesmanagement.conf`
- `deploy/systemd/salesmanagement-backend.service`
- `deploy/env/backend.env.example`

### 5. 准备后端环境变量

复制：

- `deploy/env/backend.env.example`

到：

- `/opt/salesmanagement/backend/backend.env`

并修改数据库连接、端口和附件目录。

### 6. 部署 systemd 服务

复制：

- `deploy/systemd/salesmanagement-backend.service`

到：

- `/etc/systemd/system/salesmanagement-backend.service`

然后执行：

```bash
sudo systemctl daemon-reload
sudo systemctl enable salesmanagement-backend
sudo systemctl start salesmanagement-backend
sudo systemctl status salesmanagement-backend
```

### 7. 部署 Nginx

复制：

- `deploy/nginx/salesmanagement.conf`

到：

- `/etc/nginx/conf.d/salesmanagement.conf`

或：

- `/etc/nginx/sites-available/salesmanagement.conf`

检查并重载：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 部署后检查

### 后端检查

```bash
curl -i http://127.0.0.1:8081/api/auth/me
```

未登录时返回 `401` 属于正常。

### 前端检查

浏览器访问：

- `http://your-domain/`

检查：

1. 登录页是否能打开
2. 刷新任意业务路由是否正常回到前端页面
3. 登录后是否能正常请求 `/api`

## 当前配套文件

- Nginx 配置：
  - `deploy/nginx/salesmanagement.conf`
- systemd 服务：
  - `deploy/systemd/salesmanagement-backend.service`
- 后端环境变量示例：
  - `deploy/env/backend.env.example`
- 前端环境变量示例：
  - `frontend/.env.production.example`

