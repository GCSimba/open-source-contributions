# 复现文件

这些文件来自 2026-10-31 的 ADK Kotlin 排查。它们保留原 Apache 2.0 版权头，用于在独立源码工作副本中检查记录中的旧行为。本档案目录没有 Gradle 项目，测试不会在这里自动运行。

## 文件与执行背景

基线：`2809a837752644f3720eaa759d2cb5144ba1a84e`。

| 文件 | 对应源码位置 | 历史结果 |
| --- | --- | --- |
| [LoadArtifactsParallelResponseBugReproTest.kt](LoadArtifactsParallelResponseBugReproTest.kt) | `core/src/commonTest/kotlin/com/google/adk/kt/tools/` | JVM/Android 各一项首位对照通过、一项后位断言失败；对应 PR #837 |
| [AgentToolRunConfigBugReproTest.kt](AgentToolRunConfigBugReproTest.kt) | `core/src/commonTest/kotlin/com/google/adk/kt/tools/` | JVM/Android 各一项直接 Runner 对照通过、一项 AgentTool 断言失败；对应 PR #838 |
| [GenaiPromptSeedRegressionTest.kt](GenaiPromptSeedRegressionTest.kt) | `mlkit/src/test/kotlin/com/google/adk/kt/mlkit/` | 已执行的一测试版本，期望 seed123、实际0；尚未提交 |
| [InstructionArtifactTextReproductionTest.kt](InstructionArtifactTextReproductionTest.kt) | `core/src/commonTest/kotlin/com/google/adk/kt/processors/` | 两个平台均观察到 JSON/调试表示；预期行为待确认 |
| [GenaiPromptMediaTraceRegressionTest.kt](GenaiPromptMediaTraceRegressionTest.kt) | `mlkit/src/test/kotlin/com/google/adk/kt/mlkit/` | 两项媒体字段断言失败；日志策略待确认 |

[baseline-results.json](baseline-results.json) 保存两项选定问题的 8 条历史结果：两个平台各 2 个对照通过、2 个缺陷断言失败。断言失败在这里表示旧行为被复现，不是最终修复的测试结果。

媒体 TRACE 草稿中的 `example.invalid`、`private-test-token` 及媒体数据为合成测试数据。

## 在源码工作副本运行

1. 使用 ADK 的独立工作副本并检出上述基线，先读取该提交的构建指引。
2. 将需要的单个文件复制到表中对应目录，保留文件名和包名；避免覆盖其他 PR 的修改。
3. 从源码根目录运行对应的筛选任务，例如：

```sh
./gradlew -PjdkVersion=21 "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g" --max-workers=2 --no-daemon --console=plain :google-adk-kotlin-core:jvmTest --tests com.google.adk.kt.tools.AgentToolRunConfigBugReproTest
./gradlew -PjdkVersion=21 "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g" --max-workers=2 --no-daemon --console=plain :google-adk-kotlin-core:testAndroidHostTest --tests com.google.adk.kt.tools.LoadArtifactsParallelResponseBugReproTest
./gradlew -PjdkVersion=21 "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g" --max-workers=2 --no-daemon --console=plain :google-adk-kotlin-mlkit:testDebugUnitTest --tests com.google.adk.kt.mlkit.GenaiPromptSeedRegressionTest
```

这些是对应历史任务的可移植写法，Windows 使用 `gradlew.bat`。归档时未重新执行；旧基线上预期包含失败退出。#837/#838 的最终回归已合入上游 PR 的既有测试类，修复后的命令和结果见各 PR 记录。
