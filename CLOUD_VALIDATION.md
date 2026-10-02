# 云端验证记录

2026-10-02，已完成首轮设备反馈后的完整性收尾验证，覆盖编辑定位的顶栏避让和小数热量差值显示。

- 最新验证代码提交：`d89b50c96c091e65e37f1785a5368fb4e137c5c0`。
- [成功的工作流运行](https://github.com/Joshmax010/wedo-Android/actions/runs/36999425832)。
- [完整报告、JUnit XML、Lint 与构建日志](https://github.com/Joshmax010/wedo-Android/tree/codex/cloud-test-results/runs/36999425832)。
- 应用版本 1.4.3（versionCode 5）、Room v4、备份 Schema v3。

## 最新检查结果

```bash
bash gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug --max-workers=4 --console=plain
```

| 检查 | 结果 |
|---|---|
| 主代码编译，包括 Room KSP 与 Compose | 通过 |
| JVM 单元测试 | 143 个通过，0 失败、0 错误、0 跳过，13 个测试类 |
| Android Lint | 通过，0 错误、39 条既有警告，与清理阶段相比无新增警告 |
| Gradle 总结果 | BUILD SUCCESSFUL，3 分 21 秒 |

云端未打包 APK，未执行真机或模拟器测试。38 个 Room 设备用例及 UI 的实际显示、触摸、键盘、动画、系统导航适配待你本地检查，见 [统一回归清单](./LOCAL_REGRESSION_CHECKLIST.md)。

### UI 分批提交与验证

| 提交 | 范围 | 云端报告 |
|---|---|---|
| `5b75aa4` | 浅深色主题、外观偏好、公共字段颜色 | [121 个 JVM 用例通过](https://github.com/Joshmax010/wedo-Android/actions/runs/36858149297) |
| `8bc2497` | 滚动导航、大标题、首页信息层级和空状态边界 | [123 个 JVM 用例通过](https://github.com/Joshmax010/wedo-Android/actions/runs/36859897788) |
| `eaf8a4a` | 日期/餐次传递、历史编辑定位、周报摘要优先 | [125 个 JVM 用例通过](https://github.com/Joshmax010/wedo-Android/actions/runs/36861505927) |
| `fdb614d`、`f3b1301` | 设置子页、模板布局、保存与完成反馈、热量上限边界修正 | [136 个 JVM 用例通过](https://github.com/Joshmax010/wedo-Android/actions/runs/36864465400) |
| `44e6956` | 首轮设备反馈：稳定视口、五个子页导航、等宽餐次、侧滑背景及热量方向 | [141 个 JVM 用例通过](https://github.com/Joshmax010/wedo-Android/actions/runs/36881191120) |
| `d89b50c` | 编辑定位避开顶部覆盖栏，热量差值保留必要小数 | [143 个 JVM 用例通过](https://github.com/Joshmax010/wedo-Android/actions/runs/36999425832) |

首次第四批检查通过编译，但新增上限测试发现 `2200 / 2000 * 100` 可产生 `110.00000000000001`，从而误判恰好 110% 的热量。`f3b1301` 改为直接比较热量区间，保留测试并重跑全套检查，现已通过。该完成反馈规则独立于既有周报统计口径。

代码复用合并了四页的生命周期事件收集、重复营养字段和 12 处字段配色；非超量进度条不创建持续条纹动画。相对 UI 改版前 `c207631`，主 Kotlin 源码从 70 个文件、11403 行变为 75 个文件、11609 行，净增 206 行，用于新增主题偏好、共享导航、完成判定和反馈；未新增依赖、数据库迁移或网络权限。

### 本次收尾验证

- 首页复用现有格式化函数，补充还差/超出两个 JVM 用例：目标 2000.5 kcal，摄入 2000/2001 显示 0.5，摄入 1999/2002 显示 1.5；保留整数和恰好达标的原有用例。
- 饮食与身体编辑共用可视区域请求，顶部范围扩展到覆盖栏高度。另用 Compose 默认滚动距离计算核对无顶栏/48dp 顶栏及 1x/3x 密度参数，表单顶部均留在顶栏下方。此补充几何核对不计入 143 个 JVM 用例，不替代真实设备显示检查。
- 用户提交的首轮本地记录见 [设备反馈修复](./DEVICE_FEEDBACK_FIXES.md)：141 个 JVM 用例、Lint 38 条警告及两个 debug APK 打包通过。当前云端为零错误、39 条既有警告；本次云端没有打包 APK，也没有运行设备用例。

### 最新 JVM 用例明细

| 测试类 | 通过数 |
|---|---:|
| BackupManagerTest | 26 |
| BackupReadFailureTest | 2 |
| CalculatorTest | 35 |
| DailyGoalTest | 8 |
| DateUtilsTest | 41 |
| InteropTest | 4 |
| PageChromeTest | 3 |
| BodyStatsViewModelTest | 3 |
| HomeViewModelTest | 6 |
| RecordViewModelTest | 9 |
| SettingsViewModelTest | 3 |
| ViewModelOperationsTest | 2 |
| WeeklyViewModelTest | 1 |
| 合计 | 143 |

UI 改版及后续设备反馈/收尾另增 22 个用例，覆盖零热量空状态、零缺口、历史日期/餐次写入、旧身体读取取消、完成条件边界，以及反馈读取失败不能误报已完成的写入失败。

## 代码清理阶段历史记录

2026-10-01，GitHub Actions 已完成代码清理后的完整验证。

- 验证代码提交：`aa75c7090aa236758370e38c8c98cf4e9e9abb49`
- [成功的工作流运行](https://github.com/Joshmax010/wedo-Android/actions/runs/36828778125)
- [完整报告与构建日志](https://github.com/Joshmax010/wedo-Android/tree/codex/cloud-test-results/runs/36828778125)
- 应用版本 1.4.3（versionCode 5）、Room v4、备份 Schema v3。

## 检查结果

实际执行命令：

```bash
bash gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug --max-workers=4 --console=plain
```

| 检查 | 结果 |
|---|---|
| 主代码编译，包括 Room KSP 与 Compose | 通过 |
| JVM 单元测试 | 121 个通过，0 失败、0 错误、0 跳过 |
| Android Lint | 通过，0 错误、39 条原有警告 |
| Gradle 总结果 | BUILD SUCCESSFUL，2 分 54 秒 |

本轮按要求仅验证代码编译、JVM 测试和 Lint；未执行 APK 打包或真机/模拟器测试。38 个 Room 设备用例仍待你本地执行。

### 代码清理范围与专项检查

- `592a1bc`：删除未调用的 DataStore 封装与依赖、未使用的样式常量和空页面占位文件。实际引导状态保存在 Room 元信息中。
- `5d2d62d`：209 条食物预设的共同字段集中构造，源码从 2526 行减至 247 行。独立使用 Kotlin 2.2.10 编译清理前后实现，逐项比较实际模型；固定 ID、顺序、营养值、标签及默认字段全部一致，仅忽略初始化时生成的时间戳。
- `aa75c70`：以 AndroidX 内置 `viewModelFactory`/`initializer` 替换六套自定义工厂，无新增封装或依赖。源码比对确认六个页面的布局及事件处理保持一致。
- 主 Kotlin 源码从 73 个文件、13901 行减至 70 个文件、11403 行，净减少 2498 行，其中 2279 行来自预设数据声明，219 行来自其余清理。
- 数据库、领域模型、仓库读写、备份逻辑及全部原有测试源码未改动。现有 121 个 JVM 用例全部通过；本批未新增测试文件。

### JVM 用例明细

| 测试类 | 通过数 |
|---|---:|
| BackupManagerTest | 26 |
| BackupReadFailureTest | 2 |
| CalculatorTest | 35 |
| DateUtilsTest | 41 |
| InteropTest | 4 |
| BodyStatsViewModelTest | 2 |
| RecordViewModelTest | 5 |
| SettingsViewModelTest | 3 |
| ViewModelOperationsTest | 2 |
| WeeklyViewModelTest | 1 |
| 合计 | 121 |

全部 15 个新增 JVM 用例已通过。测试覆盖备份读取失败与取消、模板订阅、表单保留、异步读取/保存、日期切换和周报更新。

## 验证中修复的问题

首次完整检查的编译和 121 个 JVM 用例已经通过，Lint 报告原有主题中的 `android:windowLightNavigationBar` 属性需要 API 27，项目最低版本为 API 26。

提交 `31ce4b0` 将公共颜色配置抽到基础主题，把上述属性放到 `values-v27/themes.xml`。API 26 使用基础主题，API 27 及以上才加载亮色导航栏的深色图标配置。没有提高最低 Android 版本，也没有屏蔽 Lint 检查。修复后的完整检查通过。

上轮底层优化验证提交为 `66236f38133e8662e78b6613077de78b27a71f66`，见 [原工作流运行](https://github.com/Joshmax010/wedo-Android/actions/runs/36824295964)。代码清理删除了未使用 DataStore 的依赖版本警告，警告从 40 条降至 39 条，没有新增警告。剩余警告涉及依赖/工具链版本建议、默认 Locale、启动图标、备份配置等；没有批量升级依赖或改变相关功能。

## 后续云端检查

工作流位于 [`.github/workflows/cloud-tests.yml`](./.github/workflows/cloud-tests.yml)：功能分支的代码或构建配置提交自动检查，也支持手动触发。文档提交不触发重复构建。

应用构建任务使用只读仓库权限；独立报告任务将 JSON、JUnit XML、Lint 报告和构建日志追加到 `codex/cloud-test-results`，不改应用代码分支。报告同时作为工作流产物保留 7 天，Git 报告分支保留历次记录。报告校验要求 JVM 用例数量非零且无失败、错误或跳过。

2026-10-01 的实例曾遇到 Maven Central HTTP 429 与 JetBrains 下载重定向限制；完整检查改由 GitHub 托管执行器执行并沿用至今。报告经 Git 归档，不依赖本实例调用 `api.github.com`。

早期另用 Kotlin 2.2.10/JUnit 4.13.2 独立运行了 DateUtils 与 Calculator 的 76 个原有用例，全部通过。这是纯逻辑补充检查，不替代上面的完整 Gradle 验证；其中序列化库仅提供领域模型注解，未执行 JSON 序列化或生成序列化代码。
