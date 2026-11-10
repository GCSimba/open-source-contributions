# Google ADK Kotlin

ADK 是用于构建、运行和组合 AI agent 的开发工具包。这里记录 Kotlin 实现的贡献与排查过程。

- 上游：[google/adk-kotlin](https://github.com/google/adk-kotlin)
- Fork：[GCSimba/adk-kotlin](https://github.com/GCSimba/adk-kotlin)
- 贡献规范：[CONTRIBUTING.md](https://github.com/google/adk-kotlin/blob/main/CONTRIBUTING.md)
- 项目指引：[AGENTS.md](https://github.com/google/adk-kotlin/blob/main/AGENTS.md)
- 行为参考：[google/adk-python](https://github.com/google/adk-python)
- 代码材料许可：[Apache 2.0](LICENSE)

## PR 记录

| PR | 主题 | 档案 |
| --- | --- | --- |
| [#774](https://github.com/google/adk-kotlin/pull/774) | 防止保留的 artifact 路径覆盖内部数据 | [问题、审核与验证](prs/774-metadata-path/README.md) |
| [#837](https://github.com/google/adk-kotlin/pull/837) | 加载同批并行工具响应中的全部 artifact | [问题、回归与验证](prs/837-parallel-artifacts/README.md) |
| [#838](https://github.com/google/adk-kotlin/pull/838) | AgentTool 继承调用配置（已关闭：Google 内部的 #825 包含相同修复） | [问题、回归与验证](prs/838-agent-tool-config/README.md) |
| [#856](https://github.com/google/adk-kotlin/pull/856) | Firebase 后端发送 response schema 与 penalty | [问题、回归与验证](prs/856-firebase-response-schema/README.md) |

各 PR 记录自己的提交和验证结果。#837 与 #838 从相同基线创建独立分支，各自在 Linux 完成了全量构建；一个分支的结果不用于证明另一个分支。

Google 内部变更由 copybara-service 导出为 PR，GitHub 上的版本可能落后于内部版本。选题和查重时要检查这些 PR 的最新版本；#838 就是因此与 #825 重复。

## 贡献要点

- PR 只能有一个提交，标题用 Conventional Commit（如 `fix(firebase): ...`）；按审核意见修改时 amend 后 `--force-with-lease` 推送。
- Google CLA 检查覆盖提交中的所有作者，包括 `Co-authored-by` trailer。Copilot 等机器人账号没有签 CLA，加上 trailer 会让 CLA 失败（#856 首次推送即如此）。AI 辅助在 PR 描述里说明。
- 选题避开“Python 版已修、Kotlin 尚未移植”的差异，Google 团队会自己移植（#838 对应的 Python 修复早在 8 月）；优先 Firebase、ML Kit、LiteRT-LM、Room、Java 互操作等只在 Kotlin 中存在的部分。
- 外部 PR 的 validation 等工作流需要维护者批准后才会运行。
- 仓库导入 Google 内部时会对测试运行 CheckReturnValue 检查：调用有返回值的函数（如 `saveArtifact`）要接收结果，写成 `val unused = ...`，否则内部导入构建失败（#837 第 2 轮的 HIGH 意见）。
- 测试优先用 `testing` 模块现成的辅助函数（如 `modelParallelFunctionCallsResponse`），`Runner` 用 `use {}` 关闭；不要写不可能失败的断言（例如对不可变 `LlmRequest` 断言“原请求不变”）。

## 研究与复现

[研究记录](research/README.md) 汇总筛选结果、尚未提交的候选及历史验证限制；[复现目录](research/repros/README.md) 保存五份有明确执行背景的测试草稿和两项选定问题的基线结果。

各 PR 的 `change.patch` 来自该修复提交与其父提交之间的 Git 差异；SHA、来源和适用基线见记录及 [artifacts.json](artifacts.json)。最终源码和后续变化以对应上游 PR 为准。

## 本地工作位置

实际路径记录在仓库根目录忽略的 `workspace.local.json`；本地 `.workspaces/google/adk-kotlin` 与 `.cache/google/adk-kotlin` 分别提供现有源码和全部工具/原始证据的入口。公开档案包含清理后的说明、补丁、复现和结果摘要。

记录中的命令从 ADK 源码 checkout 执行。准备 JDK、Android SDK 并读取对应提交的项目指引后，再运行 Gradle；本档案仓库本身没有 Gradle 构建。
