# #65697：Anthropic count_tokens 计数超过 max_model_len 的提示词

## 状态与来源

- [上游 PR #65697](https://github.com/vllm-project/vllm/pull/65697)：`[Bugfix][Frontend] Count prompts over max_model_len in Anthropic count_tokens`，2026-11-05 00:43（Asia/Shanghai）创建，Open、非草稿，标签 `bug`、`frontend`。
- 状态核验：2026-11-06 01:35（Asia/Shanghai）。第 1 轮审阅意见（DarkLight1337）已按建议修改、推送并回复，等待复审。
- 自动请求的审阅者（CODEOWNERS）：mgoin、njhill、chaunceyjiang、DarkLight1337。
- 检查：DCO 在 `f0e34b5` 上通过。`pre-run-check` 仍因新贡献者门禁失败：需要维护者加 `ready` 或 `verified` 标签，或作者已有 4 个合并 PR（目前 1 个）；门禁日志要求 AI agent 不要请求加标签。pre-commit、Buildkite 在加标签前不会运行。
- 基线：`7663a8c524c056493e4924fdd851025de84ac91e`，未变基。
- 提交：[981c6d6](https://github.com/GCSimba/vllm/commit/981c6d6dab32ee01857afc02c5f7aced18d53b4c)（首次提交）、[f0e34b5](https://github.com/GCSimba/vllm/commit/f0e34b5cee7e17971878b5a9f9d2f37e0237c9bd)（第 1 轮审阅后，追加提交，未强推）。两个提交都带 `Co-authored-by: Copilot` 和 `Signed-off-by: GCSimba`，DCO 签名均经用户确认后添加。
- 分支：`fix/anthropic-count-tokens-over-limit`。
- 选题：2026-11-04 第三轮筛选中，在最新 main 上用 CPU 探针发现，见 [vLLM 研究记录](../../research/README.md)。
- 重复搜索：标题含 `count_tokens` 的 issue/PR 共 6 个（#63573/#61063 工具解析器、#59180 thinking 与 output_config、#52896/#47753 Rust 前端、#44725 负载计数、#35588 最初实现），以及 "count_tokens max_model_len"、"count_tokens maximum context length"、"count tokens context length"，都不涉及本问题。Robert Shaw 的草稿 #59616/#59617 处理的是 `/v1/messages` 的上下文溢出错误与 stop reason，不涉及计数。
- [修复补丁](change.patch)（基线到 `f0e34b5` 的完整 diff）· [验证摘要](validation.json)。

## 问题与复现

`/v1/messages/count_tokens` 把请求转成 `ChatCompletionRequest` 后走 `render_chat_request`，分词参数来自 `ChatCompletionRequest.build_tok_params`（`max_total_tokens=max_model_len`）。提示词超过 `max_model_len` 时，分词先在 `max_model_len + 1` 处截断，再由 `_token_len_check` 抛出 400（"at least 2049 input tokens"）。同样的对话，`/tokenize` 用 `max_total_tokens=None` 渲染，能返回完整计数。

Anthropic 把计数定位为“发送前检查提示词是否塞得进上下文窗口、据此裁剪”的手段（[token counting](https://platform.claude.com/docs/en/build-with-claude/token-counting)），恰恰在需要裁剪时拿不到数字。

## 修复与回归覆盖

当前做法（`f0e34b5`）：

- `_build_base_request` 本来就为计数请求单独构造请求（不带采样字段），现在构造的是 `_CountTokensChatRequest`：`ChatCompletionRequest` 的子类，`build_tok_params` 与 `TokenizeChatRequest.build_tok_params` 相同（`max_total_tokens=None`、`max_output_tokens=0`、沿用请求的 `add_special_tokens`），因此只对计数关闭长度校验和提前截断。
- `count_tokens` 仍走 `render_chat_request`，模板、工具、thinking 以及工具/推理解析器的处理与 `/v1/messages` 完全相同。解析器路径用 `isinstance(request, ChatCompletionRequest | ResponsesRequest)` 判断，子类可以通过；这也是没有另起一个独立请求模型的原因。
- 公共函数签名都不改。主代码只改 `vllm/entrypoints/anthropic/serving.py`（+15/-2），测试 +27。
- Harmony（gpt-oss）的聊天渲染本来不应用分词参数，计数原本就不受限，不受影响。
- 回归测试：`tests/entrypoints/anthropic/test_messages.py::test_count_tokens_over_max_model_len`，复用该文件已有的 `RemoteOpenAIServer`（Qwen3-0.6B，`--max-model-len 2048`）。用 Anthropic SDK 对约 3000 token 的对话计数，要求等于 `/tokenize` 的计数；同一对话调用 `messages.create` 仍须 400。CI 中已有的 `entrypoints/anthropic` 步骤会运行它；Rust 前端 CI 不跑这个目录，无需跳过。

首次提交（`981c6d6`）的做法：给 `render_chat_request`、`OnlineRenderer.render_chat`、`OnlineRenderer.preprocess_chat` 各加可选参数 `tok_params`，`count_tokens` 传入 `max_total_tokens=None` 的参数。第 1 轮审阅后撤回。

## 审阅记录

- 2026-11-05 14:08（Asia/Shanghai），DarkLight1337 在 `count_tokens` 的改动处[评论](https://github.com/vllm-project/vllm/pull/65697#discussion_r4477177615)：“Perhaps it would be better to define a separate function to do this just like for `/tokenize`? cc @chaunceyjiang”。理解为：像 `TokenizeChatRequest`（以及 decisions 的 `ReadPromptRequest`）那样，由请求类型自己的 `build_tok_params` 决定分词参数，而不是给公共渲染函数加参数。
- 经用户确认，11-06 01:33 推送 `f0e34b5`（普通推送），更新 PR 描述（修复说明、验证表格，删去与 #61063 签名冲突的说明），并在讨论串[回复](https://github.com/vllm-project/vllm/pull/65697#discussion_r4483802780)：说明新做法，并表示如果他想要像 `create_tokenize` 那样完全独立的渲染路径，可以再改。
- 11-05 有人在自己 fork 内的 PR（zkawfanx/vllm#67，非上游）引用了本 PR：给 Anthropic 计数传 `skip_mm_cache`，避免图片计数污染多模态缓存（`/tokenize` 本来就这样做）。与本 PR 不冲突。

## 验证

Ubuntu 24.04 / WSL2、Python 3.12、仅 CPU、`--enforce-eager`。Python 源码来自本分支，编译扩展取自 dd4e401 的 nightly CPU wheel（`0.33.0rc1.dev711`，比基线早 31 个提交，期间没有 CPU kernel 或自定义算子改动）。以下为第 1 轮审阅后（`f0e34b5`）的结果：

| 检查 | 结果 |
| --- | --- |
| 新测试，`anthropic/serving.py` 换回基线（反证） | 1 failed：`anthropic.BadRequestError: Error code: 400 ... at least 2049 input tokens` |
| 新测试，修复后 | 1 passed |
| `test_messages.py` 整个文件 | 10 passed |
| `tests/entrypoints/anthropic/` 下的转换、错误映射、协议导出单元测试 | 151 passed；3 个 `TestDetectMergeInlineSystem::test_probes_non_jinja_renderer[deepseek-ai/...]` 失败，因为需要从 Hugging Face 下载 DeepSeek 配置而本机无法访问，在基线上同样失败 |
| pre-commit（改动文件，含 ruff、mypy、typos） | 全部通过，shellcheck 跳过 |

探针服务器：`Qwen/Qwen3-0.6B --max-model-len 2048 --enforce-eager --enable-auto-tool-choice --tool-call-parser hermes --reasoning-parser qwen3`，开启解析器后每个计数请求都会经过解析器的 `adjust_request`。单条用户消息为 N 个重复单词，基线与修复后对比：

| 单词数 | `/tokenize` | count_tokens 修复前 | count_tokens 修复后 |
| --- | --- | --- | --- |
| 300 | 309 | 309 | 309 |
| 1900 | 1909 | 1909 | 1909 |
| 2100 | 2109 | 400 | 2109 |
| 4000 | 4009 | 400 | 4009 |
| 2100，加一个工具 | — | 400 | 2248 |

不超长时，与同样请求体的 `/v1/messages`（`max_tokens: 1`）返回的 `usage.input_tokens` 对比，修复前后完全相同：普通 15/15、加 `system` 24/24、加一个工具 154/154、`thinking: disabled` 19/19。2100 词的对话发 `/v1/messages` 和 `/v1/chat/completions`，修复前后都返回 400。

首次提交 `981c6d6` 另外跑过 `openai/chat_completion/test_chat_error.py`、`openai/test_render_parity.py`（与 Anthropic 单元测试合计 198 passed）和 `cohere/test_api_router.py`（33 passed），用于覆盖当时改动的公共签名；第 1 轮已撤回这些改动。

未运行：GPU CI、模型评测（生成行为不变）。

## 审核与后续

- 风险：Anthropic 文档没有明说超长时 count_tokens 是否报错；PR 的依据是计数的用途与 vLLM `/tokenize` 的一致行为。未合并的 #64050 给 OpenAI Responses 新增的 `input_tokens` 计数接口选择了按生成请求校验，维护者可能就此讨论；PR 正文已如实说明。
- 若 DarkLight1337 或 chaunceyjiang 要求完全独立的渲染路径（类似 `create_tokenize` 直接调用 `preprocess_chat`），需要自行处理工具、模板校验和 Harmony 分支，改动会更大。
- 等待复审和维护者加 `ready` 标签；不要请求加标签。后续修改继续追加提交，每个提交带 `-s`，推送前先拉取机器人可能合入的 main；回复和签名先经用户确认。