# VS Code 工作交接

交接日期：2026-10-31（Asia/Shanghai）。本文提供接手入口和工作顺序；各 PR 的详细事实集中在自己的记录中，实时状态以 GitHub 为准。

## 目标与已完成工作

维护跨项目开源贡献档案，并继续处理 Google ADK Kotlin 与 SGLang 的已提交 PR、审核意见及后续候选问题。档案仓库为 [GCSimba/open-source-contributions](https://github.com/GCSimba/open-source-contributions)，初始归档提交为 `f34357d8e81c63acf2725284ad901cbac959f28f`。

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

以下状态核验于 **2026-10-31 20:39（Asia/Shanghai）**。

| PR | 状态 | 分支 / 修复提交 | 详细记录 |
| --- | --- | --- | --- |
| [#774](https://github.com/google/adk-kotlin/pull/774) | 已于 10 月 29 日合并 | `codex/reject-metadata-artifact-filenames` / `2bd0480` | [保留路径校验](projects/google/adk-kotlin/prs/774-metadata-path/README.md) |
| [#837](https://github.com/google/adk-kotlin/pull/837) | Open，暂无评论或审核 | `codex/fix-parallel-artifact-loading` / `433fbd2` | [并行 artifact 加载](projects/google/adk-kotlin/prs/837-parallel-artifacts/README.md) |
| [#838](https://github.com/google/adk-kotlin/pull/838) | Open，暂无评论或审核 | `codex/inherit-agent-tool-run-config` / `5a24148` | [RunConfig 继承](projects/google/adk-kotlin/prs/838-agent-tool-config/README.md) |

两个开放 PR 的 CLA、`check-changes` 已通过，validation、标题、提交和安全工作流显示 `action_required`，等待维护者操作。个人 CLA 已完成，无需因新 PR 重复签署。

读取时，实际 ADK checkout 位于 #838 分支、HEAD 为 `5a24148a114f1736fae7ae12bcd5817e50e668ab`，跟踪 `fork/codex/inherit-agent-tool-run-config`，工作区干净。源码仓库的 `origin` 是 Google 上游、`fork` 是 GCSimba 的 fork；档案仓库的 `origin` 则是本档案仓库。

#838 已经按用户后续确认推送并创建独立 PR，不要重复创建。#837 与 #838 的共同基线为 `2809a837752644f3720eaa759d2cb5144ba1a84e`，两个分支互不包含对方补丁。

## 验证结论与限制

- #774：最终 JVM44/44、Android host44/44、Dokka 和格式/差异检查通过；Windows 全仓库构建未完成。早期孤立 harness 的40项检查是另一组历史证据。
- #837：JVM17/17、Android host17/17和格式检查通过；Windows 全量构建在20分钟后未完成，使用过只排除一个已知无限等待用例的本地 init 脚本。尚无本分支完整 Linux 构建通过记录。
- #838：JVM/Android host 的 AgentToolTest 均31/31；Ubuntu24.04/WSL2、JDK21的完整 core 与仓库构建通过。完整仓库汇总10531通过、0失败、18条原始XML跳过记录，包含已完成 core 结果的复用。

#838 的成功不能作为 #837 的通过证据。外部凭据/本地模型相关跳过、设备测试及 JDK17/25矩阵等未验证范围详见 [#838 验证记录](projects/google/adk-kotlin/prs/838-agent-tool-config/validation.json)。本地通过也不表示官方 CI 已运行。

## 下一步顺序

1. 先刷新 #837、#838 的审核与 CI；目前没有待修的审核意见，也没有正在执行的本地构建。
2. 一项有价值的独立后续工作是为 #837 补 Linux 完整验证：使用单独工作副本和该 PR 的实际提交，复用工具缓存，并按上游要求运行检查。现有 Linux 验证副本包含 #838 的改动，不能直接当成 #837 环境。
3. 收到审核意见后，在对应源码分支修复，运行受影响检查，再按项目规则更新同一个 PR；ADK 要求一个提交，改历史使用 `--force-with-lease`，发现远端移动先停下来核对。
4. 每次状态或验证变化更新目标 PR 的 README/验证摘要，再提交到档案仓库。

查询进展不触发评论、@维护者或请求审核；发布与沟通按用户当前指令执行。不需要仅为再次提交或推送而重跑源码未变化的已通过检查。

## 研究候选与原始证据

[研究入口](projects/google/adk-kotlin/research/README.md) 已区分 seed 丢失、文本插值和 TRACE 日志等候选的确认程度。中间状态传播已有相关 PR；历史混合 JUnit 类初始化失败，不能用作缺陷证明。另一份三测试 seed 草稿未执行。不要把有意在旧基线上失败的复现当作修复后验证。

公开补丁和复现的来源/哈希见 [artifacts.json](projects/google/adk-kotlin/artifacts.json)。完整日志、XML、线程快照、SDK、CLA资料保存在本机 `.cache/` 入口。目录联接及本地配置文件均被 Git 忽略；公开文件使用相对路径与清理过的摘要。

## 验证环境提示

Linux 曾缺少 build-tools36.0.0，SDK自动下载也曾超时；补齐官方组件后 #838 两次完整构建通过。当前环境已有 Android platform37.0、build-tools36.0.0/37.0.0、JDK21和共享 Gradle 缓存。

Gradle 默认堆设置很大，本地验证采用3GB堆、1GB metaspace、2 workers并关闭项目并行；没有给成功的 Linux 检查加测试排除或禁用标志。Windows旧MCP全量超时根因未确认，不能统一归因为 Windows；独立探针和 Linux 本地 MCP 测试均通过。实际环境路径和命令见仅本地的交接记录。

## SGLang

2026-10-31 新增 [SGLang 项目档案](projects/sgl-project/sglang/README.md)。以下状态核验于 **2026-10-31 23:46（Asia/Shanghai）**：

| PR | 状态 | 分支 / 修复提交 | 详细记录 |
| --- | --- | --- | --- |
| [#47649](https://github.com/sgl-project/sglang/pull/47649) | Open，等待审核；尚无人工评论 | `fix/setwise-token-logprobs` / `6d97319` | [setwise token_logprobs](projects/sgl-project/sglang/prs/47649-setwise-token-logprobs/README.md) |

- 官方 CI 未运行：`pr-gate` 因缺少 `run-ci` 标签失败，需维护者添加；这不是代码失败。
- 本地验证：Ubuntu 24.04 / WSL2、Python 3.12、仅 CPU，两个评分测试文件 62 通过，下游相关测试 75 通过，改动文件的格式与静态检查通过。完整 pre-commit、完整 CPU 套件、真实模型端到端和 GPU CI 未运行。
- SGLang 源码的 `origin` 是上游、`fork` 是 GCSimba 的 fork。测试必须在 Linux 中运行；Windows 本机缺少 `resource` 模块且 Python 版本不足。
- 不要在 WSL 中对 Windows checkout 直接运行 `pre-commit run`：换行符差异会让 Git 把全仓库视为已修改。按 PR 记录，直接对改动文件运行同版本工具。
- 下一步：刷新 #47649 的审核与 CI；需要维护者添加 `run-ci`。收到审核意见后在源码分支修改并推送到同一分支。
