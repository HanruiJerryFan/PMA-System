# 开发进度记录（压缩版）

## 历史批次压缩总结

### 阶段 A：基础业务骨架与首轮联调
- 完成客户、项目、合同、物料、库存、财务凭证、附件中心等基础 CRUD。
- 建立前后端主链路、基础数据库结构与首轮联调能力。
- 完成日志页、导出能力、项目状态历史、合同条款、仓库单据等首轮功能覆盖。

### 阶段 B：标准命名与需求收口
- 完成后端标准命名重构，旧 `*Info / *BasicInfo / *DynamicInfo` 兼容层逐步清理。
- 控制器标准路由、标准 Service、标准 Mapper/XML、标准 POJO 成为唯一主链路。
- 按 `requirements.md` 第 5 至第 12 章逐条补齐数据库、后端、前端差异。
- 完成客户系统 ID、客户类型数据库编码、项目状态阶段、项目清单、项目状态履历、物料编码和财务凭证等关键规则落地。

### 阶段 C：权限体系与产品化前端
- 建立用户、角色、权限项、用户角色分配、角色权限分配完整链路。
- 完成 `access/manage` 权限拆分、按钮级权限控制、无权限页、菜单显隐和路由保护。
- 前端完成大规模中文化、乱码清理、侧边栏重构、公共 CRUD 交互统一、无权限和改密流程优化。
- 首页、项目、合同、物料、库存、财务、日志、附件中心等页面全部收口为可用业务页。

### 阶段 D：项目、库存、财务的深度业务重构
- 项目台账改为轻量列表，项目总览改为 dashboard。
- 新增项目清单 dashboard、项目清单最终汇总清单、项目阶段推进与删除锁定规则。
- 库存模块重构为库存流水聚合 + 单仓模式出入库单头/明细模型。
- 财务凭证补齐项目强关联、客户引用、票据附件、开票字段、差额类型、统计卡片、全列筛选与搜索。

### 阶段 E：生产化收尾
- 清理测试账号，仅保留 `System`。
- 清理数据库业务数据、修复自增、补默认仓库。
- 下线无业务依赖或已被统一模型替代的表与链路。
- 清理工作区中非部署需要的缓存、日志、临时文件。
- 重写前后端部署 README。
- 清理前端历史 ESLint warning，移除无用导入、无用变量和 `ProjectInfo.js` 的 BOM。
- 补齐 Linux 服务器部署模板（Nginx、systemd、环境变量示例、统一部署说明）。
- 客户编码前 4 位改为 `region.area_code` 前 4 位，并新增 `code_sequence` 序列表处理自动编号并发分配。

## 最近关键收口

### 数据库与脚本
- `database/` 根目录已重建为当前数据库基线：
  - `00_current_schema.sql`
  - `01_current_seed_data.sql`
  - `02_current_views.sql`
  - `03_production_data_cleanup.sql`
  - `04_auto_increment_repair.sql`
  - `05_code_sequence.sql`
  - `README.md`
- 旧建表、迁移、删表脚本已归档到 `database/legacy/`。
- 已实际清理并下线：
  - `domain_access_time`
  - `finance_voucher_attachment`
  - `goods_inventory`
  - `contract_status`

### 账号与日志
- `System` 用户保留。
- `System` 用户已从审计日志和登录日志链路中排除。
- 当前生产保留的权限、角色、字典、系统配置已固化到种子数据。
- 自动编号已改为通过 `code_sequence` 行锁分配，覆盖客户编码、物料编码、财务凭证号、出入库单号。

### 部署文档与目录收尾
- 新增后端部署文档：
  - `salesmanagement/README_BACKEND.md`
- 重写前端部署文档：
  - `frontend/README_FRONTEND.md`
- 已清理部署无关内容：
  - 根目录缓存、临时导入文件、cookie、开发目录
  - 后端临时日志与缓存目录
  - 前端 `node_modules`

### 附件模型与业务页上传
- 明确通用附件模型为 `attachment.business_type + business_uuid` 的 1-N 关系，不再按业务对象限制单附件。
- 合同、财务凭证、项目清单编辑时已提示已有附件数量和文件名，并提供跳转附件中心入口。
- 合同、财务凭证、项目清单、项目总览快速新增清单的业务内上传入口已统一为可复用组件，支持一次选择多个文件并逐个上传。
- 项目清单继续保留 `pdfAttachmentId` 作为主 PDF 附件指针；上传多文件时第一份新文件作为主附件，其余文件保留在通用附件中心。

