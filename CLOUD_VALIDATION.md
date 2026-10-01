# 云端验证记录

2026-10-01，GitHub Actions 已完成本轮代码验证。

- 验证代码提交：`66236f38133e8662e78b6613077de78b27a71f66`
- [成功的工作流运行](https://github.com/Joshmax010/wedo-Android/actions/runs/36824295964)
- [完整报告与构建日志](https://github.com/Joshmax010/wedo-Android/tree/codex/cloud-test-results/runs/36824295964)
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
| Android Lint | 通过，0 错误、40 条原有警告 |
| Gradle 总结果 | BUILD SUCCESSFUL，3 分 40 秒 |

本轮按要求仅验证代码编译、JVM 测试和 Lint；未执行 APK 打包或真机/模拟器测试。38 个 Room 设备用例仍待你本地执行。

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

剩余 40 条警告涉及依赖/工具链版本建议、默认 Locale、启动图标、备份配置等；没有在本轮批量升级依赖或改变相关功能。

## 后续云端检查

工作流位于 [`.github/workflows/cloud-tests.yml`](./.github/workflows/cloud-tests.yml)：功能分支的代码或构建配置提交自动检查，也支持手动触发。文档提交不触发重复构建。

应用构建任务使用只读仓库权限；独立报告任务将 JSON、JUnit XML、Lint 报告和构建日志追加到 `codex/cloud-test-results`，不改应用代码分支。报告同时作为工作流产物保留 7 天，Git 报告分支保留历次记录。报告校验要求 JVM 用例数量非零且无失败、错误或跳过。

当前 Codex 实例的 Maven Central 下载出现 HTTP 429，部分 JetBrains 下载重定向被运行中的网络策略拒绝；完整项目检查因此交给 GitHub 托管执行器完成。这不依赖 Codex 实例访问 `api.github.com`，报告可通过 Git 获取。

当前实例另用 Kotlin 2.2.10/JUnit 4.13.2 独立运行了 DateUtils 与 Calculator 的 76 个原有用例，全部通过。这是纯逻辑补充检查，不替代上面的完整 Gradle 验证；其中序列化库仅提供领域模型注解，未执行 JSON 序列化或生成序列化代码。
