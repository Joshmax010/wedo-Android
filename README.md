# Nutrition Tracker (Android) · 营养记录器

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

**English** | [中文](#中文说明)

A fully **offline**, privacy-first nutrition tracking app for Android. Record daily meals (calories, macros, and custom micronutrients), compare against your goals, and review weekly statistics — no account, no network, no tracking.

> Native Android port of the WeChat Mini Program "营养记录器" (Nutrition Tracker). Current version: **1.4.3** (final maintenance release).

## Features

- **Daily logging** — record breakfast / lunch / dinner / snacks with calories (kcal & kJ dual input), protein, fat, carbs, and custom micronutrients
- **Weight-based entry** — enter values per 100 g and let the app scale to actual portion weight
- **Food templates** — ~200 built-in preset foods with tags, plus your own custom templates with auto-complete suggestions
- **Goals & metabolism** — body profile (gender / age / height / weight / activity level), BMR & TDEE via Mifflin-St Jeor, one-tap recommended macro targets
- **Weekly report** — 7-day trends with custom Canvas line & bar charts, achievement rates, and copy-to-clipboard summary
- **Body stats** — weight / body fat / muscle mass history with trend chart
- **Backup & restore** — human-readable JSON export/import with tolerance for older schema versions

## Tech Stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVI-style MVVM — single `UiState` (StateFlow) + one-shot events (Channel) + Repository + UseCase |
| Navigation | navigation-compose 2.8.2 (type-safe `@Serializable` routes) |
| Storage | Room 2.8.4 (reads as `Flow`, writes return `Resource<Unit>`) + DataStore |
| Serialization | kotlinx.serialization |
| Error handling | `Resource<T>` wrapper — user-readable errors flow to UI |

## Build

Requirements: Android Studio (latest stable), JDK 17+, Android SDK 34. Gradle 9.4.1 is fetched by the included wrapper.

```bash
git clone <this-repo>
cd 健身wedo-android   # or your local folder name
./gradlew :app:assembleDebug
```

Run tests (see notes on non-ASCII paths in `PROJECT_DOCUMENTATION.md` §7.3):

```bash
./gradlew :app:testDebugUnitTest
```

Full architecture docs, data schema, and changelog live in [`PROJECT_DOCUMENTATION.md`](./PROJECT_DOCUMENTATION.md) (Chinese).

## Data & Privacy

All data stays in a local Room database. The app requests no network permission and collects nothing. Uninstalling clears everything — use the built-in JSON export to back up.

## License

Released under the [MIT License](./LICENSE).

---

## 中文说明

一款**纯离线、隐私优先**的 Android 营养记录应用。手动录入每日各餐次的热量、宏量营养素及自定义微量营养素，应用自动汇总并与目标值对比，生成首页概览与每周统计报告——不联网、不登录、不上传、不采集任何数据。

本项目是微信小程序「营养记录器」的 Android 原生版本，当前版本 **1.4.3**（终版维护状态）。

### 主要功能

- 每日四餐记录：热量（kcal / kJ 双单位联动输入）、蛋白质、脂肪、碳水、自定义微量营养素
- 按克重换算：以每 100g 基准值录入，自动换算实际摄入量；记录卡片显示克重
- 食物模板库：约 200 种预设食物（含分类标签），支持自定义模板、多标签管理与录入自动补全
- 代谢计算：身体档案 + Mifflin-St Jeor 公式计算 BMR / TDEE，一键生成推荐宏量目标
- 每周统计：自定义 Canvas 折线图 / 柱状图、达标率、热量缺口汇总，支持一键复制周报文本
- 身体记录：体重 / 体脂率 / 肌肉量趋势图
- 数据备份：可读 JSON 格式导出 / 导入，兼容旧版本份格式

### 架构

MVI 风格 MVVM：单一 `UiState`（StateFlow）+ `Channel` 一次性事件 + UseCase 业务下沉 + Repository 读写分离（读 `Flow` / 写 `Resource<Unit>`）+ navigation-compose 2.8 类型安全路由。详见 [`PROJECT_DOCUMENTATION.md`](./PROJECT_DOCUMENTATION.md)。

### 构建

需要 Android Studio（最新稳定版）、JDK 17+、Android SDK 34。项目已内置 Gradle Wrapper（9.4.1）：

```bash
git clone <本仓库>
cd 健身wedo-android
./gradlew :app:assembleDebug
```

### 数据与隐私

所有数据仅保存在本地 Room 数据库。应用不申请网络权限、不联网、不采集任何信息。卸载应用会清空数据，请定期使用内置的 JSON 导出备份。

### 开源协议

[MIT License](./LICENSE)，可自由使用、修改与分发。
