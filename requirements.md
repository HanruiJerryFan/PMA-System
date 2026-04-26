# PMA 功能需求书（Codex Compatible Markdown）

* 版本：V1.1.0
* 日期：2026-03-19
* 文档目的：将原始中文需求整理为适合 AI 编码代理（如 Codex）直接消费的结构化 Markdown 需求文档。
* 语言：中文
* 状态：Draft

\---

# 1\. 项目概述

## 1.1 产品名称

PMA（Project Management \& Accounting）系统

## 1.2 产品目标

PMA 系统用于支持以下业务场景：

1. 用户登录、权限与操作审计
2. 客户与客户联系人管理
3. 项目与项目阶段管理
4. 项目清单管理（销售清单、执行清单、增减清单）
5. 收支凭证管理与项目收支汇总
6. 物料代码管理与物料库存管理
7. 文档附件归档（合同扫描件、发票、货单等）

## 1.3 设计原则

1. 所有核心业务对象都应支持创建时间、创建人、更新时间、更新人。
2. 所有下拉项尽量来自字典表，避免自由输入。
3. 核心表单需支持导出 PDF、打印、附件上传/查看。
4. 系统需兼顾“项目业务”和“非项目业务”。
5. 系统需支持 Excel 导入，至少用于项目清单导入。

\---

# 2\. 角色与权限

## 2.1 用户能力范围

系统至少支持以下用户能力：

* 新建账号
* 账号禁用
* 账号关闭
* 设置初始密码
* 修改密码
* 配置操作权限

## 2.2 权限模型

采用 RBAC（Role-Based Access Control）：

* `user`
* `role`
* `permission`
* `user\_role`
* `role\_permission`

## 2.3 审计要求

系统需要记录：

* 登录开始时间
* 登录结束时间
* 登录次数
* 总使用时长
* 用户访问轨迹（页面 / 功能 / 操作记录）

\---

# 3\. 系统范围

本期范围包含以下模块：

1. 权限管理
2. 使用管理
3. 地域数据管理
4. 客户管理
5. 客户联系人管理
6. 项目管理
7. 项目清单管理
8. 项目状态管理
9. 收支管理
10. 物料代码管理
11. 库存管理
12. 附件管理
13. PDF 导出与打印

\---

# 4\. 术语与命名

## 4.1 核心术语

* 客户：业务往来单位
* 联系人：客户下属联系人
* 项目：销售、施工、调试、维保等业务对象
* 项目清单：项目相关商品/物料明细
* 收支凭证：项目或非项目的记账单据
* 物料代码：用于唯一标识商品/设备/物料的 12 位编码
* 入库/出库：库存流转记录

## 4.2 命名规则

数据库采用英文 snake\_case 命名，例如：

* `customer`
* `customer\_contact`
* `project`
* `project\_status\_history`
* `finance\_voucher`
* `material\_master`
* `inventory\_txn`

\---

# 5\. 数据库与实体设计

> 说明：以下为“数据模型”，用于帮助 Codex 直接生成后端实体、数据库表、接口与校验逻辑。

## 5.1 用户表 `user`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint / uuid|是|主键|
|username|varchar(64)|是|登录账号，唯一|
|password\_hash|varchar(255)|是|密码哈希|
|status|enum|是|`ACTIVE` / `DISABLED` / `CLOSED`|
|created\_at|datetime|是|创建时间|
|created\_by|bigint / uuid|否|创建人|
|updated\_at|datetime|是|更新时间|
|updated\_by|bigint / uuid|否|更新人|

## 5.2 角色表 `role`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint / uuid|是|主键|
|code|varchar(64)|是|角色编码，唯一|
|name|varchar(128)|是|角色名称|
|description|text|否|描述|

## 5.3 权限表 `permission`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint / uuid|是|主键|
|code|varchar(128)|是|权限编码，唯一|
|name|varchar(128)|是|权限名称|
|description|text|否|描述|

