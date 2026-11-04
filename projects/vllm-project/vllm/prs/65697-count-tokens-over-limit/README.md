# #65697：Anthropic count_tokens 计数超过 max_model_len 的提示词

## 状态与来源

- [上游 PR #65697](https://github.com/vllm-project/vllm/pull/65697)：`[Bugfix][Frontend] Count prompts over max_model_len in Anthropic count_tokens`，2026-11-05 00:43（Asia/Shanghai）创建，Open、非草稿，标签 `bug`、`frontend`。
- 自动请求的审阅者（CODEOWNERS）：mgoin、njhill、chaunceyjiang、DarkLight1337。
- 检查：DCO 通过。`pre-run-check` 因新贡献者门禁失败：需要维护者加 `ready` 或 `verified` 标签，或作者已有 4 个合并 PR（目前 1 个）；门禁日志要求 AI agent 不要请求加标签。pre-commit 等其余检查在加标签前不会运行。
- 基线：`7663a8c524c056493e4924fdd851025de84ac91e`。推送前 main 又前进了 6 个提交，都没有改动本 PR 的文件，未变基。
- 修复提交：[981c6d6dab32ee01857afc02c5f7aced18d53b4c](https://github.com/GCSimba/vllm/commit/981c6d6dab32ee01857afc02c5f7aced18d53b4c)，带 `Co-authored-by: Copilot` 和 `Signed-off-by: GCSimba`；DCO 签名经用户确认后添加。
- 分支：`fix/anthropic-count-tokens-over-limit`。
- 选题：2026-11-04 第三轮筛选中，在最新 main 上用 CPU 探针发现，见 [vLLM 研究记录](../../research/README.md)。
- 重复搜索：标题含 `count_tokens` 的 issue/PR 共 6 个（#63573/#61063 工具解析器、#59180 thinking 与 output_config、#52896/#47753 Rust 前端、#44725 负载计数、#35588 最初实现），以及 "count_tokens max_model_len"、"count_tokens maximum context length"、"count tokens context length"，都不涉及本问题。Robert Shaw 的草稿 #59616/#59617 处理的是 `/v1/messages` 的上下文溢出错误与 stop reason，不涉及计数。
- [修复补丁](change.patch) · [验证摘要](validation.json)。

## 问题与复现

`/v1/messages/count_tokens` 把请求转成 `ChatCompletionRequest` 后走 `render_chat_request`，分词参数来自 `ChatCompletionRequest.build_tok_params`（`max_total_tokens=max_model_len`）。提示词超过 `max_model_len` 时，分词先在 `max_model_len + 1` 处截断，再由 `_token_len_check` 抛出 400（"at least 2049 input tokens"）。同样的对话，`/tokenize` 用 `max_total_tokens=None` 渲染，能返回完整计数。

Anthropic 把计数定位为“发送前检查提示词是否塞得进上下文窗口、据此裁剪”的手段（[token counting](https://platform.claude.com/docs/en/build-with-claude/token-counting)），恰恰在需要裁剪时拿不到数字。

## 修复与回归覆盖

- `render_chat_request`、`OnlineRenderer.render_chat`、`OnlineRenderer.preprocess_chat` 各加一个可选参数 `tok_params`，默认 `None` 时仍用 `request.build_tok_params(...)`，其他调用方行为不变。
- `count_tokens` 传入聊天请求自身的分词参数，只把 `max_total_tokens` 设为 `None`，同时去掉长度校验和提前截断，与 `/tokenize` 一致。生成请求仍校验。
- Harmony（gpt-oss）的聊天渲染本来不应用分词参数，计数原本就不受限，不受影响。
- 主代码 +18/-3，测试 +27。
- 回归测试：`tests/entrypoints/anthropic/test_messages.py::test_count_tokens_over_max_model_len`，复用该文件已有的 `RemoteOpenAIServer`（Qwen3-0.6B，`--max-model-len 2048`）。用 Anthropic SDK 对约 3000 token 的对话计数，要求等于 `/tokenize` 的计数；同一对话调用 `messages.create` 仍须 400。CI 中已有的 `entrypoints/anthropic` 步骤会运行它；Rust 前端 CI 不跑这个目录，无需跳过。

## 验证

Ubuntu 24.04 / WSL2、Python 3.12、仅 CPU、`--enforce-eager`。Python 源码来自本分支，编译扩展取自 dd4e401 的 nightly CPU wheel（`0.33.0rc1.dev711`，比基线早 31 个提交，期间没有 CPU kernel 或自定义算子改动）。

| 检查 | 结果 |
| --- | --- |
| 新测试，三个源文件换回基线（反证） | 1 failed：`anthropic.BadRequestError: Error code: 400 ... at least 2049 input tokens` |
| 新测试，修复后 | 1 passed |
| `test_messages.py` 整个文件 | 10 passed |
| Anthropic 转换、错误映射、`test_chat_error.py`、`test_render_parity.py` | 198 passed；3 个 `TestDetectMergeInlineSystem::test_probes_non_jinja_renderer[deepseek-ai/...]` 失败，因为需要从 Hugging Face 下载 DeepSeek 配置而本机无法访问，在基线上同样失败 |
| `tests/entrypoints/cohere/test_api_router.py`（另一个 `render_chat_request` 调用方） | 33 passed |
| pre-commit（改动文件） | 全部通过，shellcheck 跳过 |

同一服务器设置，单条用户消息为 N 个重复单词，基线与修复后对比：

| 单词数 | `/tokenize` | count_tokens 修复前 | count_tokens 修复后 |
| --- | --- | --- | --- |
| 300 | 309 | 309 | 309 |
| 1900 | 1909 | 1909 | 1909 |
| 2100 | 2109 | 400 | 2109 |
| 4000 | 4009 | 400 | 4009 |

2100 词的对话发 `/v1/messages` 和 `/v1/chat/completions`，修复前后都返回 400。

未运行：GPU CI、模型评测（生成行为不变）。

## 审核与后续

- 风险一：Anthropic 文档没有明说超长时 count_tokens 是否报错；PR 的依据是计数的用途与 vLLM `/tokenize` 的一致行为。未合并的 #64050 给 OpenAI Responses 新增的 `input_tokens` 计数接口选择了按生成请求校验，维护者可能就此讨论；PR 正文已如实说明。
- 风险二：未合并的 #61063 也给 `render_chat_request` / `render_chat` 加参数（计数时跳过工具解析器校验），谁后合入谁需要简单变基。
- 等待维护者加 `ready` 标签并审阅；不要请求加标签。收到意见后在本分支修改，每个提交带 `-s`，推送前先拉取机器人可能合入的 main；回复和签名先经用户确认。
