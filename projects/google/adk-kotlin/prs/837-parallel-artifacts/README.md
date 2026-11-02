# #837：加载同批并行工具响应中的全部 artifact

## 状态与来源

- [上游 PR #837](https://github.com/google/adk-kotlin/pull/837)：Open、非草稿、无冲突。
- 状态核验：2026-11-02 20:19（Asia/Shanghai）；暂无评论或审核意见。
- CLA 与 `check-changes` 通过；[validation](https://github.com/google/adk-kotlin/actions/runs/41161626827) 及标题、提交、安全工作流仍显示 `action_required`，等待维护者操作。
- 基线：`2809a837752644f3720eaa759d2cb5144ba1a84e`。核验时 main 已前进 2 个提交（workflow 节点错误命名、A2A 文件名与媒体字段），不涉及本 PR 的文件，PR 仍可无冲突合并，未变基。
- 修复提交：[433fbd2bdbe250ee9d19da73e2be7da92f19e0f7](https://github.com/GCSimba/adk-kotlin/commit/433fbd2bdbe250ee9d19da73e2be7da92f19e0f7)。
- 分支：`codex/fix-parallel-artifact-loading`。
- 行为参考：[Python LoadArtifactsTool](https://github.com/google/adk-python/blob/e9e8f6156fb73cdfe799af0a0a57786e807732df/src/google/adk/tools/load_artifacts_tool.py#L530-L539)。
- [修复补丁](change.patch) · [验证摘要](validation.json) · [Linux 全量测试计数](test-results.json) · [基线复现](../../research/repros/README.md)。
- 2026-11-02 经用户授权更新了 PR 描述的验证部分：用 Linux 全量构建结果替换“全仓库构建未验证”，保留对此前 Windows 运行的简短说明。没有发评论，也没有通知任何人。

## 问题与复现

运行时把并行工具响应按顺序放入同一消息，旧 `LoadArtifactsTool` 只检查第一个 part。另一工具排在 `load_artifacts` 前面时，工具虽返回文件名，下一次模型请求却缺少 artifact 的实际内容。

用同一真实 Runner 场景交换 `get_currency` 和 `load_artifacts` 的顺序：加载工具在首位时，对照通过；加载工具在后面时，下一次模型请求应包含相同文件内容，但未修复基线上的断言失败。JVM、Android host 均复现该差异。

## 修复与回归覆盖

遍历原始最后一条消息的全部 part，处理所有匹配的 function response；保留响应和文件顺序，跳过无关 part 与缺失文件，并保持原请求不变。

在既有 `LoadArtifactsToolTest` 的 14 项测试基础上增加 3 项回归：Runner 中两种排列顺序，以及文本、其他工具响应、多个加载响应和缺失文件混合的情况。该分支只包含 artifact 加载修复。

## 验证

Windows、JDK 21 定向测试：JVM **17/17**、Android host **17/17**；ktfmt 与差异检查通过。

```sh
./gradlew -PjdkVersion=21 "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g" --max-workers=2 --no-daemon --console=plain --stacktrace --continue :google-adk-kotlin-core:jvmTest --tests com.google.adk.kt.tools.LoadArtifactsToolTest :google-adk-kotlin-core:testAndroidHostTest --tests com.google.adk.kt.tools.LoadArtifactsToolTest
```

2026-11-01 在独立的 Linux 副本中检出 `433fbd2`，完成全仓库构建。环境为 Ubuntu 24.04 / WSL2、OpenJDK 21.0.12.1、Gradle wrapper 9.8.0、Android platform 37.0，build-tools 36.0.0 与 37.0.0：

| 检查 | 结果 |
| --- | --- |
| 全仓库 build | 通过；500 个任务全部执行，没有复用 |
| 全仓库测试记录 | 10529 通过、0 失败、0 错误、18 条跳过记录 |
| core JVM / Android host | 4876 通过（6 条跳过记录）/ 4477 通过 |
| LoadArtifactsToolTest | JVM 17/17、Android host 17/17 |
| Windows 上失败或超时的套件 | LocalEnvironmentTest 58/58（含 Windows 上被排除的 `execute_cancellation_killsProcessTree`）、SkillToolsetEnvironmentTest 35/35、EnvironmentToolsetTest 24/24、McpToolsetIntegrationTest 22/22、McpResultSerializationIntegrationTest 1/1；McpAgentIntegrationTest 2 通过，另 6 条是缺少 `GOOGLE_API_KEY` 的跳过记录 |
| 构建后检查 | 工作区无改动（对应 CI 最后一步）；补丁哈希不变；ktfmt 0.65 两个改动文件通过 |

```sh
./gradlew -PjdkVersion=21 "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g" -Dorg.gradle.parallel=false --max-workers=2 --no-daemon --console=plain --stacktrace --continue build
```

未增加测试排除、禁用标志或测试内部超时覆盖，集成测试重试保持项目默认值，只降低了构建资源。18 条跳过记录与 #838 相同：8 条 Firebase 集成用例缺少凭据，4 条 LiteRT-LM 用例未设置模型，2 个在线 Gemini MCP 方法各记录 3 次。总数比 #838 的 10531 少 2 条，因为两个分支在同一基线上各自新增测试（#838 每个平台 4 个，#837 每个平台 3 个）。

真实后端/模型、Android 设备测试、JDK 17/25 矩阵和 Dokka（KDoc 未修改）未在本地运行。这是本地 Linux 结果，不代表官方 CI。

此前的 Windows 全仓库构建在 20 分钟期限后**未完成**。本地外部 init 脚本只排除了 `LocalEnvironmentTest.execute_cancellation_killsProcessTree*` 的已知无限等待，其余 shell/符号链接失败和 MCP 初始化超时保留可见。这些套件在上述 Linux 构建中都没有失败；Windows 上 MCP 超时的根因仍未确认。

## 验证过程中的环境问题

Gradle 报告的耗时是 10 小时 2 分。Windows 在 12:43 进入新型待机，22:13 才退出（16:43 短暂唤醒后立即再次待机），期间 WSL 虚拟机被挂起。待机窗口内构建目录没有任何文件写入，也没有测试套件跨越该窗口；webserver 测试在恢复后完整运行。实际构建时间约 32 分钟，测试套件自身耗时合计 10.4 分钟。长时间构建前应关闭睡眠和待机。

## 审核与后续

- 2026-11-02 21:37 核验：没有审阅请求、负责人、评论或审核。#774 由 damianmomotgoogle 审阅批准（经 copybara-service 合并），他 11-02 也在 #838 回复过，是最合适的推动人选。
- 用户决定暂不提醒。若约 11-05 仍无动静，经用户确认后在 PR 下 @damianmomotgoogle，说明本 PR 只改 `LoadArtifactsTool`、与 #825 无关，且 Linux 全量构建已通过。