## 5.4 登录日志表 `login\_log`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint|是|主键|
|user\_id|bigint / uuid|是|用户 ID|
|login\_started\_at|datetime|是|登录开始时间|
|login\_ended\_at|datetime|否|登录结束时间|
|duration\_seconds|int|否|时长（秒）|
|login\_ip|varchar(64)|否|IP|
|user\_agent|text|否|设备信息|
|created\_at|datetime|是|创建时间|

## 5.5 访问轨迹表 `audit\_trail`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint|是|主键|
|user\_id|bigint / uuid|是|用户 ID|
|module|varchar(64)|是|模块名称|
|action|varchar(64)|是|操作类型|
|target\_type|varchar(64)|否|目标对象类型|
|target\_id|varchar(64)|否|目标对象 ID|
|detail|json / text|否|变化详情|
|occurred\_at|datetime|是|发生时间|

<!-- ## 5.6 省份表 `province`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|int|是|主键|
|code|varchar(16)|是|行政区编码|
|name|varchar(64)|是|省份名称|

## 5.7 城市表 `city`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|int|是|主键|
|province\_id|int|是|关联省份|
|code|varchar(16)|是|行政区编码|
|name|varchar(64)|是|城市名称| -->

## 5.8 客户表 `customer`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint / uuid|是|主键|
|customer\_code|varchar(9)|是|系统客户 ID，唯一|
|name|varchar(255)|是|客户名称|
|province\_id|int|是|所属省份|
|city\_id|int|是|所属城市|
|registered\_address|varchar(255)|否|注册地址|
|office\_address|varchar(255)|否|办公地址|
|tax\_identifier|varchar(18)|否|纳税人识别号，18 位|
|customer\_type|tinyint / enum|是|1 设计单位 / 2 集成商 / 3 合作伙伴 / 4 供应商 / 5 终端客户|
|bank\_name|varchar(255)|否|开户银行|
|bank\_account|varchar(64)|否|银行账号|
|industry|enum|否|制造 / 商业地产 / 能源 / 化工医药 / 医疗卫生 / 交通运输 / 政府机构 / 教育 / 建筑 / 酒店 / 金融 / 仓储 / 文化体育 / 其他|
|remark|text|否|备注|
|activity\_status|enum|是|`ACTIVE` / `NORMAL` / `INACTIVE` / `DORMANT`|
|last\_active\_at|datetime|否|最近活跃时间，用于状态计算|
|created\_at|datetime|是|创建时间|
|created\_by|bigint / uuid|否|创建人|
|updated\_at|datetime|是|更新时间|
|updated\_by|bigint / uuid|否|更新人|

### 5.8.1 客户系统 ID 生成规则

客户系统 ID 共 9 位：

* 前 4 位：区划编码 `area_code` 的前 4 位
* 第 5 位：客户类型编码
* 后 4 位：自然顺序号

客户类型映射：

* `1` = 设计单位
* `2` = 集成商
* `3` = 合作伙伴
* `4` = 供应商
* `5` = 终端客户

### 5.8.2 客户状态自动计算

默认根据“最近更新时间或最近活跃时间”计算：

* 30 天内：`ACTIVE`
* 31\~60 天：`NORMAL`
* 61\~90 天：`INACTIVE`
* 超过 90 天：`DORMANT`

> 该阈值必须支持后台配置。

## 5.9 客户联系人表 `customer\_contact`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint / uuid|是|主键|
|customer\_id|bigint / uuid|是|所属客户|
|name|varchar(128)|是|联系人姓名|
|title|varchar(128)|否|联系人职务|
|phone|varchar(32)|否|联系电话|
|wechat|varchar(64)|否|微信|
|email|varchar(128)|否|邮箱|
|qq|varchar(32)|否|QQ|
|remark|text|否|备注|
|created\_at|datetime|是|创建时间|
|created\_by|bigint / uuid|否|创建人|
|updated\_at|datetime|是|更新时间|
|updated\_by|bigint / uuid|否|更新人|

