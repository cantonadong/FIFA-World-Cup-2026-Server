# FIFA World Cup 2026 · Project Log

> 项目根目录: `D:\Dev\WC26\`  
> 最后更新: 2026-05-27 (v2)  
> 状态: **HTML 原型 4 Tab 全完成（交互优化 v2）· Android Debug APK 已构建**

---

## 目录

1. [项目概览](#1-项目概览)
2. [开发环境](#2-开发环境)
3. [项目结构](#3-项目结构)
4. [数据层](#4-数据层)
5. [HTML 原型 — 已完成功能](#5-html-原型--已完成功能)
6. [Android App — 已完成功能](#6-android-app--已完成功能)
7. [设计系统 / 视觉规范](#7-设计系统--视觉规范)
8. [交互设计规范](#8-交互设计规范)
9. [架构 & 技术决策](#9-架构--技术决策)
10. [Bug 修复记录](#10-bug-修复记录)
11. [跨 Tab 导航逻辑 (HTML)](#11-跨-tab-导航逻辑-html)
12. [构建 & 发布](#12-构建--发布)
13. [待办 / 后续迭代](#13-待办--后续迭代)

---

## 1. 项目概览

FIFA 2026 世界杯信息 App，双平台并行开发：

| 平台 | 路径 | 状态 |
|------|------|------|
| HTML 原型 (iOS-style SPA) | `D:\Dev\WC26\UI\` | 4 Tab 全部完成 |
| Android Native (Jetpack Compose) | `D:\Dev\WC26\App\` | 4 Tab 全部完成，Debug APK 已构建 |

**产品定位**: 面向球迷的赛事信息工具，包含积分榜、赛程、球队、球员 4 大模块，视觉风格对标 iOS 原生 App（SF Pro 字体系、圆角卡片、毛玻璃效果）。

**App ID**: `com.fifa.worldcup2026`  
**当前版本**: 1.0 (versionCode 1)

---

## 2. 开发环境

### 本机环境 (D:\Dev\WC26 所在机器)

| 工具 | 版本 / 路径 |
|------|------------|
| OS | Windows 11 Pro 10.0.22631 |
| Shell | PowerShell 5.1 (Windows PowerShell，**非** PowerShell Core) |
| Android Gradle Plugin | 9.0.1 |
| Gradle | 9.1.0 (via wrapper: `gradlew.bat`) |
| Kotlin | 2.3.20 |
| Java | JVM Toolchain 17 |
| compileSdk / targetSdk | 36 |
| minSdk | 26 (Android 8.0+) |
| Compose BOM | 2026.03.01 |
| Coil | 3.2.0 (coil3，**注意**非旧版 coil2，API 有差异) |
| kotlinx.serialization.json | 1.8.1 |

### PowerShell 5.1 注意事项 (踩坑)

- **不支持** `&&` / `||` pipeline chain operators，改用 `; if ($?) { B }`
- `Out-File` / `Set-Content` 默认编码 UTF-16 LE (带 BOM)，写 JSON 要加 `-Encoding utf8`
- `ConvertFrom-Json` 返回 PSCustomObject，无 `-AsHashtable`
- PowerShell 5.1 写出的 UTF-8 文件会带 BOM (`﻿`)，Android asset reader 已做处理（见 DataRepository.kt）

### 构建命令

```powershell
cd "D:\Dev\WC26\App"
.\gradlew assembleDebug        # Debug APK
.\gradlew assembleRelease      # Release APK（需配置签名）
```

APK 输出路径: `app\build\outputs\apk\debug\app-debug.apk`  
当前 Debug APK 大小: ~25.4 MB

---

## 3. 项目结构

```
D:\Dev\WC26\
├── PROJECT_LOG.md              ← 本文件
├── UI\                         ← HTML 原型（iOS-style SPA）
│   ├── tab1.html               ← Tab1: Standings（积分榜）
│   ├── tab2.html               ← Tab2: Schedule（赛程）
│   ├── tab3.html               ← Tab3: Teams（球队）
│   └── tab4.html               ← Tab4: Players（球员）
├── Data\
│   ├── team.csv                ← 原始球队数据（CSV 格式）
│   └── vanue.csv               ← 原始场馆数据（CSV 格式，注意文件名拼写 vanue）
├── Pic\
│   └── Country\                ← 国旗图片源文件（162 张 PNG）
└── App\                        ← Android Jetpack Compose 项目
    └── app\src\main\
        ├── java\com\example\wc2026\   ← 源码（包名实为 com.fifa.worldcup2026）
        │   ├── MainActivity.kt
        │   ├── data\
        │   │   ├── Models.kt           ← Team, Venue, Match, TeamStanding, Player
        │   │   ├── DataRepository.kt   ← JSON asset 加载
        │   │   └── PlayerData.kt       ← 20名球员硬编码数据
        │   ├── theme\
        │   │   ├── Color.kt
        │   │   ├── Theme.kt
        │   │   └── Type.kt
        │   └── ui\
        │       ├── AppScaffold.kt      ← 底部导航栏 + 4 Tab 路由
        │       ├── components\
        │       │   └── Common.kt       ← 共享组件（CountryFlag, TeamBadge, RankBar, PosBadge, LiveDot, ovrColor, rankBarColor）
        │       ├── standings\
        │       │   ├── StandingsScreen.kt
        │       │   └── StandingsViewModel.kt
        │       ├── schedule\
        │       │   ├── ScheduleScreen.kt
        │       │   └── ScheduleViewModel.kt
        │       ├── teams\
        │       │   ├── TeamsScreen.kt
        │       │   └── TeamsViewModel.kt
        │       └── players\
        │           ├── PlayersScreen.kt
        │           └── PlayersViewModel.kt
        └── assets\
            ├── data\
            │   ├── teams.json          ← 48支球队
            │   ├── matches.json        ← 104场比赛（GS + KO）
            │   └── venues.json         ← 16个场馆
            └── pic\
                └── country\            ← 162张国旗 PNG
