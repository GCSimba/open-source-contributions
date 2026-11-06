# #511：把 agentic_process 中启动 harness 的函数移到独立模块

## 状态与来源

- [上游 PR #511](https://github.com/vllm-project/agentic-api/pull/511)：`chore: move agentic_process harness launch helpers into their own module`。2026-11-07 00:12（Asia/Shanghai）创建，Open。
- 对应 issue：[#312](https://github.com/vllm-project/agentic-api/issues/312)（good first issue：拆分超过 500 行的 Rust 文件）。11-06 已在 issue 中[留言认领](https://github.com/vllm-project/agentic-api/issues/312#issuecomment-6687570907)这个文件。
- 基线：`d89621f70d3480ad0428a1521eaa97de2761e1ac`（11-06 的 main）。
- 提交：[0535c6a](https://github.com/GCSimba/agentic-api/commit/0535c6a42a4c80cd731fdf56dfda150a2563cbed)，带 `Co-authored-by: Copilot` 和 `Signed-off-by: GCSimba` 两个 trailer。DCO 签名经用户确认后添加。
- 分支：`chore/split-agentic-process-harness`，位于 fork [GCSimba/agentic-api](https://github.com/GCSimba/agentic-api)。
- 检查：
  - DCO 已通过。
  - Python、Rust、Pre-commit、Release Python、Harness CLI E2E、Container 六个工作流处于 `action_required`。这是首次向该仓库贡献，需要维护者批准后才运行。
  - CODEOWNERS 自动请求了 9 位审阅者。
- [补丁](change.patch) · [验证摘要](validation.json)

## 问题与改动

`crates/agentic-server/src/agentic_process.rs` 的生产代码有 557 行，超过仓库规定的 500 行上限，在 `.rust-file-sizes.json` 中以基线形式登记（没有例外）。本 PR 把"启动 coding harness"的代码移到 `agentic_process/harness.rs`：

- 查找二进制（`harness_binary_names`、`harness_binary`）、生成启动参数（`harness_launch_args`、`CLAUDE_TOOLS`）、检查 Claude 透传参数（`validate_claude_passthrough`）；
- 为启动会话和附加会话准备环境（`harness_environment`、`prepare_attached_harness_environment`）；
- 创建子进程（`spawn_harness`、`build_harness_command`）。

函数逻辑不变。新文件中把内联的 `crate::…` 路径改成了文件顶部的 import，这是仓库 engineering skill 的要求。

以下内容留在 `agentic_process.rs`：`HarnessModel`、模型解析，以及公开的 `claude_effort` 和 `DEFAULT_CLAUDE_EFFORT`。父模块沿用现有 `catalog` 的做法，用 `#[cfg(test)] use` 把测试需要的函数引回来，所以 `tests.rs` 不用改；`catalog.rs` 只改了一行 import。

按仓库的 tree-sitter 检查器计数，拆分后 `agentic_process.rs` 为 390 行，`harness.rs` 为 178 行，并删除了原文件的基线条目。`ARCHITECTURE.md` 只描述到模块一级，不需要修改。

## 验证

环境：Ubuntu 24.04 / WSL2，Rust 1.98.1。

| 检查 | 结果 |
| --- | --- |
| `cargo fmt --all -- --check` | 通过 |
| `cargo clippy --workspace --all-targets -- -D warnings` | 通过，零警告 |
| `cargo test -p agentic-server --lib agentic_process` | 42 个通过 |
| `cargo test -p agentic-server --no-fail-fast` | 49 个测试二进制，801 个通过、0 失败、0 忽略 |
| `scripts/check_rust_file_sizes.py`（tree-sitter 0.25.2，tree-sitter-rust 0.24.2） | 通过，检查了 356 个生产文件 |
| `pre-commit run --all-files` | 全部通过（仓库里没有 symlink，该项跳过） |
| `scripts/tests/agentic-cli-e2e-test.py` 中的 `agentic run` 部分（mock vLLM 加假的 Codex 和 Claude harness，沿用原断言） | 通过；跳过了脚本里从源码 pip 安装的步骤 |

未运行的部分：
- `agentic-server-core` 的测试（这个 crate 没有改动）；
- CLI E2E 中从源码安装的步骤（需要一次 release 构建）；
- PostgreSQL 相关任务。

测试构建时设置了 `CARGO_PROFILE_DEV_DEBUG=0`、`CARGO_INCREMENTAL=0`、`CARGO_BUILD_JOBS=3`，这些只影响构建产物，不影响测试行为。原因是第一次按默认设置（带完整调试信息）构建时，每个测试二进制约 430 MB，把 C 盘写满，导致 WSL 虚拟机两次崩溃：rustc 报 SIGBUS，系统命令报 I/O 错误。

## 审核与后续

- 等维护者批准工作流并审阅。
- 如需修改，在同一分支追加提交，每个提交带 `-s`；推送和回复前先经用户确认。