## 5.10 项目类型字典表 `project\_type`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|int|是|主键|
|code|varchar(32)|是|编码|
|name|varchar(64)|是|名称|

项目类型包括：

* 产品销售
* 系统施工
* 调试
* 维保

## 5.11 项目表 `project`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint / uuid|是|主键|
|project\_code|varchar(16)|是|项目编号，例如 `DL-2303`|
|name|varchar(255)|是|项目名称|
|region\_id|int|是|项目所在区域|
|project\_type\_id|int|是|项目类型|
|owner\_user\_id|bigint / uuid|否|项目负责人|
|participant\_1\_user\_id|bigint / uuid|否|项目参与人 1|
|participant\_2\_user\_id|bigint / uuid|否|项目参与人 2|
|participant\_3\_user\_id|bigint / uuid|否|项目参与人 3|
|planned\_start\_at|date / datetime|否|计划开始时间|
|planned\_end\_at|date / datetime|否|计划结束时间|
|warranty\_until|date|否|质保截止日期|
|sales\_contract\_attachment\_id|bigint / uuid|否|销售合同 PDF 附件|
|created\_at|datetime|是|创建时间|
|created\_by|bigint / uuid|否|创建人|
|updated\_at|datetime|是|更新时间|
|updated\_by|bigint / uuid|否|更新人|

### 5.11.1 项目编号规则

项目编号格式：

* 两位字母
* 一个连字符 `-`
* 四位数字

正则：

```regex
^\[A-Z]{2}-\\d{4}$
```

### 5.11.2 补充字段

* `customer\_id`：主客户 / 销售单位

否则项目回款、清单、财务凭证关联客户时会不完整。

## 5.12 项目状态阶段字典表 `project\_stage`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|int|是|主键|
|stage\_code|varchar(16)|是|阶段编码|
|description|varchar(128)|是|阶段说明|
|sort\_order|int|是|排序|

阶段定义如下：

|stage\_code|description|
|-|-|
|1.1|发现|
|1.2|植入|
|2.1|招投标|
|2.2|中标|
|2.3|签约|
|3.1|待执行|
|3.2|执行中-备货|
|3.3|执行中-供货|
|3.4|执行中-安装|
|3.5|执行中-调试|
|3.6|执行中-验收|
|4.1|质保|
|4.2|执行完毕|

## 5.13 项目状态记录表 `project\_status\_history`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint|是|自增主键|
|project\_id|bigint / uuid|是|项目 ID|
|stage\_id|int|是|阶段 ID|
|created\_at|datetime|是|创建时间|
|created\_by|bigint / uuid|否|创建人|

## 5.14 项目清单主表 `project\_list`

用于表示某一个项目下的一类清单。

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint / uuid|是|主键|
|project\_id|bigint / uuid|是|项目 ID|
|list\_type|enum|是|`INITIAL\_SALES` / `EXECUTION` / `CHANGE`|
|customer\_name|varchar(255)|否|客户（销售）单位，冗余字段|
|entry\_date|date|否|录入日期|
|pdf\_attachment\_id|bigint / uuid|否|导出的 PDF|
|created\_at|datetime|是|创建时间|
|created\_by|bigint / uuid|否|创建人|
|updated\_at|datetime|是|更新时间|
|updated\_by|bigint / uuid|否|更新人|

## 5.15 项目清单明细表 `project\_list\_item`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint / uuid|是|主键|
|project\_list\_id|bigint / uuid|是|所属清单|
|material\_id|bigint / uuid|否|物料主数据 ID|
|material\_code|varchar(12)|否|物料代码|
|item\_name|varchar(255)|是|货物名称|
|model|varchar(255)|否|型号|
|brand|varchar(128)|否|品牌|
|unit|varchar(32)|否|单位|
|quantity|decimal(18,4)|是|数量|
|unit\_price|decimal(18,2)|否|单价|
|total\_amount|decimal(18,2)|否|金额|
|source\_type|enum|否|`PROJECT\_PURCHASE` / `WAREHOUSE\_TRANSFER\_TO\_PROJECT`|
|remark|text|否|备注|