```

> **重要**: 源码文件夹路径是 `com\example\wc2026`（Android Studio 创建时的旧路径），但 **package 声明**全部是 `com.fifa.worldcup2026`。两者不一致但 Gradle 只看 package 声明，可以正常编译。

---

## 4. 数据层

### JSON 数据文件

#### teams.json（48条）

```json
{
  "id": "fr",
  "name": "France",
  "group": "A",
  "confederation": "UEFA",
  "fifaRank": 2,
  "imageFile": "France.png",
  "flagFile": "France.png",
  "primaryColor": "#002395",
  "status": "Active",
  "worldCupDebut": false
}
```

关键字段:
- `flagFile` — 对应 `assets/pic/country/` 下的文件名，格式为**完整国家英文名 + .png**（如 `South Korea.png`，`United States.png`）
- `primaryColor` — hex 色值，用于球队详情页 hero 背景着色
- `status` — `"Active"` 或 `"Withdrawn"`（有球队退赛）
- `worldCupDebut` — boolean，首次参加世界杯标记

#### matches.json（104条）

```json
{
  "id": "gs_a1",
  "stage": "GS",
  "stageName": "Group Stage",
  "group": "A",
  "matchday": 1,
  "date": "2026-06-12",
  "time": "19:00",
  "team1Id": "us",
  "team1Name": "United States",
  "team2Id": "mx",
  "team2Name": "Mexico",
  "venueId": "sofi",
  "status": "upcoming",
  "homeScore": null,
  "awayScore": null
}
```

stage 枚举: `GS`, `R32`, `R16`, `QF`, `SF`, `3P`, `Final`  
status 枚举: `upcoming`, `live`, `ft`

#### venues.json（16条）

```json
{ "id": "sofi", "name": "SoFi Stadium", "city": "Los Angeles", "country": "USA" }
```

### PlayerData（硬编码，20名球员）

由于无 players.json，球员数据在 `PlayerData.kt` 中硬编码。数据来源：与 HTML tab4.html 中的 PLAYERS 数组完全对应。

Player 模型字段:
```
id, name(缩写), full(全名), country, flagFile, cc(国家代码), 
club, clubCC(俱乐部国家代码), pos(GK/DF/MF/FW), age, 
ovr, spd, atk, pas, dri, def, phy, valueMEUR,
height, weight, foot(L/R), caps, cGoals, cAssists
```

国家代码 (cc) 映射（HTML tab3/tab4 使用，Android 用国旗文件名）:
- `fr` → France.png
- `ar` → Argentina.png
- `br` → Brazil.png
- `gb-eng` → England.png（**注意**: 不是 UK.png）
- `de` → Germany.png
- `es` → Spain.png
- `ng` → Nigeria.png
- `ma` → Morocco.png
- `jp` → Japan.png

### 国旗文件命名规则

路径: `assets/pic/country/<完整英文国名>.png`  
特殊案例:
- 英格兰: `England.png`（不是 `United Kingdom.png`）
- 美国: `United States.png`
- 韩国: `South Korea.png`
- 科特迪瓦: `Ivory Coast.png`（也有 `Côte D'Ivoire.png`，两份都存在）
- 共有 162 张国旗，覆盖超出 48 支参赛队的范围

---

## 5. HTML 原型 — 已完成功能

所有 tab 共享特征:
- iOS safe-area 支持 (`env(safe-area-inset-top)`, `padding-bottom: env(safe-area-inset-bottom)`)
- 固定底部 tab bar（4个 Tab: Standings / Schedule / Teams / Players）
- iOS 系统字体栈: `-apple-system, 'SF Pro Display', 'Helvetica Neue', sans-serif`
- 去除滚动条: `.scroll-area::-webkit-scrollbar { display: none }`
- `-webkit-tap-highlight-color: transparent`（去除移动端点击高亮）