## V1.0 总结

### 1. 数据库基线收口
- 已按当前真实数据库结构重建基线脚本，不再以历史迁移链作为初始化入口。
- `database/` 根目录当前仅保留：
  - `00_current_schema.sql`
  - `01_current_seed_data.sql`
  - `02_current_views.sql`
  - `03_production_data_cleanup.sql`
  - `04_auto_increment_repair.sql`
  - `05_code_sequence.sql`
  - `README.md`
- 旧的建表、迁移、删表脚本已统一归档到 `database/legacy/`。
- 已完成生产清库、自增修复、默认仓库保留、`System` 用户保留、权限和字典保留。
- 已从当前数据库与脚本体系中下线无业务依赖或已被统一模型替代的表：
  - `domain_access_time`
  - `finance_voucher_attachment`
  - `goods_inventory`
  - `contract_status`

### 2. 标准命名与后端主链路重构
- 完成客户、项目、合同、物料、库存、财务、附件等核心链路的标准命名收口。
- 旧的 `*Info / *BasicInfo / *DynamicInfo` 兼容层、旧别名路由、旧 Mapper/XML/POJO 已清理。
- 后端主实现层已统一到标准 Service + 标准 Mapper/XML。
- 标准 API 路由成为唯一入口，旧别名路由已移除。
- 关键业务对象已重构：
  - 客户系统 ID、客户类型数据库驱动编码
  - 项目、项目状态履历、项目清单、项目清单明细
  - 物料主数据与 12 位物料编码规则
  - 库存流水、库存汇总、单仓模式出入库单
  - 财务凭证、票据附件、客户引用与快照字段

### 3. requirements.md 对齐完成情况
- 第 5 章至第 12 章主体需求已按当前产品范围完成落地或按新系统范围收口。
- 已补齐的重点包括：
  - `sys_user.status`
  - `customer_info` 必填字段、行业字典化、活跃状态规则
  - `customer_contact` 扩展字段
  - `project_type`、`stage`、`project_status_history`
  - `project_list / project_list_item`
  - `finance_voucher` 编号、税率、开票字段、差额逻辑、项目强关联
  - `material_*` 字典与主数据模型
  - `inventory` 库存流水、库存汇总、出入库单头明细模型
  - 导入导出、打印、校验规则、非功能配置
- 对需求的有意偏离主要只保留：
  - 地区仍采用 `region` 单表模型，而不是 `province/city` 独立物理表
  - 客户编码前 4 位按 `region.area_code` 前 4 位生成；4 位前缀反查地区时优先返回市级节点

### 4. 权限体系与安全加固
- 完成用户、角色、权限项、用户角色分配、角色权限分配全链路。
- 权限模型已从粗粒度扩展到：
  - `*.access` 页面/查看权限
  - `*.manage` 管理权限
  - 按钮级导入、导出、上传、删除等操作权限控制
- 前端侧边栏、路由和按钮已按权限显隐。
- 无权限访问统一进入无权限页，不再自动跳首页。
- 登录态已支持：
  - 禁用/关闭用户禁止登录
  - 强制改密
  - 管理员重置密码
  - `System` 用户排除审计日志和登录日志

### 5. 前端产品化整理
- 前端主页面已完成中文化，旧乱码和英文遗留已大规模清理。
- 侧边栏已完成：
  - 导航顺序重排
  - 折叠/展开
  - 全部展开/全部折叠
  - 滚动优化
  - 权限感知显示
- CRUD 交互已统一：
  - 操作列前置
  - 编辑自动回填
  - 恢复原值 / 设为默认
  - 查看附件并入操作列
- 多处业务页已重构为更贴近业务的操作体验：
  - 项目台账、项目总览 dashboard、项目清单 dashboard
  - 财务凭证统计卡片、差额提示、全列搜索与筛选
  - 出入库单纸质样式预览、无限明细行、物料联动
  - 附件中心的全局上传与业务上下文查看分离

### 6. 核心业务增强
- 项目：
  - 项目总览 dashboard
  - 状态历史进度条
  - 阶段推进
  - 删除锁定配置
- 项目清单：
  - 清单名称
  - 初始销售 / 销售变更 / 采购清单
  - 最终销售清单 / 最终采购清单虚拟汇总
  - 导出金额计算
  - 附件内联上传
- 库存：
  - 出入库单编号自动生成
  - 业务类别扩展到 `项目销售`
  - 出入库单支持客户引用、显示名称/显示型号、人民币大写金额