### 5.15.1 清单类型说明

* `INITIAL\_SALES`：初始销售清单明细
* `EXECUTION`：项目执行清单明细
* `CHANGE`：增减清单明细

### 5.15.2 导入要求

项目清单至少支持两种录入方式：

1. 单条录入
2. Excel 导入

### 5.15.3 输出要求

三类清单都应支持：

* 表单视图
* 生成 PDF
* 打印
* 保存归档

## 5.16 收支凭证表 `finance\_voucher`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint / uuid|是|主键|
|voucher\_no|varchar(10)|是|凭证序号|
|occurred\_on|date|是|发生日期|
|project\_id|bigint / uuid|否|项目 ID，非项目类时可为空|
|level\_1\_subject|enum|是|`NON\_PROJECT` / `PROJECT`|
|level\_2\_subject|enum|是|二级科目|
|summary|varchar(500)|是|内容摘要|
|transaction\_direction|enum|是|`RECEIVE` / `PAY`|
|tax\_rate|decimal(5,4)|是|记账税率|
|counterparty\_name|varchar(255)|是|收/付方名称|
|actual\_income\_amount|decimal(18,2)|否|收入金额|
|actual\_expense\_amount|decimal(18,2)|否|支出金额|
|booked\_amount|decimal(18,2)|是|记账金额|
|is\_completed|boolean|是|是否完成|
|remark|text|否|备注事宜|
|created\_at|datetime|是|创建时间|
|created\_by|bigint / uuid|否|创建人|
|updated\_at|datetime|是|更新时间|
|updated\_by|bigint / uuid|否|更新人|

### 5.16.1 凭证序号规则

凭证序号共 10 位：

* 前 8 位：`YYYYMMDD`
* 后 2 位：当日自然序号

示例：

* `2026031901`
* `2026031902`

### 5.16.2 一级科目

* `NON\_PROJECT`：非项目类
* `PROJECT`：项目类

### 5.16.3 二级科目

* 设备采购
* 辅材采购
* 施工费
* 销售费用
* 杂费（物流等）
* 摊销费
* 项目回款
* 仓库调入
* 项目调出
* 工资
* 差旅
* 招待
* 会务
* 车辆
* 其他

### 5.16.4 税率枚举

* `0%`
* `1%`
* `3%`
* `6%`
* `9%`
* `13%`

### 5.16.5 财务字段 UI 规则

1. 表单录入时，各项内容尽量使用下拉菜单。
2. 项目名称来源于项目数据库。
3. 当 `is\_completed = false` 时：

   * 对应收入或支出单元格使用粉红色填充。
4. 当二级科目为“项目调出”时：

   * “支出”显示为正数
   * “记账金额”显示为负数
   * 记账金额使用红色负号显示

### 5.16.6 记账金额公式（存在冲突，需确认）

```text
记账金额 = 实际收付金额 / (1 - (13% - 税率)) \* IF(二级科目="项目调出", -1, 1)
```

示例验证：

* 实际支出 `96.00`，税率 `0%`
* 若按“乘法公式”，结果是 `83.52`，与样例不符
* 若按“除法公式”，结果是 `110.34`，与样例一致

* **默认实现采用“除法公式”**

推荐实现逻辑：

```text
base\_amount = 实际收付金额
rate\_gap = 13% - tax\_rate
booked\_amount = base\_amount / (1 - rate\_gap)
if 二级科目 == "项目调出":
    booked\_amount = -booked\_amount
```

### 5.16.7 附件要求

每笔凭证可关联多个附件，例如：

* 发票
* 货单
* 扫描件
* 其他相关电子文档

新增关联表：`finance\_voucher\_attachment`。

