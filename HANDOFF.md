# VS Code 工作交接

交接日期：2026-10-31，最近更新 2026-11-06（Asia/Shanghai）。本文提供接手入口和工作顺序；各 PR 的详细事实集中在自己的记录中，实时状态以 GitHub 为准。

## 目标与已完成工作

维护跨项目开源贡献档案，并继续处理 Google ADK Kotlin、SGLang 与 vLLM 的已提交 PR、审核意见及后续候选问题。档案仓库为 [GCSimba/open-source-contributions](https://github.com/GCSimba/open-source-contributions)，初始归档提交为 `f34357d8e81c63acf2725284ad901cbac959f28f`。

已整理三个 PR 的问题、补丁、验证摘要、审核结果，以及五份有效历史复现草稿。链接、JSON、补丁来源、哈希和公开内容检查已通过。实际源码、工具和完整原始证据保留在独立目录，通过忽略的本地入口关联。

本仓库用于管理档案；源码修复在对应项目的独立 Git 仓库进行。未来项目按 `projects/<owner>/<repository>/` 扩展，使用 [PR 模板](templates/pr.md)。

## 接手先做这些

1. 读取 [AGENTS.md](AGENTS.md)、[ADK 项目入口](projects/google/adk-kotlin/README.md) 和目标 PR 的记录。
2. 本机读取 `HANDOFF.local.md`、`workspace.local.json`。新机器先根据 [workspace.example.json](workspace.example.json) 配置实际路径，再准备源码和依赖。
3. 分别确认档案与源码的 Git 根目录、分支、HEAD、远端和未提交修改；不要仅凭文件夹名称判断项目。
4. 操作源码前读取该 checkout 自己的 `AGENTS.md`、`CONTRIBUTING.md` 和 `.agents/skills/adk-kotlin-setup/SKILL.md`。
5. 用 GitHub 当前数据刷新开放 PR 的评论、审核和 CI，再决定需要处理什么。状态查询保持只读。

从档案根目录执行的 PowerShell 入口：

```powershell
Get-Content .\AGENTS.md
$handoffConfig = Get-Content .\workspace.local.json -Raw | ConvertFrom-Json
$adkProject = $handoffConfig.projects.'google/adk-kotlin'
git status --short --branch
git -C $adkProject.checkout rev-parse --show-toplevel
git -C $adkProject.checkout status --short --branch
git -C $adkProject.checkout log -1 --oneline
git -C $adkProject.checkout remote -v
```

## PR 快照

以下状态核验于 **2026-11-06 02:00（Asia/Shanghai）**。

| PR | 状态 | 分支 / 修复提交 | 详细记录 |
| --- | --- | --- | --- |
| [#774](https://github.com/google/adk-kotlin/pull/774) | 已于 10 月 29 日合并 | `codex/reject-metadata-artifact-filenames` / `2bd0480` | [保留路径校验](projects/google/adk-kotlin/prs/774-metadata-path/README.md) |
| [#837](https://github.com/google/adk-kotlin/pull/837) | Open，标签 `google support`（已升级给 Google 内部团队）。第 1 轮意见已处理；11-05 维护者要求处理失败的检查并持续变基，11-06 变基到 `e665796`、推送并评论，新工作流等维护者批准 | `codex/fix-parallel-artifact-loading` / `a963450` | [并行 artifact 加载](projects/google/adk-kotlin/prs/837-parallel-artifacts/README.md) |
| [#838](https://github.com/google/adk-kotlin/pull/838) | 11-02 已关闭，未合并：Google 内部的 #825 包含相同修复 | `codex/inherit-agent-tool-run-config` / `5a24148` | [RunConfig 继承](projects/google/adk-kotlin/prs/838-agent-tool-config/README.md) |
| [#856](https://github.com/google/adk-kotlin/pull/856) | Open，标签 `google support`。第 1 轮意见已处理；11-05 维护者要求处理失败的检查并持续变基，11-06 变基到 `e665796`、推送并评论，新工作流等维护者批准 | `fix/firebase-response-schema` / `3ee76e1` | [Firebase response schema](projects/google/adk-kotlin/prs/856-firebase-response-schema/README.md) |

11-04 的 validation 两个 PR 各有一个 job 失败（#837 JDK 25，JDK 17/21 随即被取消；#856 JDK 21，JDK 17/25 通过），都挂在 main 上已有的不稳定超时测试：`KtorChatCompletionsClientTest.create_timeoutClosesTheConnectionWhileTheResponseStalls` 和 `McpToolsetIntegrationTest.run_unresponsiveServer_timesOutThenThrowsAfterRetries`。11-05 02:18 aravindmurali22 在两个 PR 上要求处理失败的检查，并在合并前持续变基。11-04 00:00 至 11-05 17:06（UTC），仓库 85 次跑完的 validation 中 21 次失败，全部来自四个不稳定测试（另外两个是 `LiveRoutesTest` 和 `WorkflowHitlTest`），其中 19 次在其他 PR 上。11-06 01:53 经用户确认，两个 PR 变基推送并各发一条评论，附上统计和运行链接，请维护者在新一轮再撞上时重跑；两个 PR 描述里“After rebasing onto e863fac”一句未更新，已过时。重跑需要仓库写权限。个人 CLA 已完成，但 CLA 检查覆盖所有共同作者：ADK 提交不要加 `Co-authored-by` trailer（#856 首次推送因此失败，amend 去掉后通过），AI 辅助写在 PR 描述里（两个 PR 都已写明）。外部贡献者不能重新请求审阅（REST 404、GraphQL FORBIDDEN），处理完意见后在讨论串逐条回复，并发一条顶层评论说明。

读取时，实际 ADK checkout 位于 #837 分支 `codex/fix-parallel-artifact-loading`、HEAD 为 `a96345005e6b14173167c31dda966d88035bb6da`，跟踪 `fork/codex/fix-parallel-artifact-loading`，工作区干净；#856 分支 `fix/firebase-response-schema` 与 fork 同步于 `3ee76e18`。源码仓库的 `origin` 是 Google 上游、`fork` 是 GCSimba 的 fork；档案仓库的 `origin` 则是本档案仓库。

#838 已关闭，不要重新打开或重复提交同一修复。#838 的基线为 `2809a837752644f3720eaa759d2cb5144ba1a84e`；#837、#856 已按审阅要求变基到 `e66579634f6a12630e1ac1e31ef6953aff4f5a01`，各分支互不包含对方补丁。Google 内部变更由 copybara-service 导出为 PR，GitHub 上的版本可能落后；选题和查重时要检查这些 PR 的最新版本。

## 验证结论与限制

- #774：最终 JVM44/44、Android host44/44、Dokka 和格式/差异检查通过；Windows 全仓库构建未完成。早期孤立 harness 的40项检查是另一组历史证据。
- #837：11-06 变基到 `e665796` 后（`a963450`，diff 与 `54a7108`、`b4d321b` 逐字节相同），LoadArtifactsToolTest JVM 19/19、Android host 19/19，反证仍是同样 4 个失败。11-04 变基到 `e863fac` 后（`54a7108`）结果相同。第 1 轮审阅后（`b4d321b`），独立 Linux 副本中 LoadArtifactsToolTest JVM 19/19、Android host 19/19；把 `LoadArtifactsTool.kt` 换回 main 版本后 4 个新/旧回归失败、首位场景仍通过。全仓库构建（在 `b4d321b` 上）通过：500 个任务，458 个执行、42 个复用同一副本定向测试的结果，10584 通过、0 失败、18 条原始 XML 跳过记录。首次提交 `433fbd2` 的 Linux 全量结果（10529 通过）见 PR 记录。
- #838：JVM/Android host 的 AgentToolTest 均31/31；Ubuntu24.04/WSL2、JDK21的完整 core 与仓库构建通过。完整仓库汇总10531通过、0失败、18条原始XML跳过记录，包含已完成 core 结果的复用。
- #856：11-06 变基到 `e665796` 后（`3ee76e1`，diff 与 `fc22165`、`5ddf267` 逐字节相同），`ConversionsTest` 78/78；11-04 变基到 `e863fac` 后（`fc22165`）结果相同。第 1 轮审阅后（`5ddf267`），独立 Linux 副本中 `ConversionsTest` 78/78；把 `Conversions.kt` 换回 main 版本后 3 个失败（schema、penalty、union/enum）。`firebase` 与唯一依赖它的 `examples-android` 模块构建（在 `5ddf267` 上）通过（Firebase 单元测试 113 个、0 失败，8 个集成测试缺少凭据而跳过）。
- 格式：上游格式化器不给“单个多行参数”加尾逗号，ktfmt 0.65 会加，基线文件本身就不通过 ktfmt 0.65。两个 PR 都按“忽略右括号前尾逗号后，新增行无差异”核对，并先用基线文件校准；不要让本地 ktfmt 改动无关的行（这正是 #837 第 1 轮意见之一）。

两个分支各有自己的 Linux 全量结果，不能互相替代。外部凭据/本地模型相关跳过、设备测试及 JDK17/25矩阵等未验证范围详见各 PR 的 validation.json。本地通过也不表示官方 CI 已运行。

## 下一步顺序

1. 先刷新 #837、#856 的审核与 CI：等维护者批准 11-06 推送后的工作流、Google 内部处理和 aravindmurali22 复审；目前没有正在执行的本地构建。新一轮 validation 若又挂在不稳定测试上，评论里已请维护者重跑，不必再催。维护者要求合并前持续变基：main 有新提交时，变基后先确认 diff 哈希不变，在新基线上重跑定向测试（#837 加反证），推送和回复前经用户确认（本机交接有现成脚本）。两个 PR 描述里“After rebasing onto e863fac”一句已过时，下次变基时可一并更新（经用户确认）。
2. #838 已关闭，无后续操作。若 #825 合入后仍想补“空配置仍为空”“子运行独立计数”两个测试，先征求用户意见，作为新的只含测试的 PR。下一个候选是 ML Kit seed（一行修复），见[研究记录](projects/google/adk-kotlin/research/README.md)。另一个候选是修 CI 的不稳定测试（研究记录里有 11-04 至 11-05 的统计），首选 `McpToolsetIntegrationTest`；`LiveRoutesTest` 刚由内部作者引入，很可能自己修，暂不碰。动手前先确认没有人在修，并征求用户意见。
3. 收到审核意见后，在对应源码分支修复，运行受影响检查，再按项目规则更新同一个 PR；ADK 要求一个提交，改历史使用 `--force-with-lease`，发现远端移动先停下来核对。
4. 每次状态或验证变化更新目标 PR 的 README/验证摘要，再提交到档案仓库。

查询进展不触发评论、@维护者或请求审核；发布与沟通按用户当前指令执行。不需要仅为再次提交或推送而重跑源码未变化的已通过检查。

## 研究候选与原始证据

[研究入口](projects/google/adk-kotlin/research/README.md) 已区分 seed 丢失、文本插值和 TRACE 日志等候选的确认程度。中间状态传播已有相关 PR；历史混合 JUnit 类初始化失败，不能用作缺陷证明。另一份三测试 seed 草稿未执行。不要把有意在旧基线上失败的复现当作修复后验证。

公开补丁和复现的来源/哈希见 [artifacts.json](projects/google/adk-kotlin/artifacts.json)。完整日志、XML、线程快照、SDK、CLA资料保存在本机 `.cache/` 入口。目录联接及本地配置文件均被 Git 忽略；公开文件使用相对路径与清理过的摘要。

## 验证环境提示

Linux 曾缺少 build-tools36.0.0，SDK自动下载也曾超时；补齐官方组件后 #838 两次完整构建通过。当前环境已有 Android platform37.0、build-tools36.0.0/37.0.0、JDK21和共享 Gradle 缓存。

Gradle 默认堆设置很大，本地验证采用3GB堆、1GB metaspace、2 workers并关闭项目并行；没有给成功的 Linux 检查加测试排除或禁用标志。Windows旧MCP全量超时根因未确认，不能统一归因为 Windows；独立探针和 Linux 本地 MCP 测试均通过。实际环境路径和命令见仅本地的交接记录。

长时间构建前关闭 Windows 睡眠和新型待机：待机时 WSL 虚拟机会被挂起。2026-11-01 的 #837 构建因此报告耗时 10 小时，实际只运行了约 32 分钟。判断构建是否卡住时，先看 Windows Kernel-Power 事件（506/507）和构建产物的修改时间。

## SGLang

2026-10-31 新增 [SGLang 项目档案](projects/sgl-project/sglang/README.md)。以下状态核验于 **2026-11-04 10:57（Asia/Shanghai）**：

| PR | 状态 | 分支 / 修复提交 | 详细记录 |
| --- | --- | --- | --- |
| [#47649](https://github.com/sgl-project/sglang/pull/47649) | Open，等待审核；11-02 已 @ 评分模块负责人 sundar24295s，暂无回应 | `fix/setwise-token-logprobs` / `6d97319` | [setwise token_logprobs](projects/sgl-project/sglang/prs/47649-setwise-token-logprobs/README.md) |

- 官方 CI 未运行：门禁因缺少 `run-ci` 标签失败（17 个红叉都是门禁及其汇总任务，测试任务全部跳过）；这不是代码失败。只有 `CI_PERMISSIONS.json` 中的人能加标签，外部贡献者不能自己添加审阅者或标签。
- 本地验证：Ubuntu 24.04 / WSL2、Python 3.12、仅 CPU，两个评分测试文件 62 通过，下游相关测试 75 通过，改动文件的格式与静态检查通过。完整 pre-commit、完整 CPU 套件、真实模型端到端和 GPU CI 未运行。
- SGLang 源码的 `origin` 是上游、`fork` 是 GCSimba 的 fork。测试必须在 Linux 中运行；Windows 本机缺少 `resource` 模块且 Python 版本不足。
- 不要在 WSL 中对 Windows checkout 直接运行 `pre-commit run`：换行符差异会让 Git 把全仓库视为已修改。按 PR 记录，直接对改动文件运行同版本工具。
- 下一步：等待 sundar24295s（评分模块 Codeowner、#41188 作者、可加 `run-ci`）回应。若 11-05 前仍无回应，可 @ 相关 Merge Oncall，或由用户在 Slack `#pull-request` 询问；发言先经用户确认。收到审核意见后在源码分支修改并推送到同一分支。

## vLLM

2026-11-01 新增 [vLLM 项目档案](projects/vllm-project/vllm/README.md)。以下状态核验于 **2026-11-06 22:50（Asia/Shanghai）**：

| PR | 状态 | 分支 / 最新提交 | 详细记录 |
| --- | --- | --- | --- |
| [#64935](https://github.com/vllm-project/vllm/pull/64935) | **2026-11-04 22:58 已合并**：DarkLight1337 批准、开启自动合并，squash 进 main 的提交为 `e4991c1`，内容与档案补丁一致 | `fix/batch-chat-reasoning-tokens` / `b068fa3`（维护者合入 main） | [批量 reasoning tokens](projects/vllm-project/vllm/prs/64935-batch-reasoning-tokens/README.md) |
| [#65697](https://github.com/vllm-project/vllm/pull/65697) | **2026-11-06 15:05 已合并**：第 1 轮审阅后改为计数专用请求类型；DarkLight1337 先用 `@claude review` 审过（没有问题），再批准、加 `ready`、开启自动合并并 `/ci run`。Buildkite #100475 通过后，squash 进 main，提交为 `fd77011`，内容与档案补丁一致 | `fix/anthropic-count-tokens-over-limit` / `f0e34b5`（机器人合并 main 后为 `9868368`） | [count_tokens 超长计数](projects/vllm-project/vllm/prs/65697-count-tokens-over-limit/README.md) |

- 11-02 审阅者要求不改 Buildkite YAML，改为在测试中按 `VLLM_USE_RUST_FRONTEND` 跳过。已在 `f062ee2` 中加 `skipif` 并撤回两处 YAML 改动，回复后审阅者于 21:09 批准，并评论 `/ci run`。
- 本地验证：Ubuntu 24.04 / WSL2、Python 3.12、仅 CPU。最终回归测试修复前失败、修复后通过；审核修改后 `test_include_reasoning.py` 7 通过，设置 `VLLM_USE_RUST_FRONTEND=1` 时新测试被跳过，pre-commit 通过。GPU/ROCm CI 由 Buildkite 覆盖。
- vLLM 要求每个提交带 DCO `Signed-off-by`，并在 PR 与提交 trailer 中声明 AI 辅助；签名须由用户本人确认。PR 机器人要求 AI agent 依据 AGENTS.md 复核 PR 价值，发布后已复核并保留。合并后的 squash 提交保留了这两个 trailer。
- 源码的 `origin` 是上游、`fork` 是 GCSimba 的 fork。验证使用 WSL Linux 文件系统中的源码镜像加 nightly CPU wheel 的编译扩展；修改源码后需重新同步镜像，具体命令见本机交接。
- CI 卡住与合并经过：第一次完整 CI（Buildkite #99182）只有一个 AMD MI355 分片失败，原因是 main 上的测试缓存泄漏，在机器人同步我们分支 8 分钟后由 #65140 修复；Read the Docs 的警告也在同一时段由 #65157 修复。自动合并只差 `buildkite/ci/pr` 变绿，而作者不能在同一提交上重跑 CI，自己推送又会让 GitHub 关闭自动合并。11-04 22:02 经用户确认评论说明原因；22:26 DarkLight1337 更新分支并 `/ci retry`，Buildkite #99937 通过后于 22:58 自动合并。经验已写进 [vLLM 项目档案](projects/vllm-project/vllm/README.md) 的贡献要点。
- 后续：#64935 没有待办。fork 上的分支可在用户确认后删除。
- 11-04 第三轮筛选后提交 #65697：Anthropic `count_tokens` 原先对超过 `max_model_len` 的提示词返回 400，改为与 `/tokenize` 一样计数。11-06 按第 1 轮审阅改为计数专用请求类型后合并（`fd77011`），没有待办。两个已合并 PR 在 fork 上的分支是否删除，由用户决定。
- 11-06 第四轮筛选（nightly `a2bcb56`）：
  - 一致性探针发现的 5 个问题都已有人处理，研究代理给出的 2 个候选都不成立。
  - 最有价值的是 `previous_response_id` 续接时丢工具调用（#43244），但被维护者搁置。11-06 经用户确认在 issue 中[留言](https://github.com/vllm-project/vllm/issues/43244#issuecomment-6687572944)，询问现在是否接受最小修复，正在等回复。
  - 如果维护者同意，先问原作者 ashwing 是否重开 #43247，他不接再由我们接手并注明他的贡献。
  - 详见 [vLLM 研究记录](projects/vllm-project/vllm/research/README.md)。
- 11-02 第二轮找题暂停：找到的真实问题都已有 PR，详见 [vLLM 研究记录](projects/vllm-project/vllm/research/README.md)。`/v1/completions` 缺少 `created_cache_tokens` 可等 #58116 合并后再提。
- 11-03 另一位贡献者的 [#65383](https://github.com/vllm-project/vllm/pull/65383) 引用了本 PR：它给 `/inference/v1/generate` 的 derender 模式补 `reasoning_tokens`，是不同接口的同类修复，不冲突。

## vLLM agentic-api

2026-11-06 新增 [agentic-api 项目档案](projects/vllm-project/agentic-api/README.md)。这是 vLLM 组织下的有状态 Agent 网关，Rust 为主，对外部贡献者友好：近 30 天合并的 PR 中 61% 来自外部，合并中位数约 3.7 天。以下状态核验于 **2026-11-06 22:50（Asia/Shanghai）**：

| issue | 状态 | 下一步 |
| --- | --- | --- |
| [#312](https://github.com/vllm-project/agentic-api/issues/312) | 11-06 留言认领 `crates/agentic-server/src/agentic_process.rs` 的拆分（good first issue，基线 557 行） | 把 harness 函数移到 `agentic_process/harness.rs`，并删除这个文件的基线条目。跑通 `cargo test`、`cargo clippy --all-targets -- -D warnings`、`cargo fmt -- --check` 和 pre-commit 后，先给用户看，再建 fork 并提 PR（DCO `-s`，conventional 标题） |
| [#482](https://github.com/vllm-project/agentic-api/issues/482) | 11-06 留言询问 Than0316 和维护者 haoshan98 能否由我们接手 | 等回复。如果可以接手，按 issue 中的方向修复，并补续接测试 |

源码克隆在 WSL，工具链和本机路径见本机交接。
