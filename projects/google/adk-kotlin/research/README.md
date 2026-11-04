# ADK Kotlin 研究记录

这份记录区分已提交问题、仍需确认的候选和历史验证草稿。检查日期为 2026-10-31，2026-11-02 补充新一轮筛选（Asia/Shanghai），状态变化以对应 PR 记录为准。

## 已形成贡献的问题

| 问题 | 结果 | 证据 |
| --- | --- | --- |
| artifact 内部路径冲突 | [PR #774](../prs/774-metadata-path/README.md)，已合并 | 最终项目任务 44+44 项通过 |
| 并行工具响应中的 artifact 未加载 | [PR #837](../prs/837-parallel-artifacts/README.md)，独立分支；第 1 轮审阅已处理，11-04 再次变基，已升级给 Google 内部（`google support`） | 两种排列顺序的对照；定向测试 19+19 项通过（在新 main 上重跑过），全仓库构建通过 |
| AgentTool 遗漏 RunConfig | [PR #838](../prs/838-agent-tool-config/README.md)，11-02 关闭 | Google 内部 #825 的最新版本包含相同修复，见下文 |
| Firebase 后端丢弃 response schema 与 penalty | [PR #856](../prs/856-firebase-response-schema/README.md)；第 1 轮审阅已处理，11-04 再次变基，已升级给 Google 内部（`google support`） | 78/78（在新 main 上重跑过），不含修复时 3 个失败；firebase 与 examples-android 构建通过 |

后两项选定问题的复现基线是 `2809a837752644f3720eaa759d2cb5144ba1a84e`。当时针对现有及历史 PR/issue 搜索了 AgentTool、RunConfig、maxLlmCalls、load_artifacts 等关键词，未发现匹配修复；这不排除未公开工作。

## 尚未提交的候选

| 候选 | 已有观察 | 状态与下一步 |
| --- | --- | --- |
| MLKit seed 丢失 | 正数 123 转换后为 0；2026-10-31 单项 Android host 断言失败。11-02 在 `db97db5` 上确认：`toGenerateContentRequest` 映射 temperature、topK、candidateCount、maxOutputTokens，但整个 mlkit 模块没有 `seed` | 修复只需一行；没有内部 PR 涉及 mlkit。体量小，可作为下一个小 PR |
| LiteRT-LM 忽略采样参数 | `toConversationConfig` 把 `samplerConfig`、`maxOutputToken` 写死为 null，temperature、topK、topP、seed、maxOutputTokens 都不生效 | 采样参数需要进入会话缓存键（DTO），改动涉及缓存设计；先确认 LiteRT-LM 能否按消息设置采样 |
| artifact 指令插值 | JVM/Android 得到 JSON 或调试表示，而 Python 使用可读文本 | 行为意图待确认；现有 Kotlin 测试明确接受 JSON，先确认预期 |
| MLKit TRACE 媒体内容 | 两项断言观察到原始图片字段及文件 URI 进入 TRACE | 日志策略待确认；方法文档将字段视为非敏感，不能据此断言已确认安全漏洞 |
| AgentTool 中间状态传播 | 存在相关历史 [PR #573](https://github.com/google/adk-kotlin/pull/573) | 重复候选，暂不另提；本地混合草稿初始化失败，不能作为状态丢失的执行证据 |
| MLKit 非流式 errorCode | 源码阅读中发现候选 | 尚无执行复现，保持待验证 |

有效选定复现和三个探索性复现的来源、适用基线及命令见 [repros/README.md](repros/README.md)。

## 2026-11-02 筛选

吸取 #838 的教训后，这一轮只在 Python 版没有对应实现、且没有内部 PR 在改的部分找问题：

- **避让清单**：记录 44 个开放内部导出 PR（copybara-service）改动的文件。它们集中在 core 的 tools、agents、processors、models、workflow 等目录，`firebase/`、`mlkit/`、`litertlm/` 都没有涉及。
- **协程取消**：AGENTS.md 要求重新抛出 `CancellationException`。用脚本扫描主代码中的宽泛 `catch` 和 `runCatching`，项目已有 106 处显式处理；剩下的命中都在清理或关闭连接的路径上，或在下一个挂起点就会再次抛出取消，没有找到值得提交的问题。
- **配置字段对照**：逐个核对各模型后端读取了 `GenerateContentConfig` 的哪些字段，再读源码排除误报（OpenAI 后端在扩展函数里处理了 `responseSchema`）。结果确认了 Firebase（#856）、ML Kit seed 和 LiteRT-LM 采样参数三处缺口。`GenerateContentConfig` 自身的 equals、hashCode、Builder、toBuilder 都完整。

#838 的时间线：#825 由内部在 10-29 导出，当时 GitHub 上的版本不含 `AgentTool` 改动；#838 于 10-31 提交；11-02 #825 重新同步后才包含 `AgentTool`（提交的作者时间仍是 10-29，内部 amend 不改作者时间，无法确定改动何时加入）。对应的 Python 修复是 2026-08-15 的 “run an agent tool under the caller's RunConfig” 和 08-26 的 “run AgentTool nested agent in unary mode”。#837 修的也是 Python 已修的差异（Python 09-12 “match tool response by name instead of assuming first part”），但目前没有内部 PR 涉及 `LoadArtifactsTool`。

## 早期验证与失效草稿

早期 FileArtifactService 孤立 JVM harness 使用真实 checkout 的实现与测试，并从已发布的 ADK core JVM 1.4.0 提供支持类。修复前 40 项中 3 项保留名断言失败，普通相似文件名对照通过；修复后 40 项通过。它不等同于项目原始任务图，也不是最终 PR #774 的 44+44 项验证。

早期 MLKit 孤立 harness 基于 `7d9248f6a8ef5aad6149d3bb12f762652b2af598`，编译真实转换实现及 `genai-prompt:1.0.0-beta3`，观察到 `null→0`、`0→0`、`123→0`。前两个对照通过，正数 seed 断言失败；这仅验证请求转换，不验证模型输出确定性。10 月 31 日的一测试版本又在新基线上观察到该问题。

`AgentToolIntermediateStateBugReproTest.kt` 的历史混合类有非 Unit 返回的 JUnit 方法，整类出现初始化错误。它不用于证明状态或 RunConfig 缺陷；RunConfig 后来使用显式 Unit 的独立类和正向对照完成有效复现。

另有一份同名 `GenaiPromptSeedRegressionTest.kt` 三测试草稿未执行，未收录为执行证据。格式化中间文件、含本机路径的 harness 配置及原始失败数组保存在本地入口，公开档案采用重写的结论和有效小型草稿。

## 验证边界

- 原始日志、XML、线程快照和下载工具通过本地 `.cache/` 入口保留。
- 原始失败数组可能包含编码后的本机 URI，不能仅依赖普通路径文本搜索来清理。
- 复现测试有意在旧基线上失败；修复后的验证以各 PR 记录中的实际提交和结果为准。
- 此次归档只整理历史执行证据，没有重新运行这些研究测试。