## 5.17 附件表 `attachment`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint / uuid|是|主键|
|biz\_type|varchar(64)|是|业务对象类型|
|biz\_id|varchar(64)|是|业务对象 ID|
|file\_name|varchar(255)|是|文件名|
|file\_ext|varchar(32)|否|扩展名|
|mime\_type|varchar(128)|否|MIME|
|file\_size|bigint|否|文件大小|
|storage\_path|varchar(500)|是|存储路径|
|uploaded\_by|bigint / uuid|否|上传人|
|uploaded\_at|datetime|是|上传时间|

\---

# 6\. 物料代码管理

## 6.1 编码规则

物料代码共 12 位，由 2 位字母 + 10 位数字组成。

根据图片示例，编码结构为：

```text
\[大类2位字母]\[分项2位数字]\[品牌2位数字]\[顺序号3位数字]\[频段3位字符]
```

示例：

* `SB0101001400`

  * `SB` = 大类（设备）
  * `01` = 分项（信道机）
  * `01` = 品牌（和源通信）
  * `001` = 顺序号
  * `400` = 频段代码（400\~470MHz）
* `SB0101002350`

  * 顺序号 `002`
  * 频段代码 `350`
* `SB0102003150`

  * 分项 `01`
  * 品牌 `02`
  * 顺序号 `003`
  * 频段 `150`

> 注意：频段代码虽然通常为数字，但样例中也存在 `GW`、`DM` 等非纯数字值，因此最后 3 位定义为 `varchar(3)` 而不是纯数字。

## 6.2 物料代码字典表设计

### 6.2.1 品牌字典表 `material\_brand\_dict`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|int|是|主键|
|code|varchar(2)|是|品牌代码|
|name|varchar(128)|是|品牌名称|
|sort\_order|int|否|排序|
|is\_active|boolean|是|是否启用|

根据图片可识别的品牌样例：

|code|name|
|-|-|
|01|和源通信|
|02|海能达|
|03|摩托罗拉|
|04|科立讯|
|05|中兴高达|
|07|瑞玛通信|
|13|中天|
|99|国产|

> 业务方补齐完整字典后录入。系统必须支持后台自定义维护品牌字典。

### 6.2.2 大类字典表 `material\_category\_dict`

|code|name|
|-|-|
|SB|设备|
|XL|线缆|
|FC|辅材|
|YY|应用|
|FW|服务|
|QT|其他|

### 6.2.3 分项字典表 `material\_subcategory\_dict`

根据图片可识别的分项：

|code|name|
|-|-|
|01|信道机|
|02|手台|
|03|分合器|
|04|直放站|
|05|天线|
|06|馈合功分器|
|07|车台|
|08|电池|
|09|耳机|
|10|跳线|
|11|网线|
|12|置频|
|21|接头|
|22|支架|
|23|避雷器|
|24|巡更点|
|30|软件|
|40|调试费|
|41|驻点费|
|42|测试费|
|43|代办费|
|99|其它|

### 6.2.4 频段字典表 `material\_band\_dict`

根据图片可识别的频段代码：

|code|description|
|-|-|
|400|400\~470MHz|
|350|350\~370MHz|
|150|136\~174MHz|
|88|88\~108MHz|
|GW|公网|
|DM|多模（公网）|

## 6.3 物料主数据表 `material\_master`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint / uuid|是|主键|
|brand\_code|varchar(2)|是|品牌代码|
|category\_code|varchar(2)|是|大类代码|
|subcategory\_code|varchar(2)|是|分项代码|
|sequence\_no|varchar(3)|是|顺序号|
|band\_code|varchar(3)|是|频段代码|
|material\_code|varchar(12)|是|完整物料代码，唯一|
|name|varchar(255)|是|名称|
|model|varchar(255)|否|型号|
|specification|text|否|参数 / 规格|
|other\_note|text|否|其他说明|
|is\_active|boolean|是|是否启用|
|created\_at|datetime|是|创建时间|
|created\_by|bigint / uuid|否|创建人|
|updated\_at|datetime|是|更新时间|
|updated\_by|bigint / uuid|否|更新人|

