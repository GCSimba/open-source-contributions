# Open Source Contributions

跨项目的开源 PR 工作档案：记录问题复现、修复、验证、审核和上游链接。

接手工作先读 [HANDOFF.md](HANDOFF.md)。本机另有忽略的 `HANDOFF.local.md` 和 `adk-contributions.local.code-workspace`，分别提供实际路径/命令和 VS Code 多目录入口。

## 项目

| 项目 | 上游 | 贡献记录 |
| --- | --- | --- |
| Google ADK Kotlin | [google/adk-kotlin](https://github.com/google/adk-kotlin) | [项目说明与 PR 索引](projects/google/adk-kotlin/README.md) |
| SGLang | [sgl-project/sglang](https://github.com/sgl-project/sglang) | [项目说明与 PR 索引](projects/sgl-project/sglang/README.md) |
| vLLM | [vllm-project/vllm](https://github.com/vllm-project/vllm) | [项目说明与 PR 索引](projects/vllm-project/vllm/README.md) |

## 目录约定

```text
projects/<owner>/<repository>/
├─ README.md                  项目、fork、贡献入口
├─ LICENSE                    收录代码对应的上游许可证
├─ prs/<number>-<topic>/       每个 PR 的说明、补丁和验证摘要
└─ research/                  候选问题、复现草稿及取舍依据
templates/pr.md               新贡献记录模板
```

每个项目使用自己的 Git 仓库和 fork；每个 PR 使用独立分支，需要同时工作时再创建独立工作副本。最终代码提交到对应项目的 fork，本仓库保存贡献档案和小型复现材料。

## 本地工作入口

`.workspaces/` 保存独立源码仓库的本地入口，`.cache/` 保存工具和原始证据的本地入口；两者及 `workspace.local.json` 均被 Git 忽略。现有入口连接到已有工作目录，原目录保持原位。执行构建时使用 `workspace.local.json` 中的实际目录。

新机器可参考 [workspace.example.json](workspace.example.json) 配置本地路径，并在 `.workspaces/` 中准备各项目的独立 checkout。SDK、依赖、完整日志、CLA 表单和账户配置保存在本地。

## 新增项目或 PR

1. 在 `projects/<owner>/<repository>/` 建立项目说明，登记上游、fork 和贡献规范。
2. 按 [PR 模板](templates/pr.md) 建立记录，登记复现基线、提交 SHA、验证环境及未完成的检查。
3. 在对应源码仓库修复和测试，遵守该项目的 `AGENTS.md`、`CONTRIBUTING.md` 与分支规则。
4. 发布 PR 后补齐链接；后续审核和合并结果集中更新在该 PR 的记录中。

PR 状态是标注时间的人工快照，实时状态以 GitHub 为准。验证结果只适用于记录中的提交，不跨分支继承。代码材料的来源与许可见 [NOTICE.md](NOTICE.md)。
