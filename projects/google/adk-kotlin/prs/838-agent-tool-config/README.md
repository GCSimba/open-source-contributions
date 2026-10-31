# #838：AgentTool 继承调用配置

## 状态与来源

- [上游 PR #838](https://github.com/google/adk-kotlin/pull/838)：Open、非草稿、无冲突。
- 状态核验：2026-10-31 20:18（Asia/Shanghai）；暂无评论或审核意见。
- CLA 与 `check-changes` 通过；[validation](https://github.com/google/adk-kotlin/actions/runs/41199680234) 及标题、提交、安全工作流显示 `action_required`，等待维护者操作。
- 基线：`2809a837752644f3720eaa759d2cb5144ba1a84e`。
- 修复提交：[5a24148a114f1736fae7ae12bcd5817e50e668ab](https://github.com/GCSimba/adk-kotlin/commit/5a24148a114f1736fae7ae12bcd5817e50e668ab)。
- 分支：`codex/inherit-agent-tool-run-config`。
- 行为参考：[Python AgentTool](https://github.com/google/adk-python/blob/95dd267c22f822d554e93df165274389d5ae7f31/src/google/adk/tools/agent_tool.py#L395-L418)。
- [修复补丁](change.patch) · [验证摘要](validation.json) · [完整测试计数](test-results.json) · [基线复现](../../research/repros/README.md)。

## 问题与复现

`AgentTool` 启动子 Runner 时遗漏调用者的 `RunConfig`。在 Kotlin 实现中，子运行接收到 `null`，因此不受调用者配置的正数 `maxLlmCalls` 限制，并丢失自定义元数据。

用需要两次模型调用的子智能体作对照：直接 Runner 配置 `maxLlmCalls=1` 会拒绝第二次调用；相同调用配置通过 AgentTool 委托时，旧实现却可以完成两次调用。JVM、Android host 均复现。这里的 Kotlin 空配置表示无上限，不是 Python 默认的 500 次。

## 修复与回归覆盖

向子 Runner 传递调用配置的可空副本，并强制 `StreamingMode.NONE`。其他设置保留，调用者配置保持不变；子运行拥有独立计数器，原有 null 行为保留。

新增 4 项回归，覆盖调用上限、父运行已计数时的独立子计数、元数据及实际非流式模型请求与调用者配置不变、空配置。既有 `AgentToolTest` 共 31 项，在两个平台均通过。

## 验证

Ubuntu 24.04 / WSL2、JDK 21、Gradle wrapper 9.8.0、Android SDK 37.0，build-tools 36.0.0 与 37.0.0：

| 检查 | 结果 |
| --- | --- |
| 完整 core build | 通过，21 分 11 秒 |
| core JVM | 4877 通过、0 失败；6 条跳过记录 |
| core Android host | 4478 通过、0 失败、0 跳过 |
| AgentToolTest | JVM 31/31、Android host 31/31 |
| 全仓库 build | 通过，29 分 28 秒；500 个任务，437 执行、63 复用 |
| 全仓库测试记录 | 10531 通过、0 失败、0 错误、18 条跳过记录 |
| 格式与差异检查 | ktfmt、工作区及暂存区差异检查通过 |

完整 core 检查：

```sh
./gradlew -PjdkVersion=21 "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g" -Dorg.gradle.parallel=false --max-workers=2 --no-daemon --console=plain --stacktrace --continue :google-adk-kotlin-core:build
```

完整仓库检查：

```sh
./gradlew -PjdkVersion=21 "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g" -Dorg.gradle.parallel=false --max-workers=2 --no-daemon --console=plain --stacktrace --continue build
```

未增加测试排除、禁用标志或测试内部超时覆盖，集成测试重试保持项目默认值。全仓库运行复用了已完成 core 检查的结果，计数包含这些结果，不代表又执行了一遍 core。

18 是原始 XML 跳过记录数，其中两个 Gemini 方法各记录了 3 次；其余为缺少 Firebase 凭据或 LiteRT 模型的用例。真实后端/模型、Android 设备测试及 JDK 17/25 矩阵未在本地运行。KDoc 未修改，本次未运行 Dokka。

## 验证过程中的环境问题

首次 Linux 构建在进入测试前因缺少 build-tools 36.0.0、自动下载超时而失败；补齐并校验官方组件后，上述两次构建完整通过。此前 Windows 全量运行的 MCP 超时根因仍未确认；独立同步、异步、生产 await 探针和此次完整 Linux 本地 MCP 测试均通过。

本分支不包含 #837 补丁。测试前后及发布提交的补丁 SHA-256 一致：`50b4d2bc1b2a3e106cef5d8c3a9d41241265c55b4541ac5380a05229d3b49582`。
