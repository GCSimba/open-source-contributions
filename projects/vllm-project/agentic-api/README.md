# agentic-api

vLLM 组织下的有状态 Agent API 网关，部署在 vLLM 前面，提供以下能力：

- OpenAI Responses：`previous_response_id`、conversations、后台任务；
- Anthropic Messages；
- 对 Codex 和 Claude Code 的兼容；
- MCP；
- 由网关执行的工具：web search、tool search、code interpreter、file search；
- 持久化。

主体用 Rust 编写（workspace 在 `crates/`），另有 Python 绑定和测试。vLLM 主仓库 5 月把 `previous_response_id` 等有状态功能的开发暂停，交给这个项目承接（见 vLLM issue [#43244](https://github.com/vllm-project/vllm/issues/43244)）。

- 上游：[vllm-project/agentic-api](https://github.com/vllm-project/agentic-api)
- Fork：尚未创建，首次提交 PR 前创建 `GCSimba/agentic-api`。
- 贡献规范：
  - [AGENTS.md](https://github.com/vllm-project/agentic-api/blob/main/AGENTS.md)：写给 AI 编码代理看，要求先读 `.agents/skills/` 下的 engineering skill 和 Rust skill。
  - [CONTRIBUTING.md](https://github.com/vllm-project/agentic-api/blob/main/CONTRIBUTING.md)
  - [PR 模板](https://github.com/vllm-project/agentic-api/blob/main/.github/pull_request_template.md)

## 贡献要点（2026-11-06 调研）

- **提交规范**：
  - 每个提交需要 DCO 签名（`git commit -s`），由 CI 检查。
  - 提交和 PR 标题使用 conventional 前缀（`feat:`、`fix:`、`chore:`、`docs:`、`ci:`）。
  - PR 正文需要 Summary 和 Test Plan 两部分。
- **需要通过的检查**：
  - `cargo test`
  - `cargo clippy --all-targets -- -D warnings`
  - `cargo fmt -- --check`
  - `pre-commit run --all-files`
  - Python 测试：`uv run --with maturin --with pytest python -m pytest tests/python`
- **代码约束**：
  - Rust 2024 edition，MSRV 1.98.1，禁止使用 `unsafe`，每行不超过 120 列。
  - `rust-file-sizes` hook 要求 Rust 文件的生产代码不超过 500 行。`.rust-file-sizes.json` 记录现有的超限基线，以及附带理由的例外。
- **认领惯例**：没有成文规则，但大家都会先在 issue 下留言，说明想做什么、打算怎么做，然后再动手。
- **审阅与合并**：
  - 近 30 天合并的 54 个 PR，全部由 maralbahari（23 个）、haoshan98（15 个）、franciscojavierarceo（15 个）和 tjtanaa（1 个）合并。
  - 61% 来自外部贡献者。
  - 外部 PR 首次审阅的中位数约 12.7 小时，合并的中位数约 3.7 天。
- **规模**：29 位贡献者，约 54 个开放 issue、32 个开放 PR。约三分之二的开放 issue 已有人认领、已指派、带 RFC 标签或有关联 PR。

## 进展

| issue | 主题 | 状态 |
| --- | --- | --- |
| [#312](https://github.com/vllm-project/agentic-api/issues/312) | 拆分超过 500 行的 Rust 文件（good first issue） | 2026-11-06 [留言认领](https://github.com/vllm-project/agentic-api/issues/312#issuecomment-6687570907) `crates/agentic-server/src/agentic_process.rs`（基线 557 行，没有例外）。计划把 harness 命令和环境构建函数移到子模块 `agentic_process/harness.rs`，不改变行为，并删除这个文件的基线条目 |
| [#482](https://github.com/vllm-project/agentic-api/issues/482) | 用 `previous_response_id` 续接时，网关执行的工具调用被排到它前面的推理之前 | 由维护者 haoshan98 报告，issue 里已写明修复方向。Than0316 在 11-01 问过能否贡献，没有得到回复。2026-11-06 [留言询问](https://github.com/vllm-project/agentic-api/issues/482#issuecomment-6687571904)：如果没人在做，就按 issue 中的方向修复，并补一个"推理位于 hosted tool search 之前"的续接测试 |

## 其他候选

以下候选尚未认领，按价值排序：

| issue | 主题 | 备注 |
| --- | --- | --- |
| [#461](https://github.com/vllm-project/agentic-api/issues/461) | executor 路径不向模型服务转发可配置的请求头（`store: false` 的透传路径会转发全部请求头） | ENCHIGO 10-20 表示想做，并提了一个设计问题，至今没有回复；PR #488 与之部分相关 |
| [#395](https://github.com/vllm-project/agentic-api/issues/395) | `list_agents` 返回 `last_task_message` | ENCHIGO 10-20 表示想做，但还没开 PR |
| [#434](https://github.com/vllm-project/agentic-api/issues/434) | Responses 回显上游实际使用的参数 | 有贡献者提出两个接口约定问题，没有回复，需要先定下设计 |

另有两个 issue 已在 main 上修好，只是还没关闭：

- #409：`server_tool_use` 块，实现见 `executor/messages_server_tools.rs`；
- #459：`traceparent`，其他人无法复现。

## 本地工作位置

实际路径记录在仓库根目录被忽略的 `workspace.local.json` 中。源码克隆在 WSL 的 Linux 文件系统里，用 rustup 安装的 1.98.1 工具链（含 clippy 和 rustfmt）构建。2026-11-06 在 `07ec48d` 上首次 debug 构建，`cargo build` 用时约 11 分钟。