- 财务：
  - 对方单位改为客户引用 + 名称快照
  - 凭证号改为月内顺序号
  - 收款/付款方向驱动收入支出
  - 记账金额公式按业务规则自动计算
  - 开票状态、发票号码、发票金额
  - 差额类型支持：`应收 / 应付 / 待开票 / 缺票`

### 7. 日志与附件模型调整
- 已保留并整理：
  - 登录日志
  - 审计日志
- 已移除访问日志页面与采集链路。
- 附件模型已统一回到 `attachment.business_type + business_uuid`，财务凭证附件不再单独维护关系表。
- 附件关系已明确为 1-N：同一业务对象可关联多个附件，数据库层仅对附件 `uuid` 做唯一约束，不对 `business_type + business_uuid` 做唯一约束。
- 业务页普遍改成就地上传，附件中心定位为：
  - 全局检索
  - 下载与删除
  - 元数据维护
- 编辑态已补齐已有附件提示：
  - 合同编辑展示当前合同已有附件
  - 财务凭证编辑展示当前票据附件
  - 项目清单编辑展示当前清单附件
- 业务内上传入口已统一支持多文件选择；项目清单的 `pdfAttachmentId` 仅作为主 PDF 指针，不影响通用 1-N 附件关系。

### 8. 生产环境准备
- 已清理当前目录中明显非部署所需内容：
  - 开发缓存
  - 临时导入文件
  - 调试日志
  - 本地 cookie
  - 前端 `node_modules`
- 已新增和重写部署文档：
  - `salesmanagement/README_BACKEND.md`
  - `frontend/README_FRONTEND.md`
- 已新增部署模板目录：
  - `deploy/README.md`
  - `deploy/nginx/salesmanagement.conf`
  - `deploy/systemd/salesmanagement-backend.service`
  - `deploy/env/backend.env.example`
  - `frontend/.env.production.example`
- 当前项目已具备：
  - 数据库初始化基线
  - 生产清库脚本
  - 自增修复脚本
  - 后端部署说明
  - 前端部署说明

### 9. 当前 V1.0 交付形态
- 当前仓库已收口为可部署版本，保留：
  - 后端源码与构建产物目录
  - 前端源码与构建产物目录
  - 当前数据库基线脚本
  - 部署 README
  - 进度与需求文档
- 当前前端生产构建已再次验证通过。
- 当前仍可能继续演进的事项主要属于后续增强，不属于 V1.0 阻塞项：
  - 更细的规则引擎化配置
  - 更多业务详情页/dashboard
  - 更深层级的审批与审计扩展

---

## V1.1 稳定性加固与架构治理（阶段 F）

对应分支：`fix/hardening-and-consistency`（提交 `42698dc` .. `91dd0a9`）

### 1. 安全加固与鉴权边界修复
- **关闭默认公开自注册并规范管理员重置密码**（`4ce491d`）：
  - 新增 `app.auth.self-registration-enabled`（默认 `false`），`GET /api/auth/config` 暴露开关状态，`POST /api/auth/register` 未开启时返回 `403`。
  - `POST /api/users/{id}/reset-password` 改为由服务端通过 `CurrentUserService` 获取当前管理员 ID，不再信任请求体传入的 `operatorUserId`。
- **集中式 CORS 策略配置**（`53923e2`）：
  - 移除控制器上的硬编码 `@CrossOrigin`，新增 `CorsConfig` 统一管理允许来源（支持 `APP_CORS_ALLOWED_ORIGINS`）。
- **附件路径穿越防护与安全删除顺序**（`358ed88`）：
  - 附件持久化改为相对路径存储，读写前强制路径归一化与根目录越界校验；删除附件时先删存储文件确认成功再删数据库记录，防止孤儿文件残留。

### 2. 跨机部署、反向代理与运维兼容性
- **反向代理 / 跨域 / 遗留绝对路径兼容**（`91dd0a9`）：
  - 启用 `server.forward-headers-strategy=framework` 适配 Nginx HTTPS 转发头。
  - `CorsConfig` 改用 `allowedOriginPatterns` 并默认支持 `http://localhost:3000` 与 `http://127.0.0.1:3000`，同时支持通过环境变量扩展多域名或内网 IP 模式。
  - 附件读取兼容 Windows 盘符遗留路径在 Linux 服务器上的映射定位。
- **Nginx 上传体积对齐**（`7902a90`）：
  - 将 `deploy/nginx/salesmanagement.conf` 的 `client_max_body_size` 与后端 `100MB` 上传限制对齐。
