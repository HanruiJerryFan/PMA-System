# 销售管理系统前端部署说明

## 目录说明

- 源码目录：`frontend/src`
- 静态资源目录：`frontend/public`
- 生产构建输出：`frontend/build`

## 技术栈

- React 18
- Ant Design 5
- React Router
- Axios

## 环境要求

- Node.js 18 或更高版本
- npm 9 或更高版本

## 本地安装与构建

### 安装依赖

```bash
npm ci
```

### 开发模式

```bash
npm start
```

默认开发地址：

- `http://localhost:3000`

### 生产构建

```bash
npm run build
```

构建完成后，静态文件输出到：

- `frontend/build`

## API 基址规则

前端 API 基址逻辑在：

- `frontend/src/api/baseUrl.js`

当前规则如下：

1. 如果配置了 `REACT_APP_API_BASE_URL`，优先使用该值
2. 如果未配置：
   - 浏览器运行在 `localhost/127.0.0.1` 时，默认请求 `http://localhost:8081/api`
   - 非本机访问时，默认请求当前站点同源的 `/api`

### 推荐配置

#### 同域部署

前端站点和后端通过同一个域名暴露，推荐不设置 `REACT_APP_API_BASE_URL`，由反向代理把 `/api` 转发到后端。

#### 分域部署

如果前端和后端使用不同域名，构建前设置：

```bash
REACT_APP_API_BASE_URL=https://your-backend.example.com
```

Windows PowerShell 示例：

```powershell
$env:REACT_APP_API_BASE_URL='https://your-backend.example.com'
cmd /c npm run build
```

## 部署建议

### 推荐方式

推荐使用 Nginx、OpenResty、Apache 或其他静态文件服务器托管 `frontend/build`。

需要保证两件事：

1. React Router 刷新时回落到 `index.html`
2. `/api` 请求能正确转发到后端服务

### Nginx 示例

```nginx
server {
    listen 80;
    server_name your-domain.example.com;

    root /opt/salesmanagement/frontend/build;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8081/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri /index.html;
    }
}
```

## 权限与会话说明

- 前端使用基于 Session 的登录态
- 所有 API 请求默认带 `withCredentials`
- 后端返回 `401` 时会跳转登录页
- 后端返回 `403` 时会跳转无权限页
- 如果后端要求强制改密，会跳转到修改密码页

## 业务上传说明

当前业务页已支持就地上传，附件中心主要用于：

- 全局检索附件
- 下载和删除附件
- 维护附件元数据

业务上下文进入附件中心时，默认不再作为主上传入口。

## 生产发布前检查

1. 确认后端已启动且 `/api/auth/me` 可访问
2. 确认 `/api` 反向代理已配置
3. 确认浏览器访问页面后，刷新任意业务路由不会返回 404
4. 确认登录、无权限跳转、强制改密跳转正常
5. 确认附件上传与下载路径可用

## 配套部署资产

仓库内已提供部署模板：

- `../deploy/README.md`
- `../deploy/nginx/salesmanagement.conf`
- `./.env.production.example`

## 常见问题

### 页面刷新后 404

静态服务器没有把前端路由回落到 `index.html`。

### 出现 `Network error or backend unavailable`

通常有以下几种原因：

- 后端没有启动
- `/api` 反向代理未配置
- `REACT_APP_API_BASE_URL` 配置错误
- 浏览器访问的前端地址和后端 API 地址不通

### 登录成功但页面仍提示无权限

通常是用户权限不足，或者权限更新后还没有重新登录。