### 6.3.1 自动生成要求

当商品第一次录入时，系统应支持通过选择：

* 大类
* 分项
* 品牌
* 频段

自动生成固定物料代码。

### 6.3.2 顺序号分配规则

* 同一 `(category\_code, subcategory\_code, brand\_code)` 组合下递增生成 `001`、`002`、`003`...
*  `band\_code` 也纳入顺序号唯一性范围

\---

# 7\. 库存管理

## 7.1 库存交易表 `inventory\_txn`

|字段|类型|必填|说明|
|-|-|-:|-|
|id|bigint / uuid|是|主键|
|material\_id|bigint / uuid|是|物料 ID|
|material\_code|varchar(12)|是|物料代码|
|material\_name|varchar(255)|是|物料名称|
|model|varchar(255)|否|型号规格|
|brand|varchar(128)|否|品牌|
|category|enum|是|库存业务类别|
|txn\_type|enum|是|`IN` / `OUT`|
|txn\_time|datetime|是|出入库时间|
|quantity|decimal(18,4)|是|数量|
|unit\_price|decimal(18,2)|否|采购/销售单价|
|amount|decimal(18,2)|否|金额|
|project\_id|bigint / uuid|否|相关项目|
|warehouse\_id|bigint / uuid|否|仓库|
|source\_ref\_type|varchar(64)|否|来源单据类型|
|source\_ref\_id|varchar(64)|否|来源单据 ID|
|created\_at|datetime|是|创建时间|
|created\_by|bigint / uuid|否|创建人|

## 7.2 库存业务类别

* 项目采购（销售）
* 集中采购
* 仓库调拨项目
* 项目调拨仓库

## 7.3 库存实时计算

系统应根据所有入库/出库交易自动计算实时库存：

```text
实时库存 = 累计入库数量 - 累计出库数量
```

额外提供物化视图或汇总表：

* `inventory\_balance`

## 7.4 输出报表

系统应支持以下输出：

1. 单次入库单
2. 单次出库单
3. 当月盘点明细汇总表
4. 项目物料出库汇总明细表

\---

# 8\. 界面与交互要求

## 8.1 通用要求

1. 所有列表页支持搜索、筛选、排序、分页。
2. 所有字典字段优先使用下拉框。
3. 支持详情查看、编辑、删除、附件查看。
4. 支持表格导出 Excel。
5. 关键业务单据支持导出 PDF 和打印。

## 8.2 财务凭证列表表现

根据示例图，财务汇总列表至少应包含：

* 序号
* 日期
* 项目名称
* 二级科目
* 内容摘要
* 收/付
* 记账税率
* 收/付方名称
* 收入
* 支出
* 记账金额（支出）
* 是否完成

并满足以下显示规则：

* 未完成项：对应金额单元格粉红底色
* 项目调出：记账金额为负数，红色显示
* 顶部可显示汇总值：收入合计、支出合计、记账金额合计

## 8.3 物料主数据列表表现

根据示例图，物料列表至少包含：

* 序号
* 品牌
* 大类
* 分项
* 名称
* 型号
* 参数
* 物料代码
* 其他说明

\---

# 9\. 导入导出要求

## 9.1 Excel 导入

至少支持：

* 项目清单导入
* 物料主数据导入
* 地域字典导入

导入能力应支持：

* 模板下载
* 行级错误提示
* 失败行回显
* 全量失败不入库或部分成功策略（需配置）

## 9.2 PDF 导出

以下对象应支持导出 PDF：

* 项目销售清单
* 项目执行清单
* 项目增减清单
* 入库单
* 出库单
* 财务凭证

\---

# 10\. 校验规则

## 10.1 基础校验

* 用户名唯一
* 客户名称 唯一或支持重复提醒
* 客户纳税人识别号长度必须为 18 位
* 项目编号必须匹配 `^\[A-Z]{2}-\\d{4}$`
* 物料代码长度必须为 12
* 凭证号长度必须为 10