- **数据库视图与种子数据清理**（`dd85515`）：
  - 同步 `database/02_current_views.sql` 与最新表结构，清理种子脚本中的孤立角色权限映射。

### 3. 数据准确性、金额精度与引用完整性
- **全面迁移货币与库存数量类型至 `BigDecimal`**（`514642a`）：
  - 消除 `ProjectListItem`、`WarehouseDocument`、`InventoryTransaction`、`ContractBasicInfo`、`FinanceVoucher`、`ProductHistoryPrice` 等核心对象使用 `Double` 导致的浮点舍入误差。
- **统一财务凭证前端校验与记账公式**（`42698dc`）：
  - 抽取 `frontend/src/utils/financeVoucherCalculations.js`，消除 `FinanceVouchers` 与 `ProjectDashboard` 间的重复计算和校验偏差。
- **主数据删除前强制引用完整性校验**（`99fdb89`）：
  - 新增 `ReferenceCheckMapper`，删除客户、项目、物料、仓库前校验关联业务引用并给出明确中文提示。

### 4. 性能扩展、服务端分页与前端状态收口
- **消除高并发列表 N+1 查询**（`e47d46f`）：
  - 重构 `WarehouseDocumentServiceImpl`、`ProjectServiceImpl`、日志和认证权限加载的批量查询链路（如 `selectByDocNumbers`、`selectCurrentStageSortOrdersByProjectIds`）。
- **向后兼容的服务端分页**（`48bc9c8`）：
  - 为客户、项目、物料、库存流水、登录日志、审计日志新增 `/page` 接口及前端 `CRUDTable` 按需分页支持，保留全量接口向后兼容。
- **集中式前端认证上下文 `AuthContext`**（`9b01a9a`）：
  - 将分散在多个页面与布局中的 `getCurrentUser()` 轮询收敛到 `AuthProvider / useAuth()`，统一处理登录态刷新、401 清理与强制改密流转。

### 5. 阶段 F 验证基线（提交 `91dd0a9`）
- 后端离线 H2+MyBatis 单元测试：14 个测试类 / 71 项测试全部通过（0 失败，0 跳过）。
- 前端 Jest 单元测试：6 个测试套件 / 28 项测试全部通过；`npm run build` 零错误零 ESLint 警告。

---

## V1.2 项目跟踪看板、统一审批、采发对账与可插拔 AI 分析（阶段 G）

对应分支：`feat/tracking-board-analytics-ai`（基于 `fix/hardening-and-consistency` 构建，零新增外部 Maven/npm 依赖、零破坏性数据库结构变更）

### 1. 第一阶段：项目跟踪看板 + 统一待审核工作台
- **后端新增能力**（`5399f2b`）：
  - 新增 `FinanceVoucherMetrics` 公共精确计算工具（与前端 `应收 / 应付 / 待开票 / 缺票` 逻辑完全对齐）。
  - 新增 `GET /api/projects/tracking-board`（`ProjectTrackingController` / `ProjectTrackingBoardServiceImpl`）：按阶段泳道聚合全部项目进度、阶段停留天数、计划超期、质保状态（`ACTIVE / EXPIRING_SOON / EXPIRED`）、合同总额、回款率、估算毛利、出库金额与待审单据数，自动计算风险标签（`OVERDUE / STALLED / WARRANTY_EXPIRING / WARRANTY_EXPIRED / LOW_COLLECTION / NEGATIVE_MARGIN / PENDING_AUDIT`）。
  - 停留阈值支持通过 `system_config` 键 `project.tracking.stalled_days`（默认 `30`）动态调整。
  - 新增 `GET /api/approvals/pending`（`ApprovalWorkbenchController` / `ApprovalWorkbenchServiceImpl`）：聚合未审核的项目清单、出入库单据与财务凭证；自动过滤划拨单生成的关联入库单（`source_ref_type='WAREHOUSE_TRANSFER'`），避免重复审核；`SecurityConfig` 新增 `/api/approvals/**` 权限控制。
- **前端新增页面**（`9801c65`）：
  - 新增「项目跟踪看板」（`/project/tracking-board`，`ProjectTrackingBoard.js`）：支持阶段泳道看板与列表双视图、风险 KPI 快捷筛选、停留/质保预警徽标、快速推进下一阶段、一键导出 Excel。
  - 新增「统一待审核工作台」（`/approvals/pending`，`ApprovalWorkbench.js`）：按模块分标签页展示待审核事项，支持一键审核通过与详情跳转。
  - 已在 `App.js`、`Sidebar.js`、`HomePage.js` 和 `pagePermissions.js` 注册入口与权限感知显隐。

