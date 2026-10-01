# 健身wedo · Android

[![Release](https://img.shields.io/github/v/release/Joshmax010/wedo-Android?style=flat-square)](https://github.com/Joshmax010/wedo-Android/releases/latest)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?style=flat-square)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square)](https://developer.android.com/jetpack/compose)
[![Offline](https://img.shields.io/badge/network-none-blueviolet?style=flat-square)](#数据与隐私)
[![License: MIT](https://img.shields.io/badge/license-MIT-yellow?style=flat-square)](./LICENSE)

**中文** | [English](#english)

> 纯离线、隐私优先的营养记录工具。本地记录每日饮食与身体数据，自动汇总对比目标并生成周报 —— 全程不联网。

「健身wedo」是同名微信小程序的 Android 原生版本，两端共享核心数据模型与备份 JSON Schema，备份文件可直接跨端迁移。当前版本 **v1.4.3**，处于**终版维护状态**：功能已冻结，仅保留必要的兼容性与工具链维护。

## 下载安装

| 项目 | 说明 |
|------|------|
| 最新版本 | [v1.4.3 · Releases](https://github.com/Joshmax010/wedo-Android/releases/latest) |
| 安装包 | `wedo-fitness-v1.4.3.apk`（约 2.3 MB） |
| 系统要求 | Android 8.0（API 26）及以上 |
| 包名 | `com.example.nutrition` |

下载 APK 后传至手机安装即可（首次安装需允许「安装未知来源应用」）。后续版本沿用同一签名密钥，可直接覆盖升级。

<details>
<summary>校验安装包（可选）</summary>

| 项目 | 值 |
|------|------|
| APK SHA-256 | `119b0fe5be6c95ae4283b6d3e74bbb8a7833bbd81184ef3df62f123ee8f4301f` |
| 签名证书 SHA-256 | `041d81d18eb0d91506d4ec5c88d1c0f3a49aacf59bbaeb8f6d3fd697f8e4dd23` |
| 签名证书 DN | `CN=Nutrition Tracker, OU=wedo, O=Joshmax010, L=Beijing, ST=Beijing, C=CN` |
| 签名方案 | v2 + v3 |

```bash
sha256sum wedo-fitness-v1.4.3.apk
apksigner verify --print-certs wedo-fitness-v1.4.3.apk
```

</details>

## 功能特性

### 记录

- **四餐分列** — 早餐 / 午餐 / 晚餐 / 加餐独立录入与合计
- **双单位热量** — kcal 与 kJ 联动输入，按 1 kcal = 4.184 kJ 自动换算
- **按克重换算** — 以每 100 g 基准值录入，自动换算实际摄入份量
- **食物模板库** — 约 200 种内置预设食物，覆盖主食、肉蛋奶、豆制品、蔬菜、水果、坚果零食、饮品、油脂调料，带分类标签；支持自定义模板、多标签管理与录入自动补全

### 分析

- **代谢计算** — 填写身体档案（性别 / 年龄 / 身高 / 体重 / 活动量），按 Mifflin-St Jeor 公式计算 BMR 与 TDEE，一键套用推荐宏量目标
- **目标对比** — 首页概览展示当日热量与三大营养素完成度
- **每周统计** — 7 日趋势折线图与营养素柱状图（自绘 Canvas 实现）、达标率与热量缺口汇总，支持一键复制周报文本
- **身体记录** — 体重 / 体脂率 / 肌肉量历史与趋势图

### 数据

- **JSON 备份** — 可读格式导出 / 导入，兼容旧版备份（缺失字段自动补默认值）
- **跨端迁移** — 与微信小程序端使用同一份备份 Schema

## 技术栈

| 层级 | 选型 |
|------|------|
| 语言 | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3（Compose BOM 2024.06.00） |
| 架构 | MVI 风格 MVVM — 单一 `UiState`（StateFlow）＋一次性事件（`Channel`）＋ UseCase ＋ Repository |
| 导航 | navigation-compose 2.8.2（`@Serializable` 类型安全路由） |
| 存储 | Room 2.8.4（读操作返回 `Flow`，写操作返回 `Resource<Unit>`）＋ DataStore |
| 序列化 | kotlinx.serialization 1.6.3 |
| 构建 | Gradle 9.4.1 · AGP 9.2.1 · KSP 2.3.2 |
| 测试 | JUnit 4 ＋ kotlinx-coroutines-test（106 个单元测试）＋ Room instrumentation 测试 |

最低支持 Android 8.0（API 26），targetSdk / compileSdk 34。

## 项目结构

```
app/src/main/java/com/example/nutrition/
├── data/
│   ├── local/db/          # Room 数据库与 DAO
│   ├── local/entity/      # 数据表实体
│   ├── local/prefs/       # DataStore 封装
│   └── repository/        # 仓库实现（Flow 读 / Resource 写）
├── domain/
│   ├── constants/         # 营养素常量与预设食物模板
│   ├── model/             # 领域模型
│   ├── repository/        # 仓库接口
│   └── usecase/           # 业务逻辑（换算 / 校验 / 计算器 / 备份）
├── ui/
│   ├── charts/            # 自绘 Canvas 图表
│   ├── components/        # 通用组件
│   ├── navigation/        # 路由与底部导航
│   ├── screens/           # 页面
│   └── theme/             # 主题与排版
└── viewmodel/             # 各页面 ViewModel 与事件定义
```

主源码共 71 个 Kotlin 文件。

## 构建与开发

**环境要求**：Android Studio（最新稳定版）、JDK 17+、Android SDK 34。项目已内置 Gradle Wrapper（9.4.1），克隆后无需额外配置：

```bash
git clone https://github.com/Joshmax010/wedo-Android.git
cd wedo-Android
./gradlew :app:assembleDebug
```

**运行测试**：

```bash
./gradlew :app:testDebugUnitTest
```

<details>
<summary>发布签名配置</summary>

签名密钥不纳入版本控制。如需产出可安装的正式包，在项目根目录创建 `keystore.properties`：

```properties
storeFile=<你的密钥文件名>
storePassword=<密码>
keyAlias=<别名>
keyPassword=<密码>
```

再执行 `./gradlew :app:assembleRelease`。该文件缺失时构建自动退化为 unsigned APK，不影响日常开发与 CI。

> 密钥丢失将导致老用户无法覆盖升级，请妥善备份。

</details>

**已知限制**：项目路径包含非 ASCII 字符（如中文）时，在 Windows 上直接执行单元测试会触发 Gradle Test Worker 的 `ClassNotFoundException`（Gradle 8.13 与 9.4.1 均已复现，属 Gradle 路径编码缺陷，非代码问题）。解决办法见 [`PROJECT_DOCUMENTATION.md` §7.3](./PROJECT_DOCUMENTATION.md)。

## 数据与隐私

- 所有数据仅保存在设备本地的 Room 数据库中
- 应用不申请任何网络权限，不联网、不上传、不埋点、不采集任何信息
- 卸载应用会清除全部数据，请定期使用内置的 JSON 导出功能备份

本轮底层可靠性优化的本地编译、自动化测试及真机检查见 [统一回归清单](./LOCAL_REGRESSION_CHECKLIST.md)。

## 维护状态

遵循语义化版本。当前 **v1.4.3** 为终版维护版本，功能已冻结。完整的架构说明、数据模型、数据库迁移记录与逐版本变更历史见 [`PROJECT_DOCUMENTATION.md`](./PROJECT_DOCUMENTATION.md)。

## 开源协议

基于 [MIT License](./LICENSE) 开源，可自由使用、修改与分发。

Copyright (c) 2026 Joshmax010

---

## English

[中文](#健身wedo--android) | English

> A fully offline, privacy-first nutrition tracker for Android. Log meals and body metrics locally, compare them against your goals and generate weekly reports — entirely on-device.

**健身wedo** (wedo Fitness) is the native Android port of the WeChat Mini Program of the same name. Both ends share the same core data model and backup JSON schema, so backups migrate across platforms. Current version **v1.4.3**, in **final maintenance state** — feature-frozen, with compatibility and toolchain maintenance only.

### Download

| | |
|---|---|
| Latest release | [v1.4.3](https://github.com/Joshmax010/wedo-Android/releases/latest) |
| APK | `wedo-fitness-v1.4.3.apk` (~2.3 MB) |
| Requires | Android 8.0 (API 26)+ |
| Package | `com.example.nutrition` |

Verify the download: SHA-256 `119b0fe5be6c95ae4283b6d3e74bbb8a7833bbd81184ef3df62f123ee8f4301f`, signed with v2 + v3 schemes.

### Features

- **Meal logging** — breakfast / lunch / dinner / snacks, with kcal & kJ dual-unit input (1 kcal = 4.184 kJ)
- **Weight-based entry** — log per 100 g and let the app scale to the actual portion
- **Food templates** — ~200 built-in preset foods with category tags, plus custom templates and auto-complete
- **Metabolism** — body profile → BMR & TDEE via Mifflin-St Jeor, one-tap recommended macro targets
- **Weekly report** — 7-day trends with hand-drawn Canvas charts, achievement rates, copy-to-clipboard summary
- **Body stats** — weight / body fat / muscle mass history and trend charts
- **Backup & restore** — human-readable JSON, tolerant of older schemas, cross-platform with the Mini Program

### Tech Stack

Kotlin 2.2.10 · Jetpack Compose + Material 3 · MVI-style MVVM (single `UiState` + `Channel` events, UseCase layer, Repository with `Flow` reads / `Resource<Unit>` writes) · navigation-compose 2.8.2 type-safe routes · Room 2.8.4 · DataStore · kotlinx.serialization · Gradle 9.4.1 / AGP 9.2.1 / KSP 2.3.2 · 106 unit tests.

### Build

Requires Android Studio (latest stable), JDK 17+, Android SDK 34. The Gradle 9.4.1 wrapper is included:

```bash
git clone https://github.com/Joshmax010/wedo-Android.git
cd wedo-Android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Release signing is optional: create a gitignored `keystore.properties` to produce a signed APK; without it the build falls back to unsigned. See [`PROJECT_DOCUMENTATION.md`](./PROJECT_DOCUMENTATION.md) (Chinese) for architecture, migrations and the full changelog.

### Privacy

All data stays in a local Room database. The app requests no network permission and collects nothing. Uninstalling clears everything — use the built-in JSON export to back up.

### License

[MIT License](./LICENSE) · Copyright (c) 2026 Joshmax010
