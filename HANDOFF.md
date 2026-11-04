# VS Code 工作交接

交接日期：2026-10-31，最近更新 2026-11-04（Asia/Shanghai）。本文提供接手入口和工作顺序；各 PR 的详细事实集中在自己的记录中，实时状态以 GitHub 为准。

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

以下状态核验于 **2026-11-04 14:00（Asia/Shanghai）**。

| PR | 状态 | 分支 / 修复提交 | 详细记录 |
| --- | --- | --- | --- |
| [#774](https://github.com/google/adk-kotlin/pull/774) | 已于 10 月 29 日合并 | `codex/reject-metadata-artifact-filenames` / `2bd0480` | [保留路径校验](projects/google/adk-kotlin/prs/774-metadata-path/README.md) |
| [#837](https://github.com/google/adk-kotlin/pull/837) | Open，标签 `google support`（已升级给 Google 内部团队）。第 1 轮意见已处理；11-04 按要求再次变基到 `e863fac` 并回复，等待内部处理和复审 | `codex/fix-parallel-artifact-loading` / `54a7108` | [并行 artifact 加载](projects/google/adk-kotlin/prs/837-parallel-artifacts/README.md) |
| [#838](https://github.com/google/adk-kotlin/pull/838) | 11-02 已关闭，未合并：Google 内部的 #825 包含相同修复 | `codex/inherit-agent-tool-run-config` / `5a24148` | [RunConfig 继承](projects/google/adk-kotlin/prs/838-agent-tool-config/README.md) |
| [#856](https://github.com/google/adk-kotlin/pull/856) | Open，标签 `google support`。第 1 轮意见已处理；11-04 按要求再次变基到 `e863fac` 并回复，等待内部处理和复审 | `fix/firebase-response-schema` / `fc22165` | [Firebase response schema](projects/google/adk-kotlin/prs/856-firebase-response-schema/README.md) |

开放的 #837 与 #856 在新提交上都已通过 CLA 和 `check-changes`。11-04 12:58 维护者批准工作流后，标题、提交和安全检查都通过；validation 两个 PR 各有一个 job 失败（#837 JDK 25，JDK 17/21 随即被取消；#856 JDK 21，JDK 17/25 通过），都挂在 main 上已有的不稳定超时测试：`KtorChatCompletionsClientTest.create_timeoutClosesTheConnectionWhileTheResponseStalls` 和 `McpToolsetIntegrationTest.run_unresponsiveServer_timesOutThenThrowsAfterRetries`。同一天别的 PR 和内部导出分支也有 4 次运行挂在这两个测试上，与本仓库的改动无关。重跑需要仓库写权限；用户决定暂不评论，等维护者处理。11-04 维护者把两个 PR 的 `waiting on author` 换成了 `google support`，并要求变基到最新 main，导入前可能还会再要求一次。个人 CLA 已完成，但 CLA 检查覆盖所有共同作者：ADK 提交不要加 `Co-authored-by` trailer（#856 首次推送因此失败，amend 去掉后通过），AI 辅助写在 PR 描述里（两个 PR 都已写明）。外部贡献者不能重新请求审阅（REST 404、GraphQL FORBIDDEN），处理完意见后在讨论串逐条回复，并发一条顶层评论说明。

读取时，实际 ADK checkout 位于 #837 分支 `codex/fix-parallel-artifact-loading`、HEAD 为 `54a710809ac0aefb11aae3246caaaecdd2744f42`，跟踪 `fork/codex/fix-parallel-artifact-loading`，工作区干净；#856 分支 `fix/firebase-response-schema` 与 fork 同步于 `fc221657`。源码仓库的 `origin` 是 Google 上游、`fork` 是 GCSimba 的 fork；档案仓库的 `origin` 则是本档案仓库。

#838 已关闭，不要重新打开或重复提交同一修复。#838 的基线为 `2809a837752644f3720eaa759d2cb5144ba1a84e`；#837、#856 已按审阅要求变基到 `e863facc7faa6ec0b91b2d9080e68bd5d22fda2f`，各分支互不包含对方补丁。Google 内部变更由 copybara-service 导出为 PR，GitHub 上的版本可能落后；选题和查重时要检查这些 PR 的最新版本。

## 验证结论与限制

- #774：最终 JVM44/44、Android host44/44、Dokka 和格式/差异检查通过；Windows 全仓库构建未完成。早期孤立 harness 的40项检查是另一组历史证据。
- #837：11-04 变基到 `e863fac` 后（`54a7108`，diff 与 `b4d321b` 逐字节相同），LoadArtifactsToolTest JVM 19/19、Android host 19/19；反证仍是同样 4 个失败，说明 bug 在新 main 上依然存在。第 1 轮审阅后（`b4d321b`），独立 Linux 副本中 LoadArtifactsToolTest JVM 19/19、Android host 19/19；把 `LoadArtifactsTool.kt` 换回 main 版本后 4 个新/旧回归失败、首位场景仍通过。全仓库构建（在 `b4d321b` 上）通过：500 个任务，458 个执行、42 个复用同一副本定向测试的结果，10584 通过、0 失败、18 条原始 XML 跳过记录。首次提交 `433fbd2` 的 Linux 全量结果（10529 通过）见 PR 记录。
- #838：JVM/Android host 的 AgentToolTest 均31/31；Ubuntu24.04/WSL2、JDK21的完整 core 与仓库构建通过。完整仓库汇总10531通过、0失败、18条原始XML跳过记录，包含已完成 core 结果的复用。
- #856：11-04 变基到 `e863fac` 后（`fc22165`，diff 与 `5ddf267` 逐字节相同），`ConversionsTest` 78/78。第 1 轮审阅后（`5ddf267`），独立 Linux 副本中 `ConversionsTest` 78/78；把 `Conversions.kt` 换回 main 版本后 3 个失败（schema、penalty、union/enum）。`firebase` 与唯一依赖它的 `examples-android` 模块构建（在 `5ddf267` 上）通过（Firebase 单元测试 113 个、0 失败，8 个集成测试缺少凭据而跳过）。
- 格式：上游格式化器不给“单个多行参数”加尾逗号，ktfmt 0.65 会加，基线文件本身就不通过 ktfmt 0.65。两个 PR 都按“忽略右括号前尾逗号后，新增行无差异”核对，并先用基线文件校准；不要让本地 ktfmt 改动无关的行（这正是 #837 第 1 轮意见之一）。

两个分支各有自己的 Linux 全量结果，不能互相替代。外部凭据/本地模型相关跳过、设备测试及 JDK17/25矩阵等未验证范围详见各 PR 的 validation.json。本地通过也不表示官方 CI 已运行。

## 下一步顺序

1. 先刷新 #837、#856 的审核与 CI，等待 Google 内部处理、aravindmurali22 复审，以及维护者重跑失败的 validation job；目前没有正在执行的本地构建。原定 11-05 @damianmomotgoogle 的计划已不需要。若维护者再次要求变基：变基后先确认 diff 哈希不变，在新基线上重跑定向测试（#837 加反证），推送和回复前经用户确认。若之后想推动重跑，评论草稿见本机交接（经用户确认再发，发之前先确认还没有人重跑）。
2. #838 已关闭，无后续操作。若 #825 合入后仍想补“空配置仍为空”“子运行独立计数”两个测试，先征求用户意见，作为新的只含测试的 PR。下一个候选是 ML Kit seed（一行修复），见[研究记录](projects/google/adk-kotlin/research/README.md)。另一个可能的候选是修上面两个不稳定的超时测试（它们也在拖累 Google 自己的 PR），动手前先确认没有人在修，并征求用户意见。
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

2026-11-01 新增 [vLLM 项目档案](projects/vllm-project/vllm/README.md)。以下状态核验于 **2026-11-04 22:03（Asia/Shanghai）**：

| PR | 状态 | 分支 / 最新提交 | 详细记录 |
| --- | --- | --- | --- |
| [#64935](https://github.com/vllm-project/vllm/pull/64935) | **已获 DarkLight1337 批准**，带 `ready` 标签，并已开启自动合并（squash）；唯一失败的必需检查 `buildkite/ci/pr` 来自 AMD 分片，原因在 main 上已由 #65140 修复；11-04 已评论请他再 `/ci run` | `fix/batch-chat-reasoning-tokens` / `2a40ba7`（机器人合入 main） | [批量 reasoning tokens](projects/vllm-project/vllm/prs/64935-batch-reasoning-tokens/README.md) |

- 11-02 审阅者要求不改 Buildkite YAML，改为在测试中按 `VLLM_USE_RUST_FRONTEND` 跳过。已在 `f062ee2` 中加 `skipif` 并撤回两处 YAML 改动，回复后审阅者于 21:09 批准，并评论 `/ci run`。
- 分支当时落后 main 50 个提交，机器人把 main 合入分支（合并提交 `2a40ba7`，作者 vllm-agent）后才启动 CI。本地分支已快进到 `2a40ba7`；以后推送前必须先拉取远端分支。
- 加上 `ready` 标签后，pre-run-check、pre-commit、Check format 均通过；DCO 通过。Read the Docs 失败，原因是 main 上 #63440、#64029 新增的 ROCm kernel docstring 在 `mkdocs --strict` 下产生 11 条交叉引用警告，与本 PR 无关。
- 本地验证：Ubuntu 24.04 / WSL2、Python 3.12、仅 CPU。最终回归测试修复前失败、修复后通过；审核修改后 `test_include_reasoning.py` 7 通过，设置 `VLLM_USE_RUST_FRONTEND=1` 时新测试被跳过，pre-commit 通过。GPU/ROCm CI 和需要 Qwen2.5-1.5B 的服务测试未在本地运行，由 Buildkite 覆盖。
- vLLM 要求每个提交带 DCO `Signed-off-by`，并在 PR 与提交 trailer 中声明 AI 辅助；签名须由用户本人确认。PR 机器人要求 AI agent 依据 AGENTS.md 复核 PR 价值，发布后已复核并保留。
- 源码的 `origin` 是上游、`fork` 是 GCSimba 的 fork。验证使用 WSL Linux 文件系统中的源码镜像加 nightly CPU wheel 的编译扩展；修改源码后需重新同步镜像，具体命令见本机交接。
- 11-03 已查明 Buildkite #99182 的失败：只有 `amd-mi355-basic-models-other-shard-2-slash-2` 失败（26 failed、597 passed），26 个失败全是 `models/test_muse_spark_rocm_mono.py` 中同一个 HIP kernel 错误（`cos_sin_cache must be a contiguous 2-D CUDA tensor`），与本 PR 的 4 个前端/测试文件无关。11-04 进一步查明，这是测试间的缓存泄漏，main 上已由 #65140 修复（在机器人同步我们分支 8 分钟后合入）。
- 卡住的原因与处理（11-04）：自动合并已开启，只差 `buildkite/ci/pr` 变绿。作者在同一提交上不能再 `/ci run`，`/ci retry` 会在旧快照上再次失败，自己推送又会让 GitHub 关掉自动合并。因此经用户确认，评论请 DarkLight1337 再 `/ci run`。下一步：等他重跑；机器人会先合入最新 main，CI 通过后自动合并。若需要在源码分支修改，带 `-s` 提交，先拉取机器人的合并提交，推送和回复先经用户确认。
- 11-02 第二轮找题暂停：找到的真实问题都已有 PR，详见 [vLLM 研究记录](projects/vllm-project/vllm/research/README.md)。`/v1/completions` 缺少 `created_cache_tokens` 可等 #58116 合并后再提。
- 11-03 用户决定暂不在 PR 下说明 CI 失败；11-04 查清原因后改为评论请求重跑（见上）。同日另一位贡献者的 [#65383](https://github.com/vllm-project/vllm/pull/65383) 引用了本 PR：它给 `/inference/v1/generate` 的 derender 模式补 `reasoning_tokens`，是不同接口的同类修复，不冲突。