### 2. 第二阶段：项目清单采发对账 + 项目利润表与应收应付账龄分析
- **后端新增能力**（`2f193c1`）：
  - `ProjectListItemMapper` 新增 `selectByProjectListIds` 批量查询，消除 `ProjectListServiceImpl.getAggregateView` 的 N+1 明细查询。
  - 新增 `GET /api/projects/material-reconciliation/summary` 与 `GET /api/projects/{projectUuid}/material-reconciliation`（`ProjectReconciliationController` / `ProjectMaterialReconciliationServiceImpl`）：对比最终销售清单（`INITIAL_SALES + CHANGE`）、最终采购清单（`PROCUREMENT`）与实际采购入库、集采调入（`TRANSFER_JTX`）、项目调出（`TRANSFER_XTJ`）、发货出库（`OUTBOUND_NOTE`）及手工库存流水，识别 `PROCUREMENT_SHORTAGE`（采购缺口）、`PENDING_SHIPMENT`（待发货）、`OVER_SHIPPED`（超发预警）、`OVER_PROCURED`（超采）、`UNPLANNED`（清单外采发）与 `MATCHED`（已齐套）。
  - 严格剔除 `source_ref_type='WAREHOUSE_TRANSFER'` 自动生成入库单，杜绝划拨重复计算；库存数据受 `inventory.access / inventory.manage` 严格保护。
  - 新增 `GET /api/finance/analytics/overview`（`FinanceAnalyticsController` / `FinanceAnalyticsServiceImpl`，受 `FINANCE_PATHS` 保护）：输出基于 `BigDecimal` 的项目利润表（保留 `PROJECT_TRANSFER_OUT` 负向记账成本冲减）、非项目收支汇总，以及按业务发生日期统计的 `0-30 天 / 31-60 天 / 61-90 天 / 90 天以上` 应收与应付账龄分布。
- **前端新增页面**（`ad06283`）：
  - 新增「项目清单与采发对账」（`/project/reconciliation`，`ProjectMaterialReconciliation.js`）：支持跨项目汇总与单项目物料逐行钻取对账及 Excel 导出，并在 `ProjectDashboard.js` 添加「采发对账」直达按钮。
  - 新增「项目利润与应收应付账龄分析」（`/finance/analytics`，`FinanceAnalytics.js`）：展示整体毛利与毛利率卡片、项目利润明细表、应收/应付账龄进度条与往来方明细及 Excel 导出。

### 3. 第三阶段：可插拔 AI 经营简报与受 RBAC 约束的智能问答（`210c493`）
- **后端可插拔架构**（零新增第三方 SDK 依赖）：
  - 新增 `AiAnalysisProperties`（配置前缀 `app.ai.*`，默认 `app.ai.enabled=false`），未配置 Key 或未开启时系统照常运行。
  - 新增 `OpenAiCompatibleLlmClient`（基于 JDK 17 `java.net.http.HttpClient` + `ObjectMapper`）：支持无 Key 的本地 Ollama（如 `http://127.0.0.1:11434/v1`）及任意标准 OpenAI 兼容接口。
  - 新增 `GET /api/ai/status`、`POST /api/ai/briefing`、`POST /api/ai/ask`（`AiAnalysisController` / `AiBusinessAnalysisServiceImpl`，`SecurityConfig` 按模块访问权限受控）：
    - 仅采集当前登录账号实际拥有 RBAC 权限的数据域（`PROJECT / CONTRACT / INVENTORY / FINANCE / APPROVAL`）的确定性聚合指标；
    - 调用外部非本地端点且启用 `app.ai.mask-sensitive-names=true` 时，自动对客户名称脱敏（`客户#1` 等）并在本地还原；
    - 当未配置大模型或模型请求异常/超时时，自动使用确定性规则引擎生成简报与问答（`mode=RULE_BASED`、`fallbackUsed=true`），不抛出 500 故障。
- **前端页面与文档配套**：
  - 新增「智能分析」（`/analytics/ai`，`AiAnalytics.js`）：展示当前推理引擎模式（AI 大模型模式 vs 规则分析模式）、账号授权数据域标签、单项目/全局经营简报、可点击跳转的重点风险清单、可折叠底层 JSON 数据依据，以及按权限过滤的预设问题与交互问答区。
  - 同步更新 `requirements.md`（升级至 `V1.2.0` 第 15 章）、`deploy/env/backend.env.example` 与 `salesmanagement/README_BACKEND.md`。

