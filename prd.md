Tab 1 — Standings Screen: Native Android 需求文档

 Context

 基于 UI/tab1.html 设计原型，构建 FIFA World Cup 2026 Native Android App 的 Tab
  1（Standings 积分榜）页面。
 数据来自 data/ 目录下的 CSV 文件，图片资源来自 pic/ 目录（全部为本地 PNG）。
 目标：完整还原原型中的所有设计要素和交互，同时在视觉层面对标主流 iOS 体育类
 App（ESPN、Apple Sports、One Football）的精致度。

 ---
 一、真实数据概览

 1.1 实际分组（来自 team.csv）

 ┌─────┬─────────────────────────────────────────────────┬─────────────────┐
 │ 组  │                      队伍                       │    FIFA 排名    │
 ├─────┼─────────────────────────────────────────────────┼─────────────────┤
 │ A   │ Mexico · South Korea · Czechia · South Africa   │ 15 · 25 · 41 ·  │
 │     │                                                 │ 60              │
 ├─────┼─────────────────────────────────────────────────┼─────────────────┤
 │ B   │ Switzerland · Canada · Qatar · Bosnia &         │ 14 · 30 · 35 ·  │
 │     │ Herzegovina                                     │ 65              │
 ├─────┼─────────────────────────────────────────────────┼─────────────────┤
 │ C   │ Brazil · Morocco · Scotland · Haiti             │ 6 · 8 · 47 · 83 │
 ├─────┼─────────────────────────────────────────────────┼─────────────────┤
 │ D   │ USA · Australia · Türkiye · Paraguay            │ 16 · 24 · 42 ·  │
 │     │                                                 │ 64              │
 ├─────┼─────────────────────────────────────────────────┼─────────────────┤
 │ E   │ Germany · Ecuador · Ivory Coast · Curaçao       │ 10 · 28 · 33 ·  │
 │     │                                                 │ 81              │
 ├─────┼─────────────────────────────────────────────────┼─────────────────┤
 │ F   │ Netherlands · Japan · Sweden · Tunisia          │ 7 · 17 · 39 ·   │
 │     │                                                 │ 40              │
 ├─────┼─────────────────────────────────────────────────┼─────────────────┤
 │ G   │ Belgium · Iran(W) · Egypt · New Zealand         │ 9 · 18 · 29 ·   │
 │     │                                                 │ 95              │
 ├─────┼─────────────────────────────────────────────────┼─────────────────┤
 │ H   │ Spain · Uruguay · Saudi Arabia · Cape Verde     │ 2 · 13 · 57 ·   │
 │     │                                                 │ 70              │
 ├─────┼─────────────────────────────────────────────────┼─────────────────┤
 │ I   │ France · Senegal · Norway · Iraq                │ 1 · 21 · 44 ·   │
 │     │                                                 │ 61              │
 ├─────┼─────────────────────────────────────────────────┼─────────────────┤
 │ J   │ Argentina · Austria · Algeria · Jordan          │ 3 · 23 · 36 ·   │
 │     │                                                 │ 68              │
 ├─────┼─────────────────────────────────────────────────┼─────────────────┤
 │ K   │ Portugal · Colombia · DR Congo · Uzbekistan     │ 5 · 15 · 51 ·   │
 │     │                                                 │ 62              │
 ├─────┼─────────────────────────────────────────────────┼─────────────────┤
 │ L   │ England · Croatia · Panama · Ghana              │ 4 · 11 · 53 ·   │
 │     │                                                 │ 65              │
 └─────┴─────────────────────────────────────────────────┴─────────────────┘

 1.2 淘汰赛阶段

 - Round of 32（M073–M088）：2026-06-28 ~ 07-03
 - Round of 16（M089–M096）：2026-07-04 ~ 07-07
 - Quarterfinal（M097–M100）：2026-07-09 ~ 07-11
 - Semifinal（M101–M102）：2026-07-14 ~ 07-15
 - Third Place（M103）：2026-07-18
 - Final（M104）：2026-07-19，MetLife Stadium NJ

 1.3 特殊情况

 - Iran 已退赛（T26 status=Withdrawn）：涉及 3 场比赛（M016/M038/M065）标注 TBD
 - 世界杯首次参赛：Curaçao · Cape Verde · Jordan · Uzbekistan

 1.4 本地图片资源路径规则

 ┌────────────────────────┬──────────────┬─────────────────┬────────────────┐
 │          用途          │     目录     │    命名规则     │      示例      │
 ├────────────────────────┼──────────────┼─────────────────┼────────────────┤
 │ 国家队国旗（扁矩形）   │ pic/Country/ │ [国家名].png    │ Mexico.png     │
 ├────────────────────────┼──────────────┼─────────────────┼────────────────┤
 │ 国家队队徽（圆形徽章） │ pic/Team/    │ [国家名].png    │ Mexico.png     │
 ├────────────────────────┼──────────────┼─────────────────┼────────────────┤
 │ 球员头像               │ pic/Player/  │ [ID]_[姓名].png │ 231747_Kylian  │
 │                        │              │                 │ Mbappé.png     │
 ├────────────────────────┼──────────────┼─────────────────┼────────────────┤
 │ 俱乐部 logo            │ pic/Club/    │ [俱乐部名].png  │ Real           │
 │                        │              │                 │ Madrid.png     │
 └────────────────────────┴──────────────┴─────────────────┴────────────────┘
