# vLLM 研究记录

## 候选筛选

筛选限定在本机没有 GPU、能在 CPU 上端到端验证的问题，主要检查 OpenAI 兼容服务入口（`vllm/entrypoints/openai/`）。vLLM 前端的热门缺陷常在报告后数小时内就有人提交 PR，动手前需要先搜索开放 PR 和 issue。

| 候选 | 结论 | 依据 |
| --- | --- | --- |
| `/v1/chat/completions/batch` 不报告 `completion_tokens_details.reasoning_tokens` | 已确认并提交 [#64935](https://github.com/vllm-project/vllm/pull/64935)，2026-11-04 合并 | [PR 记录](../prs/64935-batch-reasoning-tokens/README.md)、[usage 探针](repros/README.md) |
| generative scoring 的参数与顺序问题 | 重复，不再提交 | 已有 [#64468](https://github.com/vllm-project/vllm/pull/64468)、[#63625](https://github.com/vllm-project/vllm/pull/63625) |
| beam search 不遵守 stop 字符串 | 重复，不再提交 | 已有 [#52621](https://github.com/vllm-project/vllm/pull/52621) |
| 在线服务中 beam search 与 structured outputs 组合 | 重复，不再提交 | 已有 [#36285](https://github.com/vllm-project/vllm/pull/36285) |
| `/tokenize` 的 `mm_processor_kwargs` 处理 | 不是缺陷 | 核实后符合当前设计 |
| Gemma4 参数被原地修改 | 未继续 | 影响面窄，不足以单独提交 |

“没有同题 PR”只代表搜索时的公开记录，不能证明没有其他人在私下处理。

## 2026-11-02 第二轮筛选（暂停）

基于 main `e8570ef`，本机仅 CPU。方法：

- 检查近 10 天合并的 16 个前端 PR，看兄弟路径是否遗漏同样的修复；
- 三轮差分探针，约 45 组对照：流式与非流式 usage，`/tokenize`、`count_tokens` 与实际生成的 prompt token 数，Responses、Anthropic、batch、completions 之间，prefix cache 命中，n=2，以及 Prometheus 指标增量与 usage；
- 静态检查 batch 端点、run-batch、pooling（rerank/score）、Cohere 兼容接口、`developer` 角色处理。

| 候选 | 结论 | 依据 |
| --- | --- | --- |
| `/v1/completions` 多 prompt 时 `cached_tokens` 只取一个 prompt（非流式取最后一个，流式取最先返回的） | 重复 | [#58116](https://github.com/vllm-project/vllm/pull/58116)，9 月起开放，需要 rebase |
| 流式第一个 usage 块没有缓存拆分，导致 Anthropic `message_start` 报告 `input_tokens=256` 且没有 cache 字段 | 重复 | [#65036](https://github.com/vllm-project/vllm/pull/65036)，11-02 已批准 |
| Anthropic `/v1/messages` 静默忽略 `min_tokens` | 重复 | issue [#65095](https://github.com/vllm-project/vllm/issues/65095)；[#65057](https://github.com/vllm-project/vllm/pull/65057)、[#47613](https://github.com/vllm-project/vllm/pull/47613) |
| `/v1/completions` 从不报告 `created_cache_tokens`（chat、batch、Responses、Anthropic 都有） | 未被覆盖，但与 #58116 改的是同一段代码 | 等 #58116 合并后可作为后续 PR |
| Cohere `/v2/embed` 的 `truncate=END` 没有明确指定方向，沿用 tokenizer 默认值 | 不提交 | 19 个主流 embedding 模型都默认从右截断，不会触发 |
| run-batch 中一行请求非法会让整批失败 | 设计如此 | `test_completions_invalid_input` 要求返回码非 0 |
| Prometheus 指标、pooling、batch 与单对话、`developer` 角色 | 一致，无问题 | 差分探针与源码 |

结论：vLLM 前端有大量（含 AI 辅助的）贡献者在系统排查，新报告的 bug 往往几小时内就有 PR。探针脚本保存在本地工具目录，可在新功能合入后重跑。

## 2026-11-04 第三轮筛选

基于 main `11558e3`（提交时变基到 `7663a8c`），本机仅 CPU，CPU wheel 升级到 dd4e401 的 nightly。方法：

- 后台研究代理检查了 9/20 以来 209 个开放 bug 中的 109 个，约 95% 已有 PR，往往在报告后数小时内；
- 约 120 个边界与非法请求探针，覆盖 chat、completions、batch、Responses、Anthropic、tokenize；没有 5xx；
- 对照最近合并的修复，检查兄弟路径；
- 阅读 Robert Shaw 的 Anthropic 加固 RFC [#58647](https://github.com/vllm-project/vllm/issues/58647)。

| 候选 | 结论 | 依据 |
| --- | --- | --- |
| `/v1/messages/count_tokens` 对超过 `max_model_len` 的提示词返回 400，`/tokenize` 能计数 | 未被覆盖，已提交 [#65697](https://github.com/vllm-project/vllm/pull/65697) | 见 [PR 记录](../prs/65697-count-tokens-over-limit/README.md) |
| DeepSeek V4.1 DSML 工具调用解析的 5 种失败（issue [#62051](https://github.com/vllm-project/vllm/issues/62051)） | 已认领 | 有人在 issue 中认领，并开了 [#62517](https://github.com/vllm-project/vllm/pull/62517)、[#62259](https://github.com/vllm-project/vllm/pull/62259) |
| 用户文本中的 `<\|image_pad\|>` 被当成图片占位符（issue [#57740](https://github.com/vllm-project/vllm/issues/57740)） | 不提交 | DarkLight1337 认为应由客户端处理，并担心全量扫描的性能开销 |
| RFC #58647 中“无人处理”的条目（tool_result 图片、is_error、系统块拼接、长 prefill 的 ping） | 已认领 | 评论认领，或已有 #58700、#58682、#61356 等 PR |
| `/v1/messages/count_tokens` 带工具时要求 `--tool-call-parser`（issue [#63573](https://github.com/vllm-project/vllm/issues/63573)） | 已认领 | [#61063](https://github.com/vllm-project/vllm/pull/61063) |
| batch 端点静默忽略 `stream: true` 和 `tools` | 价值低，未提交 | 端点小众，文档已说明不支持 |
| Responses `tools: []` 加 `tool_choice: required` 返回 400，chat 已改为按 `none` 处理（[#65547](https://github.com/vllm-project/vllm/pull/65547)） | 未提交 | OpenAI Responses 的行为不确定 |
| `/detokenize` 遇到超出词表的 token id 返回空字符串和 200 | 价值低，未提交 | — |

注意：RFC #58647 的作者在 10-04 明确表示不欢迎未经人工审查的 AI 生成 PR。在 Anthropic 接口方向提交时，正文需要写清人工审阅和验证。

## 2026-11-06 第四轮筛选

基于 nightly `a2bcb56`，CPU wheel 为 `0.33.0rc1.dev806`，本机仅 CPU。方法：

- 36 个流式与非流式一致性探针，模型为 Qwen3-0.6B，开启 hermes 工具解析器和 qwen3 推理解析器：
  - 覆盖 chat、Anthropic、Responses、completions；
  - 组合工具调用（auto、required、指定工具、并行、截断）、推理、stop、logprobs、n=2；
  - 另外开启 `VLLM_ENABLE_RESPONSES_API_STORE`，测试 Responses 的链式调用。
- 后台研究代理扫描 10-29 以来的开放 bug，看了约 150 个标题、细读约 45 个，并检查近期合并修复的兄弟路径。
- 顺带查看 Rust 前端对齐路线图 [#44280](https://github.com/vllm-project/vllm/issues/44280)：未勾选的条目大多已在评论中认领，或要求先做设计讨论。

| 候选 | 结论 | 依据 |
| --- | --- | --- |
| 工具调用被 max_tokens 截断时，hermes 非流式把 `<tool_call>` 原文放进 content，流式则返回参数为空的工具调用。Anthropic 同理：一边是 text 块，一边是 `input: {}` 的 tool_use | 已有 issue 和 PR | issue [#47903](https://github.com/vllm-project/vllm/issues/47903)，PR [#30481](https://github.com/vllm-project/vllm/pull/30481)。解析引擎上的同类问题已由 #47562 修复 |
| 开启 thinking 后接工具调用，流式多出 `"\n\n"` 的 content，Anthropic 流式多出一个只有换行的 text 块 | 已认领 | issue [#61972](https://github.com/vllm-project/vllm/issues/61972) 有人 `/assign`，PR [#62125](https://github.com/vllm-project/vllm/pull/62125) |
| `/v1/messages` 请求体校验失败时返回 OpenAI 的错误格式 | 已有 PR | RFC #58647 第 12 条，PR [#58023](https://github.com/vllm-project/vllm/pull/58023) |
| 服务正常关停时打印 `EngineDeadError` traceback | 已有 PR | issue [#48745](https://github.com/vllm-project/vllm/issues/48745)，PR [#49000](https://github.com/vllm-project/vllm/pull/49000) |
| Responses 用 `previous_response_id` 续接时丢掉上一轮的 `function_call`：续接时 prompt 为 177 token，手动拼接同样的历史为 201 token | 维护者搁置 | issue [#43244](https://github.com/vllm-project/vllm/issues/43244)。PR #43247 曾获一位审阅者批准，但 chaunceyjiang 认为 `previous_response_id` 属于有状态功能，相关开发暂停，等 [agentic-api](../../agentic-api/README.md)；之后该 PR 过期关闭。11-06 经用户确认在 issue 中[留言](https://github.com/vllm-project/vllm/issues/43244#issuecomment-6687572944)，询问现在是否接受最小修复 |
| 研究代理：Anthropic 指定不存在的工具时不报错 | 不成立 | 第三轮边界探针已确认返回 400 |
| 研究代理：慢速 tokenizer 下 `/score`、`/rerank` 截断时返回 500 | 不成立 | transformers 5.22.1 下指定 `use_fast=False` 仍得到 fast tokenizer，触发不到 |
| `store: true` 但未开启存储时，`previous_response_id` 只返回 404 "not found" | 价值低，未提交 | 未开启存储时静默关闭 store 是有意的设计（代码注释有说明），而且有状态功能正在迁往 agentic-api |

结论：直接报告的前端 bug 几乎都在数小时内被认领；本轮一致性探针发现的真实问题，也都已有人在处理。下一步转向 vLLM 组织下的 [agentic-api](../../agentic-api/README.md)。探针脚本保存在本地工具目录。

## #64935 的相关 PR

状态核验于 2026-11-01（Asia/Shanghai）。

| PR | 状态 | 内容 | 与 #64935 的关系 |
| --- | --- | --- | --- |
| [#64757](https://github.com/vllm-project/vllm/pull/64757) | 10 月 31 日合并 | 同一端点报告 `prompt_tokens_details` | 先例，只处理 prompt 侧 |
| [#64041](https://github.com/vllm-project/vllm/pull/64041) | Open | derender 接口报告 reasoning 与 cached tokens | 不同端点 |
| [#58010](https://github.com/vllm-project/vllm/pull/58010) | Open | `include_reasoning=false` 时隐藏批量响应的 `logprobs`、`token_ids` | 不涉及 usage；与本 PR 修改同一函数的不同段落，后合并的一方可能需要变基 |
| [#62727](https://github.com/vllm-project/vllm/pull/62727) | Open | 批量 chat completions 遵守 `X-Vllm-Priority` | 修改同一文件，不涉及 usage |
| [#55432](https://github.com/vllm-project/vllm/pull/55432) | Open | reasoning effort 映射到 thinking budget | 修改同一文件，不涉及 usage |

#64935 已于 2026-11-04 先合并；上表其余 PR 的状态此后未再核验。

上游 `test_include_reasoning.py` 的模块说明要求隐藏推理内容时 reasoning tokens 仍体现在 usage 中，#64935 在批量接口沿用这一语义。

## 贡献节奏参考

vLLM 与 SGLang 的合并耗时和未回复积压对比见 [PR 节奏分析](../../../sgl-project/sglang/research/pr-latency-analysis.md)，数据范围与结论在该文中说明。