### 4. 阶段 G 验证结果
- 后端离线 H2+MyBatis 单元测试：**19 个测试类 / 81 项测试全部通过**（0 失败，0 跳过）。
- 前端 Jest 单元测试：**9 个测试套件 / 32 项测试全部通过**（0 失败）。
- 前端生产构建：`npm run build` 编译成功，0 错误，0 ESLint 警告。

---

## V1.2.1 权限控制与安全防线深度加固（阶段 H）

### 1. 会话状态与实时鉴权
- **非激活/已删除用户实时拦截与权限热同步**（`CustomUserDetailsService`、`PasswordChangeRequiredFilter`、`AuthController`）：
  - 提取 `CustomUserDetailsService.loadAuthorities`，`PasswordChangeRequiredFilter` 每次受保护 API 请求实时校验用户仍处于 `ACTIVE` 状态，禁用/关闭/删除用户立即销毁 `HttpSession` 并返回 `401`。
  - 当数据库中的角色/权限变更时，自动同步刷新 `SecurityContextHolder` 与 Session 中的 `SPRING_SECURITY_CONTEXT`，实现角色/权限实时生效；同步校验 `/api/auth/heartbeat` 与 `/api/auth/me`，并将心跳与 `/me` 纳入强制改密白名单避免误拦。

### 2. 单据审批职责分离（SoD）与特权保护
- **移除硬编码角色名绕过并落库防自审与防篡改**（`CurrentUserServiceImpl`、`EntryAuditServiceImpl`、`WarehouseDocumentServiceImpl`、`FinanceVoucherServiceImpl`、`ProjectListServiceImpl`、`ProjectListItemServiceImpl`）：
  - 移除 `ROLE_System` / `ROLE_管理员` 硬编码特权判断，统一收敛为内置 `System` 账号或明确权限码 `document.entry-audit.manage`。
  - 后端强制阻止重复审核覆盖审核人、阻止无特权的录入人审核自己录入的单据、锁定已审核单据的修改与删除（修复 `WarehouseDocumentServiceImpl.audit` 此前未查询 `entryUser` / `auditorUser` 的问题）。
- **防止权限管理提权与误删内置 `System` 实体**（`SysUserServiceImpl`、`SysRoleServiceImpl`、`SysUserRoleServiceImpl`、`SysRolePermissionServiceImpl`、`SysRoleController`、`SysPermissionController`）：
  - 禁止非 `System` 账号修改、禁用、删除或重置 `System` 用户密码，禁止修改或删除 `System` 角色，禁止当前用户自禁用、自删或自改角色，禁止分配/回收超出当前账号自身权限范围的角色或权限项。
  - 新增 `/api/roles/options` 与 `/api/permissions/options` 选项接口，彻底解耦角色分配页与权限项管理页之间的跨权请求。

### 3. 附件按模块隔离、日志防篡改与前后端一致性修复
- **附件模块归属鉴权**（`AttachmentController`、`GlobalExceptionHandler`、`AttachmentCenter.js`）：按 `businessType` 严格校验并过滤对应业务模块访问/操作权限，统一 `AccessDeniedException` 返回 HTTP `403`。
- **认证与日志纵深加固**（`SecurityConfig`、`LoginRecordController`、`SystemConfigServiceImpl`、`SystemConfigController`、`application.properties`）：
  - 移除 `.httpBasic()` 与无效 `/api/admin/**` 规则，显式配置 `HttpStatusEntryPoint(401)` 与 Session Cookie `http-only` + `same-site=lax`。
  - 下线 `POST / PUT /api/login-logs`，并在路由层 `.denyAll()` 禁止外部写删登录日志与审计日志；校验 `system_config` 路径类配置防目录穿越并补全审计记录。
  - 补全 `/options` 与 `/api/pdf-exports/**` 对 `*.manage` / `*.entry` / `*.audit` / `logs.*` / `product.access` 的对齐放行。
- **前端路由、菜单与页面权限对齐**（`App.js`、`Sidebar.js`、`HomePage.js`、`pagePermissions.js`、`entryAudit.js`、`CRUDTable.js`、`ApprovalWorkbench.js` 等）：
  - 全站补齐 `*.manage` 自动包含查看与导出权限，释放 `product.access` 对物料主数据与价格管理页面的只读访问，`CRUDTable` 与各审核单据页面同步落实自审禁用与已审锁定提示。