### Tab1: Standings（积分榜）`tab1.html`

- 积分榜列表（按 Group A–L 分组）
- 分组 Chip 导航（横向滚动，点击跳转 + 列表滚动同步 chip）
- 赛段切换: Group Stage / Knockout Stage（下拉菜单 pill）
- Group Stage: 每组显示队伍排名表格（P/W/D/L/GD/PTS/FIFA Rank）
  - 前2名绿色左边条（晋级指示）
  - 积分榜图例（Qualify to Round of 32）
- Knockout Stage: 按轮次展示对阵卡片
- 今日比赛横向卡片区（即将开赛的比赛）
  - 卡片布局: mc-top（状态/分组）与 mc-teams（国旗/队名）采用同构三列结构（flex:1 | 固定50px | flex:1），确保左侧状态+国旗+队名及右侧分组+国旗+队名各自垂直对齐居中
  - 点击卡片任意区域 → 跳转 tab2 并滚动到该场比赛所在日期锚点（`goSchedule(date)`）
- 小组阶段队伍行点击 → 跳转 tab3 并自动打开该队详情（`goTeam(code)`）
- 淘汰赛阶段对阵卡片中非 TBD 球队块点击 → 跳转 tab3 并自动打开该队详情

### Tab2: Schedule（赛程）`tab2.html`

- 赛程日期 chip 横向滚动（2026-06-12 至 2026-07-19）
- 按日期分组展示比赛卡片
- 赛段筛选（Group Stage / Knockout Stage）
- 比赛卡片显示: 两队 flag + 队名 + 比分/时间 + 场馆 + 状态（live/ft/upcoming）
- Live 比赛: 红色脉冲点动画
- 点击比赛卡片中队旗/队名（小组赛 + 淘汰赛均支持）→ 跳转 tab3 并自动打开该队详情
- 点击赛程卡片 → 展示比赛详情（场馆、时间、地图链接）
- 添加日历功能（iOS Calendar / Google Calendar 跳转）
- 页面加载时读取 `sessionStorage('tab2_date')`，自动滚动到对应日期锚点（供 tab1 卡片跳转使用）

### Tab3: Teams（球队）`tab3.html`

- 搜索框（实时过滤）
- 分段排序控制（FIFA Rank / Name / Group）
- 联合会筛选 chips（Group 排序时显示: UEFA / CONMEBOL / CONCACAF / CAF / AFC / OFC）
- 队伍列表: 国旗 + 队名 + 分组 + FIFA Rank + RankBar
- 队伍详情页（右滑进入）:
  - Hero 区: 大幅国旗 + 队名 + 分组徽章 + debut 徽章
  - Zone B（深色）: FIFA Rank + 联合会 + 分组
  - 近期比赛 section
  - 阵容 section（当前球员列表，来自 PLAYERS 数组匹配 country）
  - 球员行点击 → 跳转 tab4 并自动打开该球员详情
  - 分享按钮（Toast 提示）
- 来自 tab1/tab2 的 deep link: 读取 `sessionStorage('tab3_team')` 自动展开队伍详情

### Tab4: Players（球员）`tab4.html`

- 搜索框（实时过滤: 名字/国家/俱乐部/位置）
- 分段排序（Rating OVR / Position）
- Position 排序时显示位置子筛选（All / GK / DF / MF / FW）
- 球员列表分段显示（OVR: Elite 90+ / World Class 85-89 / Top Player 80-84 / Good 79-）
- 球员详情页（右滑进入）:
  - Zone A（暖色背景）: 球员剪影头像 + 全名
  - Zone B（深色背景）: 国旗+国家 / AGE / POS / OVR
  - Attributes 六维属性雷达图（Canvas 绘制）+ SPD/ATK/PAS/DRI/DEF/PHY 数值
  - 俱乐部 section: 俱乐部盾牌 SVG + 名称 + 联赛 + 市场价值
  - Physical Profile: 身高 / 体重 / 惯用脚
  - National Team Stats: 出场数 / 进球 / 助攻
  - 国旗/国家名点击 → 跳转 tab3 并自动打开该队详情
  - 分享按钮（Share Modal）
- OVR Stars: 5颗星按 OVR/20 比例点亮（量化到 0.25 步进）
- 俱乐部盾牌: SVG 动态生成（参见 CLUBS 字典）
- 来自 tab3 的 deep link: 读取 `sessionStorage('tab4_player')` 自动展开球员详情

---

## 6. Android App — 已完成功能

### AppScaffold

