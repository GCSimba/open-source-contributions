# SGLang 研究记录

## 候选筛选

筛选限定在无 GPU 时能用 CPU 单元测试验证的问题，主要检查 OpenAI 兼容服务入口、tokenizer 与评分 API（`python/sglang/srt/entrypoints/`、`python/sglang/srt/managers/tokenizer_manager_score_mixin.py`）。

| 候选 | 结论 | 依据 |
| --- | --- | --- |
| setwise 评分丢失 `return_token_logprobs` | 已确认并提交 [#47649](https://github.com/sgl-project/sglang/pull/47649) | [CPU 组件复现](repros/README.md)、[PR 记录](../prs/47649-setwise-token-logprobs/README.md) |
| reranker yes/no token 可能落到 unknown token 或碰撞 | 重复，不再提交 | 已有 [#47427](https://github.com/sgl-project/sglang/pull/47427) |
| SequenceClassification setwise 忽略 temperature | 重复，不再提交 | 已有 [#41267](https://github.com/sgl-project/sglang/pull/41267) |

“没有同题 PR”只代表搜索时的公开记录，不能证明没有其他人在私下处理。

### 第二轮（2026-11-06 至 11-07，main `0afb736d`）

在独立 worktree 中检出上游 main，不改动 #47649 的分支。

| 方向 | 结论 | 依据 |
| --- | --- | --- |
| 10-10 以后新建的、可在 CPU 验证的 Bug Issue（OpenAI 接口、解析器、采样等） | 几乎都已有 PR，不再提交 | 逐条查时间线交叉引用；唯一无 PR 的 [#46566](https://github.com/sgl-project/sglang/issues/46566) 由报告人认领 |
| 工具调用解析器流式与非流式差分（按各 detector 的 `structure_info()` 生成调用） | 确认缺陷但重复 | 经 `OpenAIServingChat` 实际流式路径复现：Hermes 下整个调用在 1–2 个 delta 内到达时参数丢失，前面带文字时整个调用消失；已有 [#31915](https://github.com/sgl-project/sglang/issues/31915)、[#36129](https://github.com/sgl-project/sglang/pull/36129)、[#32167](https://github.com/sgl-project/sglang/pull/32167) 等 |
| 非流式丢弃工具调用之后的文本 | 不提交 | 多个 detector 共有的设计取舍 |
| 参数校验（`n=0`、`top_logprobs` 越界、`max_completion_tokens=0`） | 价值低，未继续 | 仅静态阅读 |
| AST 扫描失效的 `from sglang... import`（含函数内延迟导入） | 13 处命中，提交 1 处 | 见下表 |

| 失效导入 | 结论 |
| --- | --- |
| `pplx.py` FP8 分发导入 `srt.layers.quantization.fp8_kernel` | 默认配置可达、无人报告，已提交 [#49002](../prs/49002-pplx-fp8-quant-import/README.md) |
| DeepSeek AWQ 导入 `sglang.kernel_api_logging` | 重复：[#46312](https://github.com/sgl-project/sglang/issues/46312)、[#46322](https://github.com/sgl-project/sglang/pull/46322)、[#46560](https://github.com/sgl-project/sglang/pull/46560) |
| MiMo-V2 处理器 mrope 分支导入 `.rope_utils` | 不提交：已发布的 MiMo-V2 系列配置都不含 `mrope_section`，走不到该分支 |
| Beam 导入 `beam_decode_gemv` | 不提交：默认关闭的实验开关，仅 SM103 特定形状 |
| 其余 | `TYPE_CHECKING` 块、ROCm 测试库或编译生成的扩展，运行时不执行或不适用 |

补充扫描 `import sglang.x` 与 `importlib.import_module("sglang.x")` 未发现其他失效目标。

## 贡献节奏参考

[PR 合并耗时与未回复积压分析](pr-latency-analysis.md) 对比了 vLLM 与 SGLang（2026-08-01 至 2026-10-31 已合并 PR，以及当时全部开放 PR），汇总数据见 [pr_metrics_summary.json](pr_metrics_summary.json)。主要结论：排除成员/协作者后，两者合并耗时接近，中位数约 5 天，P90 超过一个月。