### 4. 阶段 H 验证结果
- 后端离线单元测试（筛选 `service.impl.*`、`mapper.*`、`controller.*`、`config.*`，未包含依赖外部 MySQL 的 `SalesmanagementApplicationTests`）：**20 个测试类 / 89 项测试全部通过**（0 失败，0 错误，0 跳过）。
- 前端 Jest 单元测试：**10 个测试套件 / 35 项测试全部通过**（0 失败）。
- 前端生产构建：`npm run build` 编译成功，0 错误，0 ESLint 警告。

---

## V1.2.2 全模块深度缺陷排查与系统性治理（阶段 I）

### 1. 数据库增量升级与基线同步
- **新增增量升级脚本**：`database/12_defect_fixes_schema.sql`，并同步更新基线脚本 `database/00_current_schema.sql`、`database/01_current_seed_data.sql`、`database/02_current_views.sql` 与 `database/README.md`：
  - 将 `product_basic_info` 插入/更新触发器校验放宽为 `CHAR_LENGTH(TRIM(material_code))` `10-12` 位，支持 2 位频段码（如 `GW`/`DM`）与 3 位流水号生成的 11 位物料编码；
  - 将 `goods_transaction_dynamic_info` 的 `quantity`、`unit_price` 升级至 `DECIMAL(18,4)`，`amount` 升级至 `DECIMAL(18,2)`，消除高精度数量截断；
  - 消除 `contract_type` 中 `CL` 与 `LW` 的重名（`CL` 调整为 `劳务分包`）；
  - 为 `warehouse_info.warehouse_code` 补充条件唯一索引 `uk_warehouse_code`（无重复编码时自动添加）；
  - 新增 `system_config` 默认配置项：`inventory.allow_negative_stock=false` 与 `project.tracking.low_collection_stage_sort_order=9`。

### 2. 后端全模块缺陷治理
- **库存与仓储（`WarehouseDocument*`、`WarehouseServiceImpl`、`InventoryTransaction*`、`GoodsInventoryMapper.xml`）**：
  - 禁止通过 `/api/inventory-transactions` 直接新增、修改或删除 `sourceRefType=WAREHOUSE_DOC` 的单据流水；创建/更新/删除出入库单时统一执行非负库存约束（受 `inventory.allow_negative_stock` 控制），更新与删除均按历史与当前仓库集合校验；`集采入库（CENTRALIZED_PURCHASE）` 强制清空 `projectId`，其余必选项目类别强制校验项目存在性；
  - 保护系统保留仓库 `CK-JZCG` 与 `CK-XMCG` 不被重命名、停用（变更编码/名称）或删除；
  - 删除出入库单同步清理 `warehouse-documents` 业务附件，并修复 `CodeSequence` 序列回收及 `GoodsInventoryMapper` 聚合分组一致化。
- **项目、清单、状态历史、对账与看板（`Project*`、`StageServiceImpl`、`ProjectMaterialReconciliationServiceImpl`、`ProjectTrackingBoardServiceImpl`）**：
  - 修复 `ProjectListItemServiceImpl` 已审核父清单明细锁定漏洞：`create`/`update`/`delete`/`importFromExcel` 全链路在父清单已审核（`auditorUser != null`）且无覆写权限时拒绝变更；
  - 加固 `ProjectStatusHistoryServiceImpl` 阶段前向推进（禁止停用阶段、重复或回退推进）以及非 `System` 操作员对达到锁定阈值历史的降级修改与删除拦截；
  - 修复项目看板 `LOW_COLLECTION`、`stalled_days` 与对账中 `netSuppliedQty`/空清单边界计算问题。
- **财务与税率（`FinanceVoucher*`、`TaxRateDictServiceImpl`、`FinanceAnalyticsServiceImpl`）**：
  - 创建凭证忽略客户端传入 `voucherNo`，更新锁定 `voucherNo` 与 `occurredOn`、`yyyyMMdd` 前缀匹配、切换收付方向自动清空对向金额并允许保留既有历史停用税率；删除凭证时回收序列并联动清理 `finance-vouchers` 附件；
  - 加固 `TaxRateDictServiceImpl`：被凭证引用的税率禁止删除或变更数值，允许仅更新显示标签与启停状态。
- **客户、合同与活跃规则（`Customer*`、`Contract*`）**：
  - `CustomerServiceImpl` 对空白税号执行 trim-to-null 避免唯一索引空串冲突，生成 `customerCode` 后锁定 `regionId` 与 `customerTypeId`，更新时允许保留既有的停用行业并补齐创建/更新人及 `customers` 附件级联清理；
  - `CustomerIndustryDictServiceImpl`、`CustomerContactServiceImpl`、`ContractBasicInfoServiceImpl`、`ContractClauseServiceImpl`、`CustomerActivityRuleServiceImpl` 全面补齐引用完整性校验、活跃规则区间重叠校验与跨客户移动拦截。
