# #837：加载同批并行工具响应中的全部 artifact

## 状态与来源

- [上游 PR #837](https://github.com/google/adk-kotlin/pull/837)：Open、非草稿、无冲突。
- 状态核验：2026-11-04 11:35（Asia/Shanghai）。第 1 轮审阅意见已处理；11-04 维护者要求再次变基，已完成并回复。标签已由 `waiting on author` 换成 `google support`（“已升级给 Google 内部支持或工程团队”），等待内部处理。
- CLA 与 `check-changes` 在新提交上通过；validation、标题、提交和安全工作流显示 `action_required`，等待维护者批准运行。
- 基线：`e863facc7faa6ec0b91b2d9080e68bd5d22fda2f`，即 11-04 时最新的 main。按审阅要求变基过两次：首次提交基于 `2809a83`，第 1 轮审阅后基于 `b961c9e`。
- 修复提交：[54a710809ac0aefb11aae3246caaaecdd2744f42](https://github.com/GCSimba/adk-kotlin/commit/54a710809ac0aefb11aae3246caaaecdd2744f42)。此前依次为 `433fbd2`（首次提交）、`b4d321b`（第 1 轮审阅后）；`b4d321b` 与 `54a7108` 的 diff 逐字节相同。
- 分支：`codex/fix-parallel-artifact-loading`。
- 行为参考：[Python LoadArtifactsTool](https://github.com/google/adk-python/blob/e9e8f6156fb73cdfe799af0a0a57786e807732df/src/google/adk/tools/load_artifacts_tool.py#L530-L539)。
- [修复补丁](change.patch) · [验证摘要](validation.json) · [Linux 全量测试计数](test-results.json) · [基线复现](../../research/repros/README.md)。

## 问题与复现

运行时把并行工具响应按顺序放进同一条消息，而旧的 `LoadArtifactsTool` 只检查第一个 part。如果另一个工具排在 `load_artifacts` 前面，工具虽然返回了文件名，下一次模型请求里却没有 artifact 的实际内容。

复现：在同一个真实 Runner 场景里交换 `get_currency` 和 `load_artifacts` 的顺序。加载工具在首位时，对照通过；加载工具在后面时，下一次模型请求本应包含同样的文件内容，但在未修复的基线上断言失败。JVM 和 Android host 都能复现这一差异。

## 修复与回归覆盖

遍历原始最后一条消息的全部 part，处理所有匹配的 function response：保留响应和文件顺序，跳过无关 part 和缺失的文件，并保持原请求不变。重新缩进的代码块保持原来的 `Content(...)` 写法。

在 `LoadArtifactsToolTest` 原有 14 个测试的基础上新增 5 个：

- Runner 中 `load_artifacts` 排在另一个工具之前、之后两种顺序；
- 两个批量响应场景，读取在另一个 session 中保存为 `user:invoice.txt` 的 artifact：一个直接请求 `user:invoice.txt`，另一个请求 `invoice.txt`，走 `loadArtifact` 的 `user:` 回退；
- 文本、其他工具响应、多个加载响应和缺失文件混在一起的请求处理测试，同时检查原请求不变。

Runner 场景断言下一次模型请求的最后一条内容正好是 `Content(USER, ["Artifact <name> is:", 文件内容])`。

## 审阅记录

- 2026-11-03 07:55–07:56（Asia/Shanghai），aravindmurali22 给出 CHANGES_REQUESTED，并加上 `waiting on author` 标签：
  1. [撤回无关的格式改动](https://github.com/google/adk-kotlin/pull/837#discussion_r4449207861)：`declaration()` 的重新折行，以及测试文件第 97–386 行既有测试上的尾逗号；
  2. [收紧断言](https://github.com/google/adk-kotlin/pull/837#discussion_r4449213365)：`nextRequest.contents.last()` 必须等于期望的 `Content`，并补一个跨 session 的 `user:` artifact 在批量响应中的用例；
  3. 变基到最新 main。
- 处理方式：
  - 变基后只保留功能相关的 hunk，把两个文件重置为 main 版本后重新套用；
  - 新增代码按上游风格写，单个多行参数后不加尾逗号；
  - 两种 `user:` 用法（直接带前缀、回退）各加一个用例。
- 2026-11-03 14:08 用 `--force-with-lease` 推送 `b4d321b`（远端旧值 `433fbd2`），随后在两个讨论串回复（[回复 1](https://github.com/google/adk-kotlin/pull/837#discussion_r4451439769)、[回复 2](https://github.com/google/adk-kotlin/pull/837#discussion_r4451440069)）并发了[顶层评论](https://github.com/google/adk-kotlin/pull/837#issuecomment-6596767371)。
- 14:44 全量构建完成后，更新了 PR 描述的测试与验证部分，并补上此前遗漏的 AI 辅助说明（与 #856 一致）。
- 外部贡献者不能重新请求审阅：REST 返回 404，GraphQL 返回 FORBIDDEN。
- 2026-11-04 03:01（Asia/Shanghai），aravindmurali22 [请求再次变基到最新 main](https://github.com/google/adk-kotlin/pull/837#issuecomment-6612882270)，并把 `waiting on author` 换成 `google support`。main 新增 13 个提交（e863fac），都不涉及本 PR 的两个文件，变基没有冲突。
- 经用户确认，11:33 用 `--force-with-lease` 推送 `54a7108`（远端旧值 `b4d321b`），在 PR 描述的验证部分补了一段变基后的重跑结果，并发了[回复](https://github.com/google/adk-kotlin/pull/837#issuecomment-6621822896)。

## 验证

### 第 2 次变基后（`54a7108`，基线 `e863fac`）

diff 与 `b4d321b` 逐字节相同（SHA-256 一致），所以只在新基线上重跑定向测试和反证。上游这次改了 core 的 agents、tools、workflow，反证用来确认 bug 在新 main 上仍然存在。复用第 1 轮的 Linux 副本，检出新提交并确认工作区干净、父提交为 `e863fac`，测试用 `--rerun` 强制重跑。

| 检查 | 结果 |
| --- | --- |
| LoadArtifactsToolTest 定向测试 | JVM 19/19、Android host 19/19，无跳过 |
| 反证：`LoadArtifactsTool.kt` 换回新 main 的版本 | 结果与第 1 轮相同：19 个测试 4 个失败，排在首位的场景仍通过。还原后工作区干净 |
| `git diff --check` | 通过 |

全仓库构建没有重跑：本 PR 的 diff 没有变化，上一次全仓库构建在 `b4d321b`（基线 `b961c9e`）上通过。

### 第 1 轮审阅后（`b4d321b`）

环境：Ubuntu 24.04 / WSL2、OpenJDK 21.0.12.1、Gradle wrapper 9.8.0、Android platform 37.0。从本地仓库新克隆一个独立 Linux 副本，检出该提交（父提交为基线），diff 哈希见 [validation.json](validation.json)。

| 检查 | 结果 |
| --- | --- |
| LoadArtifactsToolTest 定向测试 | JVM 19/19、Android host 19/19，无跳过 |
| 反证：`LoadArtifactsTool.kt` 换回 main 版本 | 19 个测试 4 个失败：排在另一个工具之后的场景、两个 `user:` 场景、多响应请求处理测试；排在首位的场景仍通过，与旧代码只读第一个 part 一致。还原后工作区干净 |
| 全仓库 `build` | 通过，耗时 35 分 40 秒；500 个任务，458 个执行，42 个复用同一副本中定向测试的结果 |
| 全仓库测试记录 | 598 个套件，10584 通过、0 失败、0 错误、18 条跳过记录、0 次 flaky |
| core JVM / Android host | 4902 通过（6 条跳过记录）/ 4502 通过 |
| Windows 上曾失败或超时的套件 | LocalEnvironmentTest 58/58、SkillToolsetEnvironmentTest 35/35、EnvironmentToolsetTest 24/24、McpToolsetIntegrationTest 22/22、McpResultSerializationIntegrationTest 1/1；McpAgentIntegrationTest 2 通过，另 6 条是缺少 `GOOGLE_API_KEY` 的跳过记录 |
| 构建后检查 | 工作区无改动；diff 哈希不变；没有残留 Gradle 进程；`git diff --check` 通过 |
| ktfmt 0.65 `--google-style` | 忽略右括号前的尾逗号后，新增行没有差异（原始比较主文件 1 处、测试 5 处，都是尾逗号）。校准：基线测试文件的 19 处差异全是尾逗号；主文件 2 处中 1 处是已有的超长行 |

```sh
./gradlew -PjdkVersion=21 "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g" -Dorg.gradle.parallel=false --max-workers=2 --no-daemon --console=plain --stacktrace --continue :google-adk-kotlin-core:jvmTest --tests com.google.adk.kt.tools.LoadArtifactsToolTest :google-adk-kotlin-core:testAndroidHostTest --tests com.google.adk.kt.tools.LoadArtifactsToolTest
./gradlew <同上参数> build
```

18 条跳过记录与之前相同：8 条 Firebase 集成用例缺少凭据，4 条 LiteRT-LM 用例未设置模型，2 个在线 Gemini MCP 方法各记录 3 次。没有加测试排除、禁用标志或测试内部超时覆盖，集成测试重试保持项目默认值，只降低了构建资源。构建期间用 `SetThreadExecutionState` 阻止 Windows 睡眠，没有出现待机。

### 首次提交（`433fbd2`，基线 `2809a83`）

- Windows 定向测试：JVM 17/17、Android host 17/17。
- 2026-11-01 Linux 全仓库构建通过：10529 通过、0 失败、18 条跳过记录。
- 那次构建 Gradle 报告耗时 10 小时 2 分：Windows 12:43–22:13 处于新型待机，WSL 虚拟机被挂起，实际只运行了约 32 分钟。长时间构建前应关闭睡眠和待机。
- 更早的 Windows 全仓库构建在 20 分钟期限后未完成。

真实后端/模型、Android 设备测试、JDK 17/25 矩阵和 Dokka（KDoc 未修改）未在本地运行。这是本地 Linux 结果，不代表官方 CI。

## 审核与后续

- 等待 Google 内部处理（`google support`）和 aravindmurali22 复审，以及维护者批准工作流。原计划的“11-05 @damianmomotgoogle”已不需要。
- 维护者可能在导入前再次要求变基：main 一有新提交，PR 就会落后。变基后先确认 diff 哈希不变，再在新基线上重跑定向测试。
- 后续修改仍保持单提交（amend + `--force-with-lease`），**不要加 `Co-authored-by` trailer**。只改与审阅相关的行，不要让本地 ktfmt 改动无关的行。