- Material3 `NavigationBar` 底部 4 Tab（Standings / Schedule / Teams / Players）
- 图标: 自定义 drawable（`ic_tab_standings`, `ic_tab_schedule`, `ic_tab_teams`, `ic_tab_players`）
- 选中色: Blue (#007AFF)，未选中: Gray (#8E8E93)
- 顶部导航惰性透明（`indicatorColor = Color.Transparent`，无 MD3 波纹指示器）
- Tab 切换无动画（直接替换，保留各 Tab 各自的 ViewModel 状态）

### Tab1: StandingsScreen

- 数据: 从 `teams.json` + `matches.json` 加载
- 赛段切换: Group Stage / Knockout Stage（右上角 pill 下拉）
  - Pill 有 spring 弹跳动画
  - 下拉菜单: scale + fade 动画，点击外部关闭
- Group Stage:
  - 分组 chip 横向滚动条
  - 列表 ↔ chip 双向同步（`LaunchedEffect(listState.firstVisibleItemIndex)`）
  - `UpcomingMatchesSection`: 今日/最近比赛横向卡片（最多5场）
  - `GroupTable`: 每组完整排名表（RK / P / W / D / L / GD / PTS 列）
  - 绿色左边条标记晋级区（前2名）
  - 积分差显示颜色: 正数绿，负数红，零 Label2
- Knockout Stage:
  - 轮次 chip 横向滚动（R32/R16/QF/SF/3P/Final）
  - 每轮赛果卡片（胜方加粗 + 绿边框，负方半透明）
- `computeUpcomingMatches`: 取最早一天且不超过5场

### Tab2: ScheduleScreen

- 数据: 从 `matches.json` + `teams.json` + `venues.json` 加载
- 日期 chip 横向滚动（2026-06-12 ~ 2026-07-19）
- 点击 chip → 列表动画滚动到对应日期
- 列表滚动 → chip 跟随高亮
- 赛程卡片: 两队 flag（圆形裁剪 + zoom fill）/ 比分或时间 / 场馆名
- Live 状态: 红色脉冲 `LiveDot` 组件
- 添加日历: `Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)`
- Google Maps 跳转: `Intent(Intent.ACTION_VIEW, Uri.parse("geo:..."))`

### Tab3: TeamsScreen

- 数据: 从 `teams.json` 加载（`TeamsViewModel extends AndroidViewModel`）
- 搜索: 实时过滤（队名 / 分组 / 联合会）
- 分段排序（FIFA Rank / Name / Group）
- Group 排序时显示联合会 filter chips（`AnimatedVisibility` expandVertically）
- 按排序分段展示:
  - RANK: 按 Top16 / 17-40 / 41-80 / 81+ 四档分组
  - NAME: 按首字母分组
  - GROUP: 按 A-L 分组
- 队伍行: 国旗 + 名称 + 分组 / 联合会 + FIFA Rank + RankBar + 箭头
- 球队详情页（`AnimatedVisibility` + `BackHandler`，右滑进入）:
  - Hero: primaryColor 10% 透明着色背景 + 大幅国旗 + 球队名
  - Chip: 分组 / 2026 Debut（橙色）/ Withdrawn（红色，若退赛）
  - Info 卡片: FIFA Rank（带 RankBar）/ 联合会 / 分组
  - Featured Players: 从 `PlayerData.all` 按 `country == team.name` 匹配
    - 显示位置徽章 + 全名 + 俱乐部 + OVR
- `team.abbr` 计算: 单词首字母，最多3字符（`Team.kt` computed property）

### Tab4: PlayersScreen

- 数据: 从 `PlayerData.all`（硬编码20人，无异步加载，`PlayersViewModel extends ViewModel`）
- 搜索: 实时过滤（缩写名 / 全名 / 国家 / 俱乐部 / 位置）
- 分段排序（Rating / Position）
- Position 排序时显示位置 chips（ALL / GK / DF / MF / FW）
- OVR 排序分段: Elite 90+ / World Class 85-89 / Top Player 80-84 / Good 79-
- 球员行: 排名序号 + 灰色剪影头像 + 名字+国旗+国家 + 位置徽章 + OVR + 箭头
- 球员详情页（`AnimatedVisibility` + `BackHandler`，右滑进入）:
  - Hero Zone A（暖色 `#F5EFE8`）: 灰色剪影头像 + 全名
  - Hero Zone B（国家色 blend 深蓝 `#1C1C2E` at 35%）: 国旗+国家 / AGE tile / POS tile / OVR数字
  - Attributes: 6条带动画的 stat bar（`animateFloatAsState` tween 600ms）
    - SPD / ATK / PAS / DRI / DEF / PHY
    - 颜色: ≥90 Gold，≥75 Blue，≥60 Green，其余 Gray
  - Club 卡片: 俱乐部名首3字母缩写徽章 + 俱乐部名 + 俱乐部国旗 + 联赛名 + 市场价值
  - Physical Profile: 身高 / 体重 / 惯用脚
  - National Team Stats: 出场数 / 进球 / 助攻

---

## 7. 设计系统 / 视觉规范

### 颜色体系（`Color.kt`）

```kotlin
val Blue   = Color(0xFF007AFF)   // 品牌主色、可交互元素、选中状态
val Green  = Color(0xFF34C759)   // 晋级、正值、成功
val Red    = Color(0xFFFF3B30)   // Live状态、负值、错误、退赛
val Orange = Color(0xFFFF9500)   // 首秀/特殊标记、Final 颜色
val Gold   = Color(0xFFB8860B)   // Elite OVR、金色元素
val Gray   = Color(0xFF8E8E93)   // 未选中Tab、次要内容
val Purple = Color(0xFFAF52DE)   // KO阶段 QF/SF 标签色

val Bg           = Color(0xFFF2F2F7)   // 页面背景（iOS 系统灰）
val WCSurface    = Color(0xFFFFFFFF)   // 卡片 / 导航栏背景
val SurfaceElevated = Color(0xFFF9F9F9) // 表格表头背景

val Label1    = Color(0xFF000000)          // 主文字
val Label2    = Color(0x993C3C43)   // 60% 透明 — 次要文字
val Label3    = Color(0x4D3C3C43)   // 30% 透明 — 辅助文字 / 占位
val Separator = Color(0x1F3C3C43)   // 12% 透明 — 分割线
```

### 字号规范

| 用途 | 字号 | 字重 |
|------|------|------|
| 导航栏标题 | 17sp | SemiBold |
| 详情页导航标题 | 15sp | SemiBold |
| 返回按钮 | 16sp | Medium |
| 大标题（详情页球队/球员名） | 24–26sp | Bold |
| 列表项主文字 | 14–15sp | Medium |
| 列表项次要文字 | 11–12sp | Regular |
| 统计数字（OVR / 积分） | 14–15sp | Bold |
| 段落标题（SECTION HEADER） | 11sp | SemiBold, 全大写, letterSpacing 0.5sp |
| 状态/标签 chip | 12–13sp | SemiBold |
| 分割线（HorizontalDivider） | 0.5dp | — |

### 圆角规范

| 元素 | 圆角 |
|------|------|
| 大卡片（详情、比赛卡） | 16dp |
| 小卡片（Chip） | 20dp |
| 排序 segmented control | 9dp outer / 7dp inner |
| 搜索框 | 10dp |
| 国旗（列表） | 3dp |
| 国旗（详情大图） | 6–8dp |
| 位置徽章 (PosBadge) | 4dp |
| 底部导航 indicator | 透明（不使用 MD3 默认 indicator） |

### 位置徽章颜色 (PosBadge / POS_CLASS)

| 位置 | 背景（12% alpha） | 文字色 |
|------|------------------|--------|
| GK | Orange 15% | `#CC7A00` |
| DF | Green 12% | `#1A7A38` |
| MF | Blue 12% | `#005EC4` |
| FW | Red 10% | `#CC2020` |

详情页深色 tile 版（暗色背景上）的徽章颜色为实色（无透明）。

### FIFA Rank 颜色（rankBarColor）

| 排名 | 颜色 |
|------|------|
| ≤15 | Blue |
| ≤40 | Green |
| ≤80 | Orange |
| >80 | Gray |

### OVR 颜色（ovrColor）

| OVR | 颜色 |
|-----|------|
| ≥90 | Gold |
| ≥85 | Blue |
| ≥80 | `#1C7A3E` (深绿) |
| <80 | Gray |

### Stat Bar 颜色（PlayersScreen）

| 数值 | 颜色 |
|------|------|
| ≥90 | Gold |
| ≥75 | Blue |
| ≥60 | `#1C7A3E` (深绿) |
| <60 | Gray |

---

## 8. 交互设计规范

### 详情页进入/退出动画

Android 实现:
```kotlin
// 进入: 从右侧滑入（带弹性）
slideInHorizontally(
    initialOffsetX = { it },
    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)
)
// 退出: 向右滑出
slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec = tween(260)
)
```

HTML 实现: `transform: translateX(100%)` → `translateX(0)`, transition 0.38s cubic-bezier(.34,1.1,.64,1)

### Back 手势处理

Android: `BackHandler { vm.selectTeam(null) }` / `vm.selectPlayer(null)`  
规则: **先注册 BackHandler，再触发 AnimatedVisibility** —— `if (selectedTeam != null) { BackHandler {...} }` 在 `Box` 外部

### 下拉/弹出菜单动画（StandingsScreen Stage Pill）

```kotlin
scaleIn(tween(200), initialScale = 0.88f, transformOrigin = TransformOrigin(1f, 0f))  // 右上角展开
scaleOut(tween(130), targetScale = 0.88f, ...)
```

### 分组 Chip ↔ 列表 双向同步

```kotlin
LaunchedEffect(listState.firstVisibleItemIndex) {
    val grp = groups[adjustedIdx / 2]
    if (grp != selectedGroup) {
        selectedGroup = grp
        chipScrollState.animateScrollToItem(grpIdx)
    }
}
```

点击 Chip:
```kotlin
scope.launch { listState.animateScrollToItem(itemIdx) }
scope.launch { chipScrollState.animateScrollToItem(grpIdx) }
```

### CountryFlag 图像缩放技巧

圆形裁剪 + 放大显示（避免国旗图片边缘白边/padding 问题）:
```kotlin
Box(Modifier.size(40.dp).clip(CircleShape).background(...)) {
    CountryFlag(modifier = Modifier.requiredSize(76.dp))  // 比容器大 ~1.9x，zoom fill 效果
}
```

### Segmented Control 实现

无需 MD3 `SegmentedButton`，自绘:
```kotlin
Row(
    Modifier.clip(RoundedCornerShape(9.dp))
        .background(Color(0x1F787880))  // iOS 风格灰底
        .padding(2.dp)
) {
    items.forEach { item ->
        Box(
            Modifier.weight(1f)
                .clip(RoundedCornerShape(7.dp))
                .background(if (isOn) WCSurface else Transparent)  // 选中项白底
                .clickable { ... }
        )
    }
}
```

### 动画 Spring 参数参考

| 效果 | dampingRatio | stiffness |
|------|-------------|-----------|
| Chip 按压回弹 | 0.4f | High |
| 详情页滑入 | 0.82f | Medium |
| Scale pill 弹跳 | 0.5f / 0.4f | High / Medium |

---

## 9. 架构 & 技术决策

### Android 架构

- **单 Activity + Compose**（`MainActivity → AppScaffold → [4 Tab Screens]`）
- **ViewModel + StateFlow** 状态管理（每个 Tab 独立 ViewModel）
- **没有使用 Navigation Compose**（虽然依赖中有 `navigation-compose`），Tab 切换和详情页均用 `when(selectedTab)` + `AnimatedVisibility` 实现
- **ViewModel 类型**:
  - Standings / Schedule / Teams: `AndroidViewModel(app)` — 需要 Context 加载 JSON asset
  - Players: `ViewModel()` — 数据硬编码，无需 Context
- **数据加载**: `viewModelScope.launch` + `Dispatchers.IO`（通过 `withContext`）

### HTML 原型架构

- **单文件 SPA**：每个 tab 是独立 HTML 文件，无框架依赖
- **跨 Tab 状态传递**: `sessionStorage` 作消息总线（不用 localStorage 是因为需要一次性读取后清除）
- 所有数据内联在 JS 变量中（`const TEAMS = [...]`, `const PLAYERS = [...]` 等）

### Coil 3 图像加载

```kotlin
AsyncImage(
    model = ImageRequest.Builder(ctx)
        .data("file:///android_asset/pic/country/France.png")
        .build(),
    contentScale = ContentScale.Crop,
    modifier = modifier
)
```

注意: Coil 3 API 与 Coil 2 不同，`ImageRequest.Builder` 构造方式相同，但部分 API 有变化。

### JSON BOM 处理

PowerShell 5.1 保存的 UTF-8 文件带有 BOM (`﻿`)，DataRepository 中统一处理:
```kotlin
.removePrefix("﻿")  // 或 .removePrefix("﻿")
```

### `Team.abbr` 计算逻辑

```kotlin
val abbr: String get() {
    val words = name.trim().split(Regex("\\s+"))
    return when {
        words.size == 1 -> name.take(3).uppercase()
        else -> words.take(3).map { it.first().uppercaseChar() }.joinToString("")
    }
}
```

---

## 10. Bug 修复记录

### HTML Bug 修复

#### B5: tab1.html — Today's Matches 卡片国旗偏左 + 垂直不对齐（2026-05-27 v2）
- **症状**: 今日比赛卡片中两侧国旗视觉偏左，状态文字/国旗/队名三者无垂直轴对齐
- **原因**: `.mc-teams` 使用 `justify-content:space-between` + `.mc-team{width:48px}` 固定宽，导致中间分数列宽度随内容变化，破坏左右对称；`.mc-top` 与 `.mc-teams` 列结构不一致，状态文字不在国旗正上方
- **修复**:
  - `.mc-team` 改为 `flex:1`（去掉 `width:48px`），两侧队伍列等宽
  - `.mc-score-col` 改为 `flex-shrink:0; min-width:50px`，分数列固定宽不压缩
  - `.mc-top` 改为同构三列结构（`flex:1 | min-width:50px 占位 | flex:1`，均 `justify-content:center`），使状态/分组标签与各自国旗列垂直对齐

#### B6: tab1.html — 今日比赛卡片无跨 Tab 导航（2026-05-27 v2）
- **症状**: 点击今日比赛卡片无反应，无法跳转到 tab2 对应日期
- **修复**: 为每条 `TODAY_CARDS` 添加 `date` 字段；新增 `goSchedule(date)` 函数写 `sessionStorage('tab2_date')` 后跳 tab2；卡片根元素添加 `onclick="goSchedule('${m.date}')"` 

#### B7: tab1.html — 淘汰赛阶段球队无法点击跳转 tab3（2026-05-27 v2）
- **症状**: 淘汰赛对阵卡片中点击球队 flag/name 无任何响应
- **修复**: `teamBlk()` 函数对非 TBD 球队添加 `onclick="goTeam('${t.c}')"` 及 `cursor:pointer`，TBD 占位球队保持不可点击

#### B1: tab3.html — `showShareToast` null reference
- **症状**: 分享 Toast 触发时 JS 报错（`Cannot read properties of null`）
- **原因**: 代码使用 `document.getElementById('screen').appendChild(t)`，但页面无 `#screen` 元素
- **修复**: 改为 `document.body.appendChild(t)` + `position: fixed`
- **教训**: 详情页 overlay 内的操作需注意作用域，body append 更安全

#### B2: tab3.html — 孤立关闭 `</div>` 导致 DOM 结构错误
- **症状**: 页面渲染异常，元素布局错位
- **原因**: `</div><!-- /screen -->` 出现在 `<script>` 标签前，无对应开标签
- **修复**: 直接删除该孤立标签
- **教训**: 动态生成 HTML 时的 DOM 拼接要检查开闭标签对称性

#### B3: tab1.html — `<div class="team-row">` 缺少关闭 `>`
- **症状**: 内联内容被当作属性值解析，队伍行显示空白
- **原因**: 在 template literal 中拼接 `onclick` 属性时，末尾 `>` 被误删
- **修复**: 补回 `onclick="goTeam('${t.c}')">` 的 `>`
- **教训**: 在 template literal 内操作 HTML 字符串时，每次修改后验证生成的 HTML 结构

#### B4: tab2.html — `<body>` 后多余 `</div>`
- **症状**: 页面底部出现意外空白，布局高度异常
- **原因**: `<body>` 标签后有孤立的 `</div>`（行 306）
- **修复**: 直接删除

### Android Bug 修复

#### B5: CountryFlag 在圆形容器内显示不满
- **现象**: 国旗图在 40dp 圆形容器中有边距，显示偏小
- **修复**: 使用 `Modifier.requiredSize(76dp)` 打破父约束，实现 zoom fill 效果
- **适用场景**: StandingsScreen 今日比赛卡片中的圆形国旗

#### B6: BOM 字符导致 JSON 解析失败
- **现象**: 第一次运行时 JSON 解析抛异常
- **原因**: PowerShell 5.1 写出的 JSON 文件带 UTF-8 BOM
- **修复**: `readAsset()` 中 `.removePrefix("﻿")`

---

## 11. 跨 Tab 导航逻辑 (HTML)

使用 `sessionStorage` 作为一次性消息总线。发送方在跳转前写入 key，接收方在页面加载时读取并清除。

### 从积分榜/赛程 → 球队详情 (Tab1/Tab2 → Tab3)

```javascript
// 发送方 (tab1.html / tab2.html)
function goTeam(code) {
    sessionStorage.setItem('tab3_team', code);
    window.location.href = 'tab3.html';
}

// 接收方 (tab3.html, script 末尾 init 检查)
const _pendingTeam = sessionStorage.getItem('tab3_team');
if (_pendingTeam) {
    sessionStorage.removeItem('tab3_team');
    const _team = TEAMS.find(t => t.code === _pendingTeam);
    if (_team) setTimeout(() => openDetail(_team), 80);  // 80ms 延迟等 DOM ready
}
```

### 从球队阵容 → 球员详情 (Tab3 → Tab4)

```javascript
// 发送方 (tab3.html 阵容球员行点击)
row.addEventListener('click', () => {
    sessionStorage.setItem('tab4_player', p.name);  // p.name = 缩写名，如 "K. Mbappé"
    window.location.href = 'tab4.html';
});

// 接收方 (tab4.html init)
const _pendingPlayer = sessionStorage.getItem('tab4_player');
if (_pendingPlayer) {
    sessionStorage.removeItem('tab4_player');
    const _p = PLAYERS.find(p => p.name === _pendingPlayer);
    if (_p) setTimeout(() => openDetail(_p), 80);
}
```

### 从球员详情 → 球队详情 (Tab4 → Tab3)

```javascript
// 球员详情页国旗/国家名点击（tab4.html openDetail 函数中）
const flagEl = document.getElementById('dBigFlag');
flagEl.style.cursor = 'pointer';
flagEl.onclick = () => {
    sessionStorage.setItem('tab3_team', p.cc);  // p.cc = 国家代码
    window.location.href = 'tab3.html';
};
```

### 从今日比赛 → 赛程日期 (Tab1 → Tab2)

```javascript
// 发送方 (tab1.html buildToday 卡片, onclick)
function goSchedule(date) {
    sessionStorage.setItem('tab2_date', date);   // date 格式: 'Jun17', 'Jun13', ...
    window.location.href = 'tab2.html';
}

// 接收方 (tab2.html script 末尾)
const _pendingDate = sessionStorage.getItem('tab2_date');
if (_pendingDate) {
    sessionStorage.removeItem('tab2_date');
    setTimeout(() => {
        const chip = document.querySelector(`[data-date="${_pendingDate}"]`);
        if (chip) goDate(_pendingDate, chip);   // 滚动到该日期 + 更新 chip 高亮
    }, 100);
}
```

TODAY_CARDS 日期映射:

| 主客队 | 日期 key |
|--------|---------|
| USA vs Panama | `Jun17` |
| Argentina vs Canada | `Jun13` |
| France vs Morocco | `Jun17` |
| Brazil vs Colombia | `Jun17` |
| Spain vs Albania | `Jun14` |

### Key 命名规范

| Key | 值类型 | 生命周期 |
|-----|--------|---------|
| `tab3_team` | 球队 country code (如 `"fr"`, `"gb-eng"`) | 一次性，读后立即删除 |
| `tab4_player` | 球员缩写名 (如 `"K. Mbappé"`) | 一次性，读后立即删除 |
| `tab2_date` | 日期 key (如 `"Jun17"`) | 一次性，读后立即删除 |

**注意**: tab4 中 `PLAYERS[].name` 字段与 tab3 阵容中显示的缩写名完全一致，两侧直接 `===` 比较即可。

---

## 12. 构建 & 发布

### Debug APK

```powershell
cd "D:\Dev\WC26\App"
.\gradlew assembleDebug
# 输出: app\build\outputs\apk\debug\app-debug.apk (~25.4 MB)
```

### Release APK（待配置）

1. 创建 keystore 文件
2. 在 `app/build.gradle.kts` 中配置 `signingConfigs`
3. 启用 minification: `isMinifyEnabled = true`
4. 运行 `.\gradlew assembleRelease`

### 已知构建警告（可忽略）

```
Warning: SDK processing. This version only understands SDK XML versions up to 3
but an SDK XML file of version 4 was encountered.
```
原因: Android Studio 与命令行工具版本不完全一致，不影响构建结果。

```
w: This annotation is currently applied to the value parameter only...
```
原因: Kotlin 2.3.20 对 `@DrawableRes` 注解的处理方式变化（AppScaffold.kt line 23），警告不影响功能，后续可加 `-Xannotation-default-target=param-property` 编译器参数消除。

---

## 13. 待办 / 后续迭代

### 高优先级

- [ ] **players.json**: 目前球员数据硬编码在 `PlayerData.kt`（20人），后续应生成完整 JSON 并通过 `DataRepository` 加载，与 teams.json 保持一致
- [ ] **Release 签名配置**: 配置 keystore 并测试 Release 构建
- [ ] **minification 开启**: Release 包未启用代码混淆/缩减

### 功能扩展

- [ ] **球员与球队双向关联**: 目前 TeamsScreen 详情的球员列表仅显示 PlayerData 中的 20 人，非完整阵容；需要完整球员数据
- [ ] **Android 跨 Tab 联动**: HTML 版本实现了 Tab 间 deep link（Tab1/2 → Tab3 → Tab4），Android 版本目前各 Tab 独立，无跨 Tab 联动
- [ ] **比赛状态实时更新**: 目前 `status` 字段为静态数据，无网络更新
- [ ] **场馆地图**: ScheduleScreen 中有地图相关 Intent，HTML 版有地图链接，Android 版未做完整实现
- [ ] **日历功能 (Android)**: ScheduleScreen 已有日历 Intent 代码
- [ ] **搜索历史 / 收藏**: 无此功能
- [ ] **多语言**: 目前仅英文
- [ ] **暗色模式**: 目前仅亮色

### 技术债

- [ ] 统一 `cc`（country code）与 `flagFile`（country name）的映射，目前在多处手动 hardcode（PlayerData、PlayersScreen、TeamsScreen）
- [ ] `navigation-compose` 依赖已引入但未使用，可删除以减小 APK 体积
- [ ] `PlayerData.kt` 中的 `clubCC` → flagFile 映射（`clubCCToFlagFile`）在 PlayersScreen 内部定义，应提取到 Common.kt 或 PlayerData.kt
- [ ] `Data\vanue.csv` 文件名拼写错误（vanue → venue），建议修正
- [ ] HTML 原型与 Android App 的数据字段未完全统一（如 HTML 中 `team.code` vs Android 中 `team.id`）

---

*本文件由 Claude Code 于 2026-05-27 根据项目完成状态自动生成。*