- **物料与字典（`MaterialMaster*`、`ProductCategory/Subcategory/Brand/Band`、`ProductHistoryPriceServiceImpl`）**：
  - 物料创建校验启用的大写字母数字字典编码，支持 10–12 位编码（含 11 位 `GW`/`DM`）；更新锁定分类/品牌/频段；Excel 导入按行/全量事务模型与公式求值加固并修复停用/引用变更拦截；`ProductHistoryPrice` 由服务端统一写入并保留 `createUser`。
- **附件、认证会话、日志与安全、AI 及 PDF 导出**：
  - `AttachmentController` 统一跨平台文件名清洗、下载强制 `application/octet-stream` + `nosniff`、PDF `%PDF-` 魔数校验以及先库/先审再删文件（附告警日志）；
  - `AuthController`、`LoginRecordServiceImpl`、`PasswordChangeRequiredFilter` 增加 `trust-forward-headers` 配置开关、`RELOGIN` 关闭旧会话、`changeSessionId()`、强制改密登出闭合与心跳/超时会话清理；`AuditTrailMapper.xml` 动作筛选大小写不敏感；并确认 20 个写操作控制器接入 `AuditTrailService`；
  - `AiBusinessAnalysisServiceImpl` 优化 `NameMasker` 长度降序单遍脱敏与 `JsonNode` 安全 JSON 字符串脱敏，完善单项目简报作用域（待审列表与项目财务）及多域/财务意图精准识别；`TablePdfExportServiceImpl` 与 `PdfExportSupport` 补充行列/分段上限、列宽有限正数校验及字体缓存键加固。

### 3. 前端全链路同步修复与单元测试增强
- **基础链路与公共组件**：`api/index.js`、`api/modules.js`、`exporters.js`、`pagePermissions.js`、`CRUDTable.js`、`App.js` 修复错误对象透传、导出公式注入与 XML 控制字符清洗、`/inventory/warehouse` 与 `/project/list-dashboard` 权限精确匹配、模态框提交防连击与提交中禁止关闭、客户端筛选状态在列表刷新后的保持。
- **各业务模块页面**：
  - 库存（`WarehouseDocumentModal.js`、`WarehouseDoc.js`、`InventoryStock.js`、`InventoryTransactions.js`、`Warehouses.js`）、财务与 AI（`FinanceVouchers.js`、`TaxRateManagement.js`、`AiAnalytics.js`）修复表单联动回弹、税率浮点展示、未完成金额计入、`briefing` 竞态覆盖与异步异常提示；
  - 项目模块（`ProjectListDashboard.js`、`ProjectDashboard.js`、`ProjectLists.js`、`ProjectTrackingBoard.js`、`ProjectMaterialReconciliation.js`）修复权限接口替换、`DatePicker` 精度保留、导入结果明细弹窗、未关联物料与无频段编辑、父清单已审锁定、看板过滤联动 KPI 与待审跳转权限控制、对账异常清旧态与库存无权限提示；
  - 客户/合同/物料/附件/日志（`CustomerInfo.js`、`CustomerActivityRules.js`、`ContractInfo.js`、`ProductInfo.js`、`ProductPrices.js`、`AttachmentCenter.js`、`AuditTrails.js`、`LoginLogs.js`）补全可读详情展示、创建后分类/地区锁定、按客户过滤联系人、停用字典兼容、正整数 ID 搜索校验与 `RELOGIN` 状态展示。
- **新增自动化回归测试**：
  - 后端新增 `DefectRemediationTests.java`（7 项综合回归测试）；
  - 前端新增 `exporters.test.js`、`rbacPermissions.test.js`，并扩展 `CRUDTable.serverPagination.test.js`。

### 4. 阶段 I 最终验证结果
- 后端离线单元测试（`service.impl.*`、`mapper.*`、`controller.*`、`config.*`，未包含依赖外部 MySQL 的 `SalesmanagementApplicationTests`）：**21 个测试类 / 100 项测试全部通过**（0 失败，0 错误，0 跳过）。
- 前端 Jest 单元测试：**11 个测试套件 / 40 项测试全部通过**（0 失败）。
- 前端生产构建：`npm run build` 编译成功，**0 错误，0 ESLint 警告**。
