# vLLM

vLLM 是面向大语言模型的高吞吐推理与服务引擎，提供 OpenAI 兼容服务。这里记录其贡献与排查过程。

- 上游：[vllm-project/vllm](https://github.com/vllm-project/vllm)
- Fork：[GCSimba/vllm](https://github.com/GCSimba/vllm)
- 贡献规范：[贡献指南](https://docs.vllm.ai/en/latest/contributing/)（源码 `docs/contributing/README.md`）、[AI 辅助贡献规则](https://github.com/vllm-project/vllm/blob/main/AGENTS.md)、[测试规范](https://github.com/vllm-project/vllm/blob/main/tests/AGENTS.md)、[PR 模板](https://github.com/vllm-project/vllm/blob/main/.github/PULL_REQUEST_TEMPLATE.md)、[pr-checklist skill](https://github.com/vllm-project/vllm/blob/main/.agents/skills/pr-checklist/SKILL.md)
- 代码材料许可：[Apache 2.0](LICENSE)

## 贡献要点

- PR 标题使用可叠加的方括号标签，例如 `[Bugfix][Frontend] ...`。
- 每个提交都需要 DCO 签名（`git commit -s` 生成 `Signed-off-by:`），由 DCO 检查核对。签名是提交者本人的声明，由本人确认后再添加。
- AI 辅助贡献：发布前查重；不提交错字、风格等琐碎修改；提交者必须理解并能解释每一行改动；在 PR 正文和提交 trailer 中声明 AI 辅助；发布或请求复审前运行 `/pr-checklist` skill。PR 机器人会要求 AI agent 依据 AGENTS.md 复核 PR 的价值。
- 回归测试遵守 `tests/AGENTS.md`：优先通过 `RemoteOpenAIServer`、`LLM` 等公开接口断言可观察结果；PR 中展示修复前失败、修复后通过的输出；确认 Buildkite 会选中新测试。
- 新贡献者的 PR 有 `pre-run-check` 门禁：维护者添加 `verified`、`ready` 或 `ready-run-all-tests` 标签，或作者已有 4 个合并 PR 后，pre-commit 与文档预览才会运行。门禁日志要求 AI agent 不要请求添加标签。
- 完整 CI（Buildkite）默认不运行，由有写权限的审阅者评论 `/ci run`；PR 获批或带 `ready` 标签后，作者可用 `/ci run`、`/ci retry`、`/ci cancel`。触发前分支必须与目标分支相差 0 个提交，fork PR 需开启 “Allow edits by maintainers”。
- 作者在已经跑过 CI 的提交上不能再 `/ci run`，`/ci retry` 只重试同一提交上失败的 job。维护者开启自动合并后，作者不要往分支推送：没有写权限的人推送会让 GitHub 关闭自动合并。CI 因 main 上已修复的问题失败时，请维护者更新分支后重跑（#64935 的做法）。
- 无写权限的贡献者最多同时有 6 个开放 PR。

## PR 记录

| PR | 主题 | 状态 | 档案 |
| --- | --- | --- | --- |
| [#64935](https://github.com/vllm-project/vllm/pull/64935) | 批量 chat completions 报告 reasoning tokens | 2026-11-04 已合并（[e4991c1](https://github.com/vllm-project/vllm/commit/e4991c1feb8ce6a11c3fa7e9d63974ee3fe25968)） | [问题、修复与验证](prs/64935-batch-reasoning-tokens/README.md) |

## 研究与复现

[研究记录](research/README.md) 汇总候选筛选和已排除的重复选题；[复现目录](research/repros/README.md) 保存对比单对话与批量接口 usage 的探针脚本。vLLM 与 SGLang 的 PR 节奏对比见 [SGLang 研究目录中的分析](../../sgl-project/sglang/research/pr-latency-analysis.md)。

各 PR 的 `change.patch` 是 PR 基线到最新提交的累计 Git 差异（只有一个提交时即修复提交与其父提交之间的差异）；SHA、来源见记录及 [artifacts.json](artifacts.json)。最终源码和后续变化以对应上游 PR 为准。

## 本地工作位置

实际路径记录在仓库根目录忽略的 `workspace.local.json`；本地 `.workspaces/vllm-project/vllm` 与 `.cache/vllm-project/vllm` 分别提供源码和验证工具的入口。

本机没有 GPU。验证在 WSL 的 Linux 文件系统镜像中进行：Python 源码从 Windows checkout 同步，编译扩展取自 vLLM CPU nightly wheel。记录中的 pytest 命令从 vLLM 源码根目录执行；本档案仓库本身不包含构建配置。