▎ 命名差异注意：Team 目录中部分名称与 team.csv 不同（如 Ivory Coast vs Côte
 ▎ D'Ivoire、DR Congo vs Congo Dr、South Korea vs Korea
 ▎ Republic）。需在数据层维护映射表。
,
-
 ---
 二、屏幕整体架构

 2.1 布局层次

 Activity (edge-to-edge, WindowInsets 处理)
 └── StandingsScreen (Composable)
     ├── TopAppBar          ← 固定，NavBar 区域
     ├── SelectorBar        ← 固定粘附于 TopAppBar 下
     └── MainContent        ← LazyColumn，占满剩余高度
         ├── TodayMatchesSection  ← 仅当日有赛事时显示
         └── GroupContent / KoContent  ← 由 Stage 决定

 2.2 技术栈

 ┌──────────┬─────────────────────────────────────────────────────┐
 │   模块   │                        选型                         │
 ├──────────┼─────────────────────────────────────────────────────┤
 │ UI 框架  │ Jetpack Compose (BOM latest)                        │
 ├──────────┼─────────────────────────────────────────────────────┤
 │ 状态管理 │ ViewModel + StateFlow                               │
 ├──────────┼─────────────────────────────────────────────────────┤
 │ 图片加载 │ Coil 3 (Compose extension，支持本地 File URI)       │
 ├──────────┼─────────────────────────────────────────────────────┤
 │ CSV 解析 │ 手动 BufferedReader（避免引入重型库）               │
 ├──────────┼─────────────────────────────────────────────────────┤
 │ 导航     │ Navigation Compose                                  │
 ├──────────┼─────────────────────────────────────────────────────┤
 │ 动画     │ spring()、Animatable、AnimatedVisibility            │
 ├──────────┼─────────────────────────────────────────────────────┤
 │ 字体     │ Inter（Google Fonts，比 Roboto 更接近 SF Pro 质感） │
 ├──────────┼─────────────────────────────────────────────────────┤
 │ 最低 API │ 26（Android 8.0）；blur 效果 API 31+                │
 ├──────────┼─────────────────────────────────────────────────────┤
 │ 依赖注入 │ Hilt                                                │
 └──────────┴─────────────────────────────────────────────────────┘

 ---
 三、设计系统（Design Tokens）

 3.1 颜色

 object WC26Colors {
     // Backgrounds
     val BgPage         = Color(0xFFF2F2F7)   // 页面背景，iOS 浅灰
     val Surface        = Color(0xFFFFFFFF)   // 卡片/导航栏
     val SurfaceElevated= Color(0xFFF9F9F9)   // 表头行

     // Accent
     val Blue           = Color(0xFF007AFF)   // 主色，选中态
     val Green          = Color(0xFF34C759)   // 晋级/胜利
     val Red            = Color(0xFFFF3B30)   // 实时/负数
     val Orange         = Color(0xFFFF9500)   // 警告/中段排名
     val GoldDark       = Color(0xFFB8860B)   // 金色装饰（冠军）

     // Text
     val Label          = Color(0xFF000000)
     val Label60        = Color(0x99000000)   // 60% 透明度
     val Label30        = Color(0x4D3C3C43)   // 30% 透明度
     val GrayUI         = Color(0xFF8E8E93)

     // Separator
     val Separator      = Color(0x1F3C3C43)   // 12%

     // FIFA Rank bar gradient stops
     val RankTop        = Color(0xFF007AFF)   // rank ≤ 15
     val RankHigh       = Color(0xFF34C759)   // rank ≤ 40
     val RankMid        = Color(0xFFFF9500)   // rank ≤ 80
     val RankLow        = Color(0xFF8E8E93)   // rank > 80
 }

 3.2 字体（Inter）

 ┌──────────────┬─────────┬─────────────────────────┐
 │     用途     │  Size   │         Weight          │
 ├──────────────┼─────────┼─────────────────────────┤
 │ NavTitle     │ 17sp    │ SemiBold (600)          │
 ├──────────────┼─────────┼─────────────────────────┤
 │ SectionTitle │ 20sp    │ Bold (700)              │
 ├──────────────┼─────────┼─────────────────────────┤
 │ TableHeader  │ 11sp    │ Medium (500), Uppercase │
 ├──────────────┼─────────┼─────────────────────────┤
 │ TeamName     │ 13.5sp  │ Medium (500)            │
 ├──────────────┼─────────┼─────────────────────────┤
 │ Score (大)   │ 22sp    │ Bold (700)              │
 ├──────────────┼─────────┼─────────────────────────┤
 │ Score (KO)   │ 24sp    │ Bold (700)              │
 ├──────────────┼─────────┼─────────────────────────┤
 │ PtsValue     │ 14sp    │ Bold (700)              │
 ├──────────────┼─────────┼─────────────────────────┤
 │ StatValue    │ 13sp    │ Normal (400)            │
 ├──────────────┼─────────┼─────────────────────────┤
 │ Label (小)   │ 10~11sp │ SemiBold (600)          │
 └──────────────┴─────────┴─────────────────────────┘

 3.3 圆角

 ┌───────────────────┬───────────┐
 │       元素        │  Radius   │
 ├───────────────────┼───────────┤
 │ 积分表容器        │ 16dp      │
 ├───────────────────┼───────────┤
 │ Today 卡片        │ 18dp      │
 ├───────────────────┼───────────┤
 │ KO 比赛卡片       │ 16dp      │
 ├───────────────────┼───────────┤
 │ Stage 下拉菜单    │ 14dp      │
 ├───────────────────┼───────────┤
 │ Stage Pill        │ 14dp      │
 ├───────────────────┼───────────┤
 │ Chip（横向）      │ 22dp 胶囊 │
 ├───────────────────┼───────────┤
 │ 国旗（矩形）      │ 3dp       │
 ├───────────────────┼───────────┤
 │ 国旗/队徽（圆形） │ 50%       │
 ├───────────────────┼───────────┤
 │ FIFA 排名 bar     │ 2dp       │
 └───────────────────┴───────────┘

 3.4 阴影 / Elevation

 ┌────────────────┬────────────────────────────────────┐
 │      元素      │             Elevation              │
 ├────────────────┼────────────────────────────────────┤
 │ NavBar         │ 0（仅底部 Divider）                │
 ├────────────────┼────────────────────────────────────┤
 │ 积分表、KO 卡  │ 1dp（subtle shadow，color tinted） │
 ├────────────────┼────────────────────────────────────┤
 │ Today 卡片     │ 3dp                                │
 ├────────────────┼────────────────────────────────────┤
 │ Stage 下拉菜单 │ 12dp                               │
 ├────────────────┼────────────────────────────────────┤
 │ TabBar         │ 0（仅顶部 Divider）                │
 └────────────────┴────────────────────────────────────┘

 ---
 四、Nav Bar 组件

 4.1 结构

 [ 32dp spacer ] [ "FIFA World Cup 2026" ] [ StagePill ]

 - 高度：56dp（含 StatusBar 之下起算）
 - 背景：Surface，底部 Divider(thickness=0.5dp, color=Separator)
 - 标题居中，17sp SemiBold，letterSpacing -0.4sp
 - 左侧：预留 32dp spacer（未来扩展返回键）

 4.2 Stage Pill（阶段选择器）

 外观
 - 背景：rgba(120,120,128,0.12) 胶囊，radius 14dp
 - 内边距：vertical 6dp，horizontal 10dp
 - 文字：12sp SemiBold，Blue (#007AFF)
 - 右侧 chevron SVG（10×6px），蓝色，随菜单展开旋转 180°

 交互
 - 点击：scale 0.93 + spring 回弹（spring(dampingRatio=0.5f)）
 - 展开下拉菜单（见 4.3）
 - 当前选中阶段更新文字："Group Stage" / "Knockout Stage"

 4.3 Stage 下拉菜单

 外观
 - 定位：Pill 右下角弹出，offset(y=8dp)
 - 容器：Surface(tonalElevation=12dp)，圆角 14dp，最小宽度 200dp
 - API 31+ 背景加高斯模糊（RenderEffect），低版本纯白
 - 阴影：BoxShadow 0 4dp 24dp rgba(0,0,0,0.15)

 选项行
 - 高度 48dp，左 padding 16dp，右 padding 16dp
 - 字体 15sp；选中行：SemiBold，蓝色 + 右侧 ✓ 图标
 - 行间 Divider 0.5dp
 - 点击：ripple → 选中 → 菜单收起（alpha+scale 动画）

 动画
 - 展开：ScaleIn(transformOrigin=TopEnd) + FadeIn，120ms，EaseOutBack
 - 收起：ScaleOut + FadeOut，100ms

 ---
 五、Selector Bar（阶段 Chip 栏）

 5.1 结构

 - 粘附于 NavBar 下方（stickyHeader in LazyColumn，或独立 Box）
 - 背景：Surface.copy(alpha=0.92f) + blur（API 31+）
 - 底部 Divider 0.5dp
 - 高度自适应内容（约 52dp）

 5.2 Chip

 - Group Stage：12 个，文字 "Group A" ~ "Group L"，宽 80dp，高 34dp，字 14sp
 - Knockout Stage：6 个，R32 · R16 · QF · SF · 3rd · Final，宽 60dp，字 13sp
 - 圆角 22dp，间距 8dp，左 padding 16dp

 状态

 ┌──────┬────────────────────┬─────────┬──────────┐
 │ 状态 │        背景        │ 文字色  │   字重   │
 ├──────┼────────────────────┼─────────┼──────────┤
 │ 默认 │ 透明               │ Label60 │ Medium   │
 ├──────┼────────────────────┼─────────┼──────────┤
 │ 选中 │ Blue               │ White   │ SemiBold │
 ├──────┼────────────────────┼─────────┼──────────┤
 │ 按下 │ scale 0.90，spring │ —       │ —        │
 └──────┴────────────────────┴─────────┴──────────┘

 联动逻辑
 - 点击 Chip[X] → LazyListState.animateScrollToItem(index) → Chip[X] 在 Chip
 栏水平居中
 - 内容区滚动 → LazyListState.firstVisibleItemIndex 变化 → 对应 Chip 高亮 +
 居中

 ---
 六、Today's Matches 横向卡片区

 6.1 显示条件

 数据层判断当天（LocalDate.now()）是否有比赛，无则隐藏整块。

 6.2 容器

 - 背景 BgPage
 - Eyebrow 标签："TODAY'S MATCHES"，11sp SemiBold，uppercase，Label30
 - LazyRow，PagerSnapHelper（每次吸附一张）
 - 左 padding 16dp，item 间距 10dp

 6.3 比赛卡片规格

 尺寸：宽 170dp，固定高 auto，圆角 18dp，Surface，elevation 3dp

 卡片内布局

 ┌──────────────────────────────────┐
 │ [● LIVE 74']           [Group A] │  ← Row，top pad 11dp，h/v 12dp
 │                                  │
 │  [队徽圆形]  [2 : 1]  [队徽圆形] │  ← Row，队徽 40dp 圆形
 │  USA          vs/score  Panama   │
 │  [队名]                [队名]    │  ← 11sp，居中
 └──────────────────────────────────┘

 ▎ Tab 1 today 卡片使用圆形队徽（pic/Team/），比原型的国旗更具辨识度。

 状态标签

 ┌───────────┬───────────────────────────────────────────────────────┐
 │   状态    │                         样式                          │
 ├───────────┼───────────────────────────────────────────────────────┤
 │ LIVE      │ 红色脉冲圆点（5dp） + "74'" 文字，10.5sp SemiBold Red │
 ├───────────┼───────────────────────────────────────────────────────┤
 │ Full Time │ "FT" 灰色，10.5sp                                     │
 ├───────────┼───────────────────────────────────────────────────────┤
 │ Upcoming  │ 时间字符串如 "21:00"，蓝色，10.5sp                    │
 └───────────┴───────────────────────────────────────────────────────┘

 脉冲动画（LIVE 圆点）
 val scale by rememberInfiniteTransition().animateFloat(
     1f, 0.65f, InfiniteRepeatableSpec(tween(700), RepeatMode.Reverse)
 )
 // alpha 同步：1f → 0.35f

 分数区
 - LIVE/FT："2" ":" "1"，22sp Bold，间距 4dp
 - Upcoming："vs" 14sp SemiBold Label30

 点击：预留 → MatchDetailScreen（NavController.navigate）

 ---
 七、Group Stage 内容区

 7.1 LazyColumn 结构

 每个 Group Section 作为一个 item { } 块：
 - Section Header
 - 积分表（白色卡片）
 - 图例（Qualify 指示）
 - 组间分隔 Spacer(6dp)（最后一组无）

 7.2 Section Header

 Group A                              Round 1
 - 左：20sp Bold，letterSpacing -0.5sp
 - 右：当前轮次文字（Round 1 / 2 / 3 / Complete），12sp Label30
 - padding: top 20dp，bottom 10dp，horizontal 16dp

 附加徽章（视觉增强）：若本组有进行中的比赛，section header 右侧显示红色 LIVE
 pill（8dp border-radius，10sp）。

 7.3 积分表容器

 - Surface，圆角 16dp，elevation 1dp
 - margin 水平 14dp
 - 溢出 clip(RoundedCornerShape(16.dp))

 7.4 表头行

 [pos 14dp] [flag 26dp] [Team flex:1] [RK 30dp] [P 22dp] [W 22dp] [D 22dp] [L
 22dp] [GD 26dp] [Pts 24dp] [chev 11dp]

 - 背景 SurfaceElevated (#F9F9F9)
 - 所有列标题：11sp Medium Uppercase Label30
 - Pts 列标题：Label60（稍加强）
 - 高度 36dp，水平 padding 12dp

 7.5 数据行

 高度：56dp（比原型略高，适应排名 bar + 队名两行）

 列定义

 ┌──────────────┬─────────────────────────────────────────┬────────┬────────┐
 │      列      │                  内容                   │  宽度  │  对齐  │
 ├──────────────┼─────────────────────────────────────────┼────────┼────────┤
 │ 晋级竖条     │ 2.5dp × 34dp，Green，position=Absolute  │ —      │ —      │
 │              │ left=0                                  │        │        │
 ├──────────────┼─────────────────────────────────────────┼────────┼────────┤
 │ 排名数字     │ 1/2/3/4，11.5sp Label30                 │ 14dp   │ center │
 ├──────────────┼─────────────────────────────────────────┼────────┼────────┤
 │ 国旗         │ pic/Country/，26×18dp，radius 3dp       │ 26dp   │ center │
 ├──────────────┼─────────────────────────────────────────┼────────┼────────┤
 │ 队名+排名bar │ flex:1                                  │ —      │ start  │
 ├──────────────┼─────────────────────────────────────────┼────────┼────────┤
 │ FIFA #       │ "#15"，11sp Label30                     │ 30dp   │ end    │
 ├──────────────┼─────────────────────────────────────────┼────────┼────────┤
 │ P W D L      │ 13sp Label60                            │ 22dp×4 │ center │
 ├──────────────┼─────────────────────────────────────────┼────────┼────────┤
 │ GD           │ 正数 Green，负数 Red，13sp SemiBold     │ 26dp   │ center │
 ├──────────────┼─────────────────────────────────────────┼────────┼────────┤
 │ Pts          │ 14sp Bold Label                         │ 24dp   │ center │
 ├──────────────┼─────────────────────────────────────────┼────────┼────────┤
 │ >            │ 7dp arrow，opacity 0.2                  │ 11dp   │ center │
 └──────────────┴─────────────────────────────────────────┴────────┴────────┘

 队名+排名bar 子列
 Mexico
 [===---] rank bar，宽 max 36dp，高 3dp，颜色按 FIFA rank 分档
 - 排名 bar 宽度公式：max(4, (1 - (rank-1)/199f) * 36).dp
 - 颜色档：rank≤15 Blue；≤40 Green；≤80 Orange；>80 Gray

 晋级竖条
 - 前 2 名（qualify → R32）：Green #34C759
 - 第 3 名（当有资格的 best third 时）：用 Orange，初期不确定则不显示

 行交互
 - Ripple 效果
 - 点击 → TeamDetailScreen(teamId)（预留）

 行分割线：0.5dp Separator，最后一行不显示

 7.6 图例

 [■ 绿竖条] Qualify to R32     [? 橙竖条] Best 3rd (TBD)
 - Row，padding bottom 8dp，horizontal 14dp，字体 10.5sp Label30

 ---
 八、Knockout Stage 内容区

 8.1 LazyColumn 结构

 6 个 Round Section：R32 · R16 · QF · SF · 3rd · Final

 8.2 Section Header

 Round of 32                        Jun 28 – Jul 3
 同 Group，20sp Bold + 右侧日期 12sp Label30。

 8.3 比赛卡片（KO Card）

 容器：Surface，圆角 16dp，elevation 1dp，margin 水平 14dp，下 10dp

 卡片头部（padding 10dp horizontal，top 10dp）
 ROUND OF 32                          Full Time / ● 67' / Jun 29 · 22:00
 - 左：10.5sp SemiBold Uppercase Label30
 - 右：状态标签（样式同 Today 卡片）

 队伍行 (Row，padding 12dp，vertically centered)

 [旗徽]    [名字]    [FIFA排名]    ||  [分数/时间]  ||    [FIFA排名]    [名字]
    [旗徽]

 队伍列（koc-team）结构（Column，width=fillMaxWidth(fraction=0.38f)）：
 - 旗帜：pic/Country/，32×22dp，radius 3dp（若已确定队伍）
 - 队名：13sp，居中，最大 2 行截断，winner → Bold；loser → Label30 + alpha 0.6
 - FIFA 排名：10sp Label30

 Winner 视觉
 - 旗帜：border(1.5dp, Green)
 - 队名：14sp Bold
 - Loser：旗帜 alpha=0.55f，队名 Label30

 TBD 队伍：旗帜位置显示 48dp 圆形灰色占位（shimmer 动画），队名显示 "TBD"

 中央分数列 (Column，width=72dp，horizontalAlignment=Center)

 ┌──────────┬───────────────────────────────────────────────────┐
 │   状态   │                       内容                        │
 ├──────────┼───────────────────────────────────────────────────┤
 │ Upcoming │ 时间 20sp SemiBold + 日期 10sp Label30            │
 ├──────────┼───────────────────────────────────────────────────┤
 │ Live     │ 比分 2 : 1（24sp Bold Red）+ 分钟 "67'" 9.5sp Red │
 ├──────────┼───────────────────────────────────────────────────┤
 │ FT       │ 比分，winner 侧正常，loser 侧 Label30             │
 └──────────┴───────────────────────────────────────────────────┘

 点球脚注（仅点球决胜时）
 [⏱] Won on penalties · 4-2
 - Row，顶部 Divider 0.5dp，padding 8dp，字体 10.5sp Label60，居中

 点击：→ MatchDetailScreen(matchId)（预留）

 ---
 九、Tab Bar

 4 个 Tab，固定底部，高度 56dp + navigationBarInsets：

 ┌──────┬────────────┬───────────┬──────┐
 │ 序号 │    图标    │   标签    │ 当前 │
 ├──────┼────────────┼───────────┼──────┤
 │ 1    │ 三横线列表 │ Standings │ 是   │
 ├──────┼────────────┼───────────┼──────┤
 │ 2    │ 日历       │ Schedule  │ —    │
 ├──────┼────────────┼───────────┼──────┤
 │ 3    │ 盾牌       │ Teams     │ —    │
 ├──────┼────────────┼───────────┼──────┤
 │ 4    │ 人物       │ Players   │ —    │
 └──────┴────────────┴───────────┴──────┘

 - 背景 Surface.copy(alpha=0.92f) + blur（API 31+）
 - 顶部 Divider 0.5dp
 - 图标 24dp，标签 10sp Medium
 - 激活：Blue；非激活：GrayUI
 - 点击 scale 0.85 → spring 回弹

 ---
 十、视觉增强细节（超越原型的改进点）

 10.1 Today 卡片 — 队徽替代原型国旗

 原型用小圆形国旗；改用 pic/Team/
 圆形队徽（40dp）——更具辨识度，视觉层次更丰富。

 10.2 Group Header — LIVE 红色胶囊

 若本组当前有进行中比赛，Section Header 行内动态显示 ● LIVE
 红色胶囊，给用户实时感知。

 10.3 积分表 — 渐变晋级区背景

 前 2 名行：背景叠加 Green.copy(alpha=0.04f) 极淡绿色渐变，强化晋级感知。

 10.4 排名 bar 过渡动画

 列表首次渲染时，排名 bar 从 0 宽度到目标宽度，tween(600ms,
 easing=EaseOutCubic)，错峰 delay(index * 30ms)。

 10.5 Stage 切换 — 内容区渐出渐入

 切换 Group ↔ Knockout 时：旧内容 FadeOut(100ms)，新内容 FadeIn(200ms) +
 SlideIn(y=16dp)。

 10.6 KO 卡片 — 胜负队伍视觉对比增强

 Winner 旗帜加绿色光晕（Modifier.shadow(4dp, shape=CircleShape,
 ambientColor=Green)），Loser
 旗帜灰度滤镜（ColorFilter.colorMatrix(ColorMatrix().apply {
 setSaturation(0.3f) })）。

 10.7 首次进入 — Stagger 动画

 Group Section 首次渲染：每个 Section 错峰 FadeIn + TranslateY(20dp→0)，delay
 递增 40ms * groupIndex。

 10.8 Section 内比赛数量角标

 Group Header 右上角小角标显示本组今日比赛场数（如 "2 Today"）；Knockout Header
  显示已完成场数进度（如 "4/16 Played"）。

 ---
 十一、数据层设计

 11.1 数据模型

 data class WcTeam(
     val teamId: String,          // "T01"
     val name: String,            // "Mexico"
     val group: String,           // "A"
     val confederation: String,
     val fifaRank: Int,
     val isWorldCupDebut: Boolean,
     val isWithdrawn: Boolean,
     val picTeamPath: String,     // "pic/Team/Mexico.png"
     val picCountryPath: String   // "pic/Country/Mexico.png"
 )

 data class GroupEntry(
     val team: WcTeam,
     val played: Int = 0,
     val won: Int = 0,
     val drawn: Int = 0,
     val lost: Int = 0,
     val goalsFor: Int = 0,
     val goalsAgainst: Int = 0,
     val goalDiff: Int = 0,
     val points: Int = 0
 )

 data class WcMatch(
     val matchNum: String,        // "M001"
     val stage: MatchStage,
     val matchday: Int?,
     val group: String?,
     val date: LocalDate,
     val timeEdt: String,
     val team1: WcTeam?,
     val team2: WcTeam?,
     val label1: String,          // For KO: "1A vs 2B" style labels
     val label2: String,
     val venueId: String,
     val status: MatchStatus,
     val score1: Int? = null,
     val score2: Int? = null,
     val liveMinute: String? = null,
     val penaltyScore: String? = null
 )

 enum class MatchStage { GROUP, R32, R16, QF, SF, THIRD, FINAL }
 enum class MatchStatus { UPCOMING, LIVE, FINISHED }

 11.2 CSV 解析

 object CsvParser {
     fun parseTeams(context: Context): List<WcTeam>
     fun parseMatches(context: Context, teams: Map<String, WcTeam>):
 List<WcMatch>
 }

 - Assets 文件放在 assets/data/ 目录下
 - 首次启动解析并缓存到 Room（可选）；演示阶段可直接内存持有

 11.3 团队名→图片路径映射

 val TEAM_ASSET_MAP = mapOf(
     "Ivory Coast" to "Ivory Coast",
     "DR Congo" to "DR Congo",
     "South Korea" to "South Korea",
     "Czechia" to "Czech Republic",
     "Bosnia & Herzegovina" to "Bosnia and Herzegovina",
     "Türkiye" to "Turkey",
     "Curaçao" to "Curacao",
     // ... 完整映射
 )

 11.4 ViewModel 状态

 data class StandingsUiState(
     val stage: TournamentStage = TournamentStage.GROUP,
     val groups: List<GroupStanding> = emptyList(),
     val koRounds: List<KoRound> = emptyList(),
     val todayMatches: List<WcMatch> = emptyList(),
     val selectedGroupKey: String = "A",
     val selectedKoRound: String = "R32",
     val isLoading: Boolean = true,
     val error: String? = null
 )

 enum class TournamentStage { GROUP, KNOCKOUT }

 ---
 十二、Compose 组件树

 StandingsScreen
 ├── StandingsTopBar(onStageClick)
 │   └── StagePill(currentStage, onClick)
 │       └── StageDropdownMenu(visible, onSelect, onDismiss)
 ├── SelectorBar(chips, selectedIndex, onChipClick)
 │   └── LazyRow { ChipItem(...) }
 └── LazyColumn(state=listState)
     ├── item { TodayMatchesSection(matches) }
     │   └── LazyRow { TodayMatchCard(match) }
     └── items(groups or koRounds) {
         ├── GroupSection(group)         // Group Stage
         │   ├── GroupSectionHeader
         │   ├── GroupTable
         │   │   ├── TableHeaderRow
         │   │   └── TeamRows (x4)
         │   └── GroupLegend
         └── KoRoundSection(round)       // KO Stage
             ├── KoRoundHeader
             └── KoMatchCards
                 └── KoMatchCard(match)
                     ├── KoCardHeader
                     ├── KoTeamsRow
                     │   ├── KoTeamColumn(team, isWinner, isLoser)
                     │   ├── KoCenterScore
                     │   └── KoTeamColumn(team, isWinner, isLoser)
                     └── KoPenaltyFooter? (if penalties)
     }

 ---
 十三、文件结构

 app/src/main/
 ├── assets/
 │   └── data/
 │       ├── match.csv
 │       ├── team.csv
 │       └── vanue.csv
 ├── java/.../wc2026/
 │   ├── data/
 │   │   ├── CsvParser.kt
 │   │   ├── model/           (WcTeam, WcMatch, GroupEntry...)
 │   │   ├── repository/      (StandingsRepository)
 │   │   └── TeamAssetMap.kt
 │   ├── ui/
 │   │   ├── standings/
 │   │   │   ├── StandingsScreen.kt
 │   │   │   ├── StandingsViewModel.kt
 │   │   │   ├── components/
 │   │   │   │   ├── TopBar.kt
 │   │   │   │   ├── SelectorBar.kt
 │   │   │   │   ├── TodayMatchCard.kt
 │   │   │   │   ├── GroupSection.kt
 │   │   │   │   ├── GroupTable.kt
 │   │   │   │   ├── KoSection.kt
 │   │   │   │   └── KoMatchCard.kt
 │   │   │   └── animations/
 │   │   │       └── StaggeredFadeIn.kt
 │   │   └── theme/
 │   │       ├── Color.kt
 │   │       ├── Type.kt
 │   │       └── Theme.kt
 │   └── MainActivity.kt
 └── res/
     └── drawable/
         └── ic_tab_*.xml  (4 tab icons, vector)

 ▎ pic/ 目录（3.78 GB）以 File 路径直接读取，不放入 assets（过大）。
 ▎ Coil 使用 ImageRequest.Builder(context).data(File(picDir, filename))
 ▎ 加载本地文件。

 ---
 十四、交互流程完整说明

 Stage 切换

 点击 StagePill
   → 展开 StageDropdownMenu（scale+fade in）
   → 选择 "Group Stage" 或 "Knockout Stage"
     → ViewModel.setStage(newStage)
     → SelectorBar chips 重新渲染（新 Stage 的 chips）
     → 主内容区 FadeOut → FadeIn（新内容）
     → LazyListState 滚动回顶部

 Chip → 内容锚点跳转

 点击 Chip[i]
   → ViewModel.selectIndex(i)
   → LazyListState.animateScrollToItem(offsetIndex)
     (Today section 占 1 个 item，Groups 从 index 1 开始)
   → Chip[i] 选中高亮
   → ChipRow.animateScrollToItem(i) 使 chip 居中

 内容滚动 → Chip 自动跟随

 LazyListState.firstVisibleItemIndex 变化
   → 计算当前可见 Group/Round index
   → ViewModel.selectedIndex 更新
   → Chip 高亮 + 居中（无动画，立即）

 ---
 十五、边界情况

 ┌──────────────────┬───────────────────────────────────────────────────────┐
 │       场景       │                         处理                          │
 ├──────────────────┼───────────────────────────────────────────────────────┤
 │ Iran 已退赛      │ 积分表中 Iran 行显示 "W/D" 角标；相关比赛显示 "TBD"   │
 ├──────────────────┼───────────────────────────────────────────────────────┤
 │ 当天无比赛       │ AnimatedVisibility(visible=false) 隐藏 TodaySection   │
 ├──────────────────┼───────────────────────────────────────────────────────┤
 │ 图片不存在       │ Coil error                                            │
 │                  │ placeholder：队旗→灰色圆角矩形，队徽→灰色圆形         │
 ├──────────────────┼───────────────────────────────────────────────────────┤
 │ TBD 队伍（KO）   │ 灰色占位 + shimmer 动画；队名 "TBD"                   │
 ├──────────────────┼───────────────────────────────────────────────────────┤
 │ 队名过长         │ overflow=Ellipsis，maxLines=1（表格行）/              │
 │                  │ maxLines=2（KO卡）                                    │
 ├──────────────────┼───────────────────────────────────────────────────────┤
 │ 世界杯首次参赛队 │ 队名后显示 ⭐ emoji 标记（可选，由                    │
 │                  │ world_cup_debut=Yes 驱动）                            │
 ├──────────────────┼───────────────────────────────────────────────────────┤
 │ 深色模式         │ 颜色系统支持 DarkTheme token（后续扩展，初期 Light    │
 │                  │ Only）                                                │
 └──────────────────┴───────────────────────────────────────────────────────┘

 ---
 十六、验证方案

 1. 布局验证：对照 UI/tab1.html 在浏览器中的视觉，截图比对 Android 渲染结果
 2. 数据验证：启动 App 后检查 Group A 四队（Mexico/South Korea/Czechia/South
 Africa）是否正确渲染，FIFA rank 对应（15/25/41/60）
 3. 图片加载：验证 pic/Team/Mexico.png 在 TodayCard 中正确显示圆形队徽
 4. Chip 联动：滚动内容区，验证 Chip 高亮自动跟随；点击 Chip 验证内容跳转
 5. Stage 切换：Group → Knockout，验证 Chip 重渲染为 R32/R16/QF/SF/3rd/Final
 6. LIVE 动画：在 TodayMatchCard 中造一个 LIVE 状态，验证圆点脉冲动画
 7. Iran 状态：验证 Group G 中 Iran 行的 Withdrawn 视觉处理

 ---
 实施顺序（供参考）

 1. Theme.kt + Color.kt + Type.kt — 设计系统基础
 2. CsvParser.kt + 数据模型 — 数据层
 3. StandingsViewModel.kt — 状态管理
 4. StandingsScreen.kt 骨架（TopBar + SelectorBar + LazyColumn）
 5. GroupSection.kt + GroupTable.kt — 小组赛积分表
 6. TodayMatchCard.kt — 今日比赛横滑区
 7. KoSection.kt + KoMatchCard.kt — 淘汰赛卡片
 8. 动画层：stagger、stage切换、live脉冲、rank bar 入场
 9. Chip ↔ 内容联动 scroll
 10. 边界情况（Iran/TBD/无图片）polish
