# #64935：批量 chat completions 报告 reasoning tokens

## 状态与来源

- [上游 PR #64935](https://github.com/vllm-project/vllm/pull/64935)：`[Bugfix][Frontend] Report reasoning tokens in batched chat completions`，Open、非草稿；自动标签 `bug`、`documentation`、`frontend`、`ci/build`、`rust`。
- 状态核验：2026-11-01 08:41（Asia/Shanghai）。已自动请求 Harry-Chen、khluu、chaunceyjiang、DarkLight1337 审核；暂无人工评论或审核意见。`claude[bot]` 提示 fork PR 不自动审查，需要维护者评论 `@claude review`。
- 检查：DCO、Mergify Summary 通过。`pre-run-check` 失败，原因是新贡献者需要维护者添加 `verified`、`ready` 或 `ready-run-all-tests` 标签（或作者已有 4 个合并 PR），不是代码问题；pre-commit、Check format 因此跳过，Read the Docs 构建也被同一门禁拦下。门禁日志要求 AI agent 不要请求添加标签。Buildkite 完整 CI 尚未运行。
- 基线：`564ac0de1b228568cafcbfbe8ccaccc82d6023eb`。发布前从 `cfa9f96f5efbb45c5c02ab127b51eaf8b86bb4ca` 变基，两者之间的上游提交只修改 `recipes/models/*.yaml`；变基前提交 `4784d89d`，patch-id 相同。
- 修复提交：[a36cacaf1209e9d69034bae92e52156b7903b3f6](https://github.com/GCSimba/vllm/commit/a36cacaf1209e9d69034bae92e52156b7903b3f6)，含 `Co-authored-by: Copilot` 和 `Signed-off-by: GCSimba`。DCO 签名在用户逐行审阅改动并确认后添加。
- 分支：`fix/batch-chat-reasoning-tokens`。
- 先例：[#64757](https://github.com/vllm-project/vllm/pull/64757) 于 10 月 31 日合并，为同一端点报告 `prompt_tokens_details`，未处理 completion 侧。
- 重复搜索：发布前搜索开放与已关闭的 PR、issue：`batch reasoning tokens`、`completions/batch usage`、`batched chat completions usage`、`reasoning_tokens`、`completion_tokens_details`、`OpenAIServingChatBatch`、`batch_serving`，没有同题 PR。相关 PR 及关系见[研究记录](../../research/README.md)。
- [修复补丁](change.patch) · [验证摘要](validation.json) · [usage 探针](../../research/repros/README.md)。

## 问题与复现

服务以 `--reasoning-parser` 启动时，`/v1/chat/completions` 在 `usage.completion_tokens_details.reasoning_tokens` 中报告推理 token；同一对话经 `/v1/chat/completions/batch` 发送时，`completion_tokens_details` 始终为 `null`。`include_reasoning=false` 时推理内容不返回，但仍然生成并计入 `completion_tokens`，批量接口的调用方无法得知其中有多少是推理 token。

根因：`OpenAIServingChatBatch.chat_completion_full_generator_batch` 构建 `UsageInfo` 时没有传 `completion_tokens_details`，也没有统计推理 token，尽管每个对话的输出已经交给 parser 解析。

修复后的 usage 探针（`Qwen/Qwen3-0.6B --reasoning-parser qwen3`，`max_tokens=64`、`temperature=0`、`include_reasoning=false`）：

| 接口 | completion_tokens | reasoning_tokens |
| --- | --- | --- |
| `/v1/chat/completions` | 64 | 63 |
| `/v1/chat/completions/batch`，同一对话两次 | 128 | 126 |

修复前批量接口的 `completion_tokens_details` 为 `null`，由回归测试的失败输出证明（见“验证”）；单对话接口不在本补丁修改范围内。

## 修复与回归覆盖

- `batch_serving.py`：每个输出经 parser 解析后，用 `parser.count_reasoning_tokens(output.token_ids)` 累加推理 token；服务配置了 reasoning parser 时（`_include_reasoning_tokens_details`），`UsageInfo` 填入 `_make_completion_tokens_details(total_reasoning_tokens)`，否则保持 `null`。做法与单对话路径 `OpenAIServingChat.chat_completion_full_generator` 一致，复用现有 helper，主代码新增 10 行。
- 回归测试 `test_include_reasoning.py::test_batch_reasoning_tokens_in_usage`：复用该模块已有的 Qwen3-0.6B + qwen3 parser 服务，通过 HTTP 发送两个对话（`include_reasoning=false`、`temperature=0`），断言 `completion_tokens_details` 非空且 `0 < reasoning_tokens <= completion_tokens`。
- `test_batched_chat_completions.py`：3 个用 `__new__` 构造的 serving 补上 `_include_reasoning_tokens_details = False`（与 #64757 处理 `enable_prompt_tokens_details` 的方式相同），假 parser 补上 `count_reasoning_tokens`。
- CI：Rust 前端对 `/v1/chat/completions/batch` 返回 not_supported，`rust_frontend.yaml` 和 `test-amd.yaml` 中 MI300 的 Rust 前端步骤用 `-k "not test_batch_reasoning_tokens_in_usage"` 排除新测试；Python 前端步骤照常运行该文件。
- 文档：`docs/serving/online_serving/openai_compatible_server.md` 在该端点 `prompt_tokens_details` 的说明之后增加一句。

未配置 reasoning parser 时 `completion_tokens_details` 仍为 `null`，与单对话接口一致。

## 验证

Ubuntu 24.04 / WSL2、Python 3.12、仅 CPU（Intel i5-11300H，AVX-512）、`--enforce-eager`、torch 2.14.0+cpu。Python 源码来自本分支，WSL 镜像中三个相关文件的 blob 与修复提交一致；编译扩展取自 vLLM CPU nightly wheel `0.33.0rc1.dev421+g2b009fc6d`（比验证基线早 20 个提交，差异仅在 CUDA kernel）。

| 检查 | 结果 | 时间（11-01） |
| --- | --- | --- |
| 修复前（`batch_serving.py` 为基线 blob `43c0727`），最终回归测试 | 1 failed、6 deselected：`test_include_reasoning.py:118`，`assert None is not None` | 08:28 |
| 修复后，最终回归测试 | 1 passed、6 deselected | 07:30 |
| 修复后，`test_include_reasoning.py` 整个文件 | 7 passed | 08:38 |
| 修复后，`test_batched_chat_completions.py` 中 6 个不启动服务的测试 | 6 passed、6 deselected | 07:01 |
| 修复后，usage 探针 | 单对话 64/63，批量两次 128/126 | 07:32 |
| `pre-commit run --files` 6 个改动文件 | 18 个 hook 通过，17 个无匹配文件而跳过；shellcheck 以 `SKIP` 跳过 | 08:00 |
| 官方 CI | 未运行：`pre-run-check` 等待维护者标签，Buildkite 需要 `/ci run` | 08:41 |

回归测试在 07:26 加入 `"temperature": 0`。在此之前的草稿曾在修复前失败于第 117 行（06:58），修复后整文件 7 passed（07:04）。表中修复前和整文件的结果是最终版本的重跑，PR 描述中的行号已据此更正为 118。

命令从 vLLM 源码根目录、在 vLLM CPU 环境中执行：

```sh
pytest tests/entrypoints/openai/chat_completion/test_include_reasoning.py -k test_batch_reasoning_tokens_in_usage
pytest tests/entrypoints/openai/chat_completion/test_include_reasoning.py
pytest tests/entrypoints/openai/chat_completion/test_batched_chat_completions.py \
  -k "echo or harmony or kv_transfer or parser_receives or reasoning_wiring"
SKIP=shellcheck pre-commit run --files <6 个改动文件>
```

第三条为等价的选择表达式，选中 `test_batched_echo_does_not_prepend_user_prompt`、`test_batched_echo_prepends_matching_assistant_prefix`、`test_batched_harmony_response_format_uses_structural_tag`、`test_batch_rejects_kv_transfer_prompt_token_ids`、`test_batched_parser_receives_each_prompt_prefix`、`test_batched_reasoning_wiring_matches_single_chat`。

未运行：`test_batched_chat_completions.py` 中需要 `Qwen/Qwen2.5-1.5B-Instruct` 的 6 个服务测试（该服务未配置 reasoning parser，本补丁只让响应多一个 `null` 字段）；GPU 与 ROCm CI；Rust 前端（CI 中已排除新测试）。模型输出不变，只改变响应 usage，因此没有做模型评测。

## 验证过程中的环境问题

- Windows 无法直接运行 vLLM。WSL 中没有 GPU，也不做 C++ 构建：把本分支的 `vllm/`、`tests/` 同步到 Linux 文件系统镜像，再接入 nightly CPU wheel 的编译扩展。
- triton CPU wheel 在 WSL 中下载超时，改为在 Windows 下载后本地安装；libnuma 没有用 apt 安装，解压到用户目录后通过 `LD_LIBRARY_PATH` 引用。
- vLLM 服务运行时 Windows 提交内存紧张，git 曾报 `Out of memory`；等服务退出后再执行 git。
- 本机 pre-commit 下载 shellcheck 超时，以 `SKIP=shellcheck` 跳过；本 PR 没有 shell 文件。
- 探针日志关闭阶段的 `AsyncLLM output_handler failed` / `EngineDeadError` 出现在两个请求返回 200 之后，是 SIGTERM 强制停止引擎导致的。

## 审核与后续

- 发布后按 PR 机器人的要求，依据 AGENTS.md 和 `/pr-checklist` 复核：改动不是琐碎修改，不重复，有修复前后证据，保留 PR。
- 等待维护者添加 `verified` 或 `ready` 标签并评论 `/ci run`；AI agent 不请求标签。
- 收到审核意见后在 `fix/batch-chat-reasoning-tokens` 修改，运行受影响测试，每个提交用 `-s` 签名后推送到 `fork`；改写历史用 `--force-with-lease`。触发 CI 前分支需与 main 相差 0 个提交。
- 若 [#58010](https://github.com/vllm-project/vllm/pull/58010) 先合并，本分支可能需要变基并重跑回归测试。
