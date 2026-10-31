# SGLang 研究记录

## 候选筛选

筛选限定在无 GPU 时能用 CPU 单元测试验证的问题，主要检查 OpenAI 兼容服务入口、tokenizer 与评分 API（`python/sglang/srt/entrypoints/`、`python/sglang/srt/managers/tokenizer_manager_score_mixin.py`）。

| 候选 | 结论 | 依据 |
| --- | --- | --- |
| setwise 评分丢失 `return_token_logprobs` | 已确认并提交 [#47649](https://github.com/sgl-project/sglang/pull/47649) | [CPU 组件复现](repros/README.md)、[PR 记录](../prs/47649-setwise-token-logprobs/README.md) |
| reranker yes/no token 可能落到 unknown token 或碰撞 | 重复，不再提交 | 已有 [#47427](https://github.com/sgl-project/sglang/pull/47427) |
| SequenceClassification setwise 忽略 temperature | 重复，不再提交 | 已有 [#41267](https://github.com/sgl-project/sglang/pull/41267) |

“没有同题 PR”只代表搜索时的公开记录，不能证明没有其他人在私下处理。

## 贡献节奏参考

[PR 合并耗时与未回复积压分析](pr-latency-analysis.md) 对比了 vLLM 与 SGLang（2026-08-01 至 2026-10-31 已合并 PR，以及当时全部开放 PR），汇总数据见 [pr_metrics_summary.json](pr_metrics_summary.json)。主要结论：排除成员/协作者后，两者合并耗时接近，中位数约 5 天，P90 超过一个月。
