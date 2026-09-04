# 营养记录器（Android）· Nutrition Tracker

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

**中文** | [English](#english)

一款**纯离线、隐私优先**的 Android 营养记录应用。手动录入每日各餐次的热量、宏量营养素及自定义微量营养素，应用自动汇总并与目标值对比，生成首页概览与每周统计报告——**不联网、不登录、不上传、不采集任何数据**。

> 本项目是微信小程序「营养记录器」的 Android 原生版本。当前版本 **1.4.3**（终版维护状态）。

## 功能特性

- **每日四餐记录** — 早餐 / 午餐 / 晚餐 / 加餐，热量支持 kcal 与 kJ 双单位联动输入，自动按 1 kcal = 4.184 kJ 换算
- **按克重换算** — 以每 100g 基准值录入，自动换算实际摄入量；记录卡片展示克重
- **食物模板库** — 约 200 种内置预设食物（主食、肉蛋奶、豆制品、蔬菜、水果、坚果零食、饮品、油脂调料，含分类标签），支持自定义模板、多标签管理与录入自动补全
- **代谢计算** — 身体档案（性别 / 年龄 / 身高 / 体重 / 活动量）+ Mifflin-St Jeor 公式计算 BMR / TDEE，一键生成推荐宏量目标
- **每周统计** — 自定义 Canvas 折线图 / 柱状图、营养素达标率、热量缺口汇总，支持一键复制周报文本
- **身体记录** — 体重 / 体脂率 / 肌肉量历史记录与趋势图
- **数据备份** — 可读 JSON 格式导出 / 导入，兼容旧版备份格式（缺失字段自动容错补默认值）

## 技术栈

| 层级 | 选型 |
|------|------|
| 语言 | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVI 风格 MVVM — 单一 `UiState`（StateFlow）+ 一次性事件（Channel）+ Repository + UseCase |
| 导航 | navigation-compose 2.8.2（`@Serializable` 类型安全路由） |
| 存储 | Room 2.8.4（读操作返回 `Flow`，写操作返回 `Resource<Unit>`）+ DataStore |
| 序列化 | kotlinx.serialization |
| 构建 | Gradle 9.4.1 · AGP 9.2.1 · KSP 2.3.2 |

## 构建

环境要求：Android Studio（最新稳定版）、JDK 17+、Android SDK 34。项目已内置 Gradle Wrapper（9.4.1），克隆后开箱即用：

```bash
git clone https://github.com/Joshmax010/wedo-Android.git
cd wedo-Android
./gradlew :app:assembleDebug
```

运行测试（非 ASCII 路径下的已知限制详见 `PROJECT_DOCUMENTATION.md` §7.3）：

```bash
./gradlew :app:testDebugUnitTest
```

完整架构说明、数据模型、数据库迁移与变更历史见 [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md)。

## 数据与隐私

所有数据仅保存在本地 Room 数据库。应用不申请网络权限、不联网、不采集任何信息。卸载应用会清空数据，请定期使用内置的 JSON 导出功能备份。

## 开源协议

以 [MIT License](./LICENSE) 开源，可自由使用、修改与分发。

---

## English

A fully **offline**, privacy-first nutrition tracking app for Android. Record daily meals (calories, macros, and custom micronutrients), compare against your goals, and review weekly statistics — no account, no network, no tracking.

> Native Android port of the WeChat Mini Program "营养记录器". Current version: **1.4.3** (final maintenance release).

### Features

- **Daily logging** — breakfast / lunch / dinner / snacks with calories (kcal & kJ dual input), protein, fat, carbs, and custom micronutrients
- **Weight-based entry** — enter values per 100 g and let the app scale to actual portion weight
- **Food templates** — ~200 built-in preset foods with tags, plus custom templates with auto-complete suggestions
- **Goals & metabolism** — body profile, BMR & TDEE via Mifflin-St Jeor, one-tap recommended macro targets
- **Weekly report** — 7-day trends with custom Canvas line & bar charts, achievement rates, copy-to-clipboard summary
- **Body stats** — weight / body fat / muscle mass history with trend chart
- **Backup & restore** — human-readable JSON export/import with tolerance for older schema versions

### Tech Stack

Kotlin 2.2.10 · Jetpack Compose + Material 3 · MVI-style MVVM (single `UiState` + Channel events, UseCase layer, Repository with `Flow` reads / `Resource<Unit`> writes) · navigation-compose 2.8.2 type-safe routes · Room 2.8.4 · DataStore · kotlinx.serialization.

### Build

Requires Android Studio (latest stable), JDK 17+, Android SDK 34. Gradle 9.4.1 wrapper included:

```bash
git clone https://github.com/Joshmax010/wedo-Android.git
cd wedo-Android
./gradlew :app:assembleDebug
```

Full architecture docs and changelog live in [`PROJECT_DOCUMENTATION.md`](./PROJECT_DOCUMENTATION.md) (Chinese).

### Data & Privacy

All data stays in a local Room database. The app requests no network permission and collects nothing. Uninstalling clears everything — use the built-in JSON export to back up.

### License

Released under the [MIT License](./LICENSE).