## 10.2 财务校验

* 收入、支出不可同时填写为正值
* `transaction\_direction = RECEIVE` 时优先填写收入金额
* `transaction\_direction = PAY` 时优先填写支出金额
* `项目调出` 必须输出负的记账金额
* 未完成记录允许金额存在，但前端需高亮

## 10.3 清单校验

* 数量必须大于 0
* 单价不得小于 0
* 金额可由前端/后端自动计算：`数量 \* 单价`
* 物料代码存在时，应可自动反查物料信息

\---

# 11\. 非功能要求

## 11.1 安全

* 密码必须加密存储
* 附件下载需鉴权
* 操作需记录审计日志

## 11.2 可维护性

* 品牌、大类、分项、频段、税率、行业、项目阶段等全部字典化
* 支持后台配置字典项启用/停用

## 11.3 扩展性

* 附件表采用通用业务绑定方式
* 物料编码规则支持后续调整
* 客户状态阈值可配置

\---

# 12\. 示例数据（来源于图片）

## 12.1 物料编码组成示例

|大类|分项|品牌|顺序号|频段|
|-|-|-|-|-|
|SB|01|01|001|400|
|SB|01|01|002|350|
|SB|01|02|003|150|

## 12.2 物料主数据示例

|品牌|大类|分项|名称|型号|参数|物料代码|
|-|-|-|-|-|-|-|
|和源通信|设备|信道机|DMR数字智能信道机|Mark1000 MAX|频率 400MHz，信道数 16，高度 2U|SB0101001400|
|和源通信|设备|信道机|DMR数字智能信道机|Mark1000 MAX|频率 400MHz，信道数 16，高度 2U|SB0101002350|
|海能达|设备|信道机|数字中继台|HR1060|频率 150MHz，信道数 64，高度 1U|SB0102003150|
|海能达|设备|手台|数字对讲机|PD660-V|频率 150MHz，信道数 256|SB0202001150|

## 12.3 财务列表展示示例（规则提取）

* 未完成记录：金额单元格粉色高亮
* 项目调出：记账金额显示负号且为红色
* 列表显示收入、支出、记账金额汇总

\---

# 13\. API 与实现（供 Codex 使用）

## 13.1 推荐后端模块划分

* `auth`
* `user`
* `rbac`
* `region`
* `customer`
* `project`
* `project-list`
* `finance`
* `material`
* `inventory`
* `attachment`
* `audit`

## 13.2 推荐 REST API 资源

* `/api/users`
* `/api/roles`
* `/api/permissions`
* `/api/login-logs`
* `/api/audit-trails`
* `/api/provinces`
* `/api/cities`
* `/api/customers`
* `/api/customer-contacts`
* `/api/projects`
* `/api/project-stages`
* `/api/project-status-history`
* `/api/project-lists`
* `/api/project-list-items`
* `/api/finance-vouchers`
* `/api/materials`
* `/api/material-brand-dicts`
* `/api/material-category-dicts`
* `/api/material-subcategory-dicts`
* `/api/material-band-dicts`
* `/api/inventory-transactions`
* `/api/attachments`

## 13.3 推荐前端页面

* 登录页
* 用户与权限管理页
* 客户列表/详情页
* 联系人管理页
* 项目列表/详情页
* 项目阶段流转页
* 项目清单页
* 财务凭证录入页
* 财务汇总页
* 物料主数据页
* 库存流水页
* 库存汇总页
* 附件中心

\---

# 14\. 交付建议

对于 AI 编码代理，建议按以下顺序实现：

1. 字典表 + 地域表 + 用户权限
2. 客户与联系人
3. 项目与项目阶段
4. 物料编码字典 + 物料主数据
5. 项目清单 + Excel 导入
6. 财务凭证 + 汇总页
7. 库存流水 + 库存汇总
8. 附件、PDF 导出、打印
9. 审计与操作日志

\---
