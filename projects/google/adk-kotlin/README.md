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
| [#838](https://github.com/google/adk-kotlin/pull/838) | AgentTool 继承调用配置 | [问题、回归与验证](prs/838-agent-tool-config/README.md) |

各 PR 记录自己的提交和验证结果。#837 与 #838 从相同基线创建独立分支，#838 的全量构建结果不用于证明 #837。

## 研究与复现

[研究记录](research/README.md) 汇总筛选结果、尚未提交的候选及历史验证限制；[复现目录](research/repros/README.md) 保存五份有明确执行背景的测试草稿和两项选定问题的基线结果。

各 PR 的 `change.patch` 来自该修复提交与其父提交之间的 Git 差异；SHA、来源和适用基线见记录及 [artifacts.json](artifacts.json)。最终源码和后续变化以对应上游 PR 为准。

## 本地工作位置

实际路径记录在仓库根目录忽略的 `workspace.local.json`；本地 `.workspaces/google/adk-kotlin` 与 `.cache/google/adk-kotlin` 分别提供现有源码和全部工具/原始证据的入口。公开档案包含清理后的说明、补丁、复现和结果摘要。

记录中的命令从 ADK 源码 checkout 执行。准备 JDK、Android SDK 并读取对应提交的项目指引后，再运行 Gradle；本档案仓库本身没有 Gradle 构建。
