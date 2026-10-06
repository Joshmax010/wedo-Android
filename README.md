# WeDo · Android 营养与身体记录

[![Release](https://img.shields.io/github/v/release/Joshmax010/wedo-Android?style=flat-square)](https://github.com/Joshmax010/wedo-Android/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square)](https://developer.android.com)
[![CI](https://github.com/Joshmax010/wedo-Android/actions/workflows/cloud-tests.yml/badge.svg)](https://github.com/Joshmax010/wedo-Android/actions/workflows/cloud-tests.yml)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](./LICENSE)

WeDo 是一款以本地数据为基础的 Android 营养记录工具。按餐记录饮食与摄入量，对照每日目标查看完成情况，通过周报和身体趋势回顾变化。无需注册账户，核心功能可离线使用。

**[下载 v1.4.5 安装包](https://github.com/Joshmax010/wedo-Android/releases/download/v1.4.5/wedo-fitness-v1.4.5.apk)** · [版本说明](https://github.com/Joshmax010/wedo-Android/releases/tag/v1.4.5) · [维护指南](./MAINTENANCE.md) · [English](#english)

## 安装与升级

| 项目 | 说明 |
| --- | --- |
| 版本 | 1.4.5（versionCode 6） |
| 系统要求 | Android 8.0（API 26）及以上 |
| 安装包 | `wedo-fitness-v1.4.5.apk`，约 2.2 MB，见 GitHub Releases 的 Assets |
| 应用包名 | `com.example.nutrition` |
| 校验信息 | [SHA-256 校验文件](https://github.com/Joshmax010/wedo-Android/releases/download/v1.4.5/wedo-fitness-v1.4.5.apk.sha256)；签名证书指纹见 Release 说明及维护指南 |

下载 APK 后在手机上安装；首次安装需允许对应来源安装应用。升级前在「设置 → 数据管理」导出 JSON 备份，并保存到应用之外的位置。

- **官方 v1.4.3 用户**：v1.4.5 沿用原发布签名，可以覆盖升级并保留数据。
- **本轮 debug 测试包用户**：测试包与正式发布包的签名不同，不能覆盖安装。先导出并保存 JSON 备份，再卸载测试包、安装正式包，最后在「数据管理」导入备份。**卸载会清除本地数据，必须先确认备份已保存。**

## 主要功能

| 页面 / 功能 | 能力 |
| --- | --- |
| 总览 | 查看指定日期的热量、宏量与微量营养素、四餐汇总，以及每日目标完成度 |
| 录入 | 早餐、午餐、晚餐、加餐独立记录；kcal / kJ 联动换算；按克重计算摄入量；编辑和删除历史记录 |
| 食物模板 | 内置常用食物，支持自定义模板、分类标签与录入自动补全 |
| 周报 | 7 日趋势、营养素统计、达标率与热量缺口汇总，支持复制报告文本 |
| 身体与目标 | 记录体重、体脂率和肌肉量；保存身体档案；计算 BMR / TDEE 并预览推荐营养目标 |
| 外观 | 跟随系统、浅色、深色主题；本机保存外观偏好 |
| 数据管理 | JSON 导出、预览与确认导入，兼容旧版备份和同名微信小程序的备份格式 |

## 1.4.5 更新

- **备份可靠性**：目标、饮食、元信息、自定义模板和身体记录在同一个 Room 事务中导入；写入或现有回读校验失败时回滚。读取失败显示错误与重试入口，避免生成不完整备份。
- **页面布局**：四餐等宽排列，压缩页头留白；滚动时左侧标题由 32sp 缩小到 20sp 并保留在顶部。右侧 WeDo 使用 20sp，点击后通过系统浏览器打开本项目。
- **编辑体验**：录入与身体记录的历史编辑定位避开顶部栏；设置二级页面隐藏底部主导航，不再保留底栏占位。
- **信息表达**：热量差值使用非负数配合「还差 / 超出 / 已达目标」，保留必要小数；修正记录卡片的侧滑背景与保存反馈。

本轮功能与界面已由用户于 **2026-10-06** 完成手工真机验收。发布构建、自动测试、签名与设备测试的详细记录见 [维护指南](./MAINTENANCE.md)；手工验收与 Room instrumentation 自动测试分别记录。

## 数据与隐私

应用数据保存在本机 Room 数据库，外观偏好保存在本机设置中。应用不申请 `INTERNET` 权限，不提供账号、云端同步或上传接口；WeDo 项目链接交由系统浏览器处理，浏览器访问 GitHub 需要网络。

备份采用可读 JSON，导出结果复制到剪贴板，导入前可预览内容。当前备份 Schema 为 v3，数据库为 v4；本次发布不引入数据库迁移。

- 备份列出的日期覆盖对应饮食记录，未列出的日期保留。
- 旧备份缺少自定义模板或身体记录字段时保留现有数据；明确提供空列表时清空相应数据，内置食物模板保留。
- 卸载应用或清除应用数据会删除本地记录。应用关闭 Android 自动备份，请自行保存导出的 JSON 文件。

## 开发与验证

使用 **JDK 21** 运行 Gradle，安装 Android SDK Platform 34，并通过 Android Studio 或 `local.properties` 指定 SDK 路径。Java / Kotlin 源码目标为 JVM 17；Gradle 运行环境与源码目标不同。

```bash
git clone https://github.com/Joshmax010/wedo-Android.git
cd wedo-Android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Windows PowerShell 使用同一组任务：

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Debug APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。Room instrumentation 用例需要 Android 设备或模拟器：

```bash
./gradlew :app:connectedDebugAndroidTest
```

正式构建使用本地、未入库的 `keystore.properties` 配置发布签名，再运行 `:app:assembleRelease`。未配置签名时生成 unsigned APK，不能直接作为安装包发布。签名管理、版本更新、发布与回滚步骤见 [MAINTENANCE.md](./MAINTENANCE.md)。

### 技术栈

| 层级 | 实现 |
| --- | --- |
| 语言与 UI | Kotlin 2.2.10 · Jetpack Compose · Material 3 |
| 状态与业务 | ViewModel / StateFlow · 一次性事件 · UseCase · Repository |
| 导航 | Navigation Compose 2.8.2，类型安全路由 |
| 持久化 | Room 2.8.4 · kotlinx.serialization 1.6.3 |
| 构建 | Gradle 9.4.1 · AGP 9.2.1 · KSP 2.3.2 |
| Android SDK | minSdk 26 · targetSdk / compileSdk 34 |
| 验证 | JUnit 4 / coroutines-test · Room instrumentation · Android Lint · 手工设备验收 |

源码位于 `app/src/main/java/com/example/nutrition/`：`data/` 负责存储，`domain/` 负责模型与业务规则，`viewmodel/` 负责页面状态，`ui/` 负责页面、导航、组件、图表和主题。

## 文档与维护

- [维护指南](./MAINTENANCE.md)：构建环境、版本与签名、测试边界、发布流程、分支策略及回滚。
- [项目文档](./PROJECT_DOCUMENTATION.md)：架构、数据模型、数据库迁移、备份兼容性与变更历史。
- [完整回归清单](./LOCAL_REGRESSION_CHECKLIST.md)：业务、备份、数据持久化与设备检查。
- [页头复测清单](./PAGE_HEADER_DEVICE_CHECKLIST.md)：滚动标题、WeDo 链接、编辑定位与二级导航。

反馈问题时请附应用版本、Android 版本、复现步骤及截图；涉及数据问题时使用脱敏示例，勿提交个人备份、签名密钥或本机配置。

## 开源协议

[MIT License](./LICENSE) · Copyright © 2026 Joshmax010

## English

WeDo is a local-first nutrition and body-metrics tracker for Android 8.0+. Log breakfast, lunch, dinner and snacks, scale food templates by portion weight, compare daily intake with goals, and review weekly reports and body trends.

**[Download v1.4.5](https://github.com/Joshmax010/wedo-Android/releases/download/v1.4.5/wedo-fitness-v1.4.5.apk)** · [Release notes](https://github.com/Joshmax010/wedo-Android/releases/tag/v1.4.5)

This release improves atomic backup imports, error recovery, scrolling headers and history editing. Main-page titles collapse from 32sp to 20sp on the left; the 20sp WeDo button on the right opens this repository in the system browser. Secondary settings pages omit the main bottom navigation.

Core features work offline. Records remain in a local Room database; the app has no `INTERNET` permission or cloud-sync service. Opening the GitHub link uses an external browser. Official v1.4.3 installations can upgrade in place using the same release signing key. The temporary debug test build uses a different key: export and save a JSON backup first, uninstall the debug app, install the official release, then import the backup. Uninstalling deletes local data.

Build with JDK 21 and Android SDK 34 using the Gradle Wrapper commands above. See [MAINTENANCE.md](./MAINTENANCE.md) and [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md) for release procedures and implementation details. Licensed under [MIT](./LICENSE).
