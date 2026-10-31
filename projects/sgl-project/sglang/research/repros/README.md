# SGLang 复现草稿

## reproduce_setwise_logprobs.py

在修复前定位 #47649 的缺陷，不依赖 SGLang 安装、模型或 GPU。

- 适用提交：`fb9846d3e2d66811b372b28c73229e9dbbf54288`（脚本内固定）。
- 做法：从该提交的 `tokenizer_manager_score_mixin.py` 原样提取 `ScoreResult`、`TokenizerManagerScoreMixin` 两个类定义，输入合成的模型结果，只测试结果后处理；另用 AST 检查 fused 调用处是否转发了该参数。
- 运行：需要网络读取 GitHub raw 文件，`python reproduce_setwise_logprobs.py`。
- 2026-10-31 结果：pointwise 返回 `[[-1.0, -2.0], [-3.0, -4.0]]`；batched setwise 返回 `[]`；fused setwise 返回 `None`；输入中有 3 行可用的原始 logprobs。

脚本固定读取旧基线的源码，断言的是缺陷行为，只能说明修复前的问题，不能当作修复后的验证。修复后的检查以 PR 中加入上游的回归测试为准。

脚本为本档案原创，不包含上游源码，运行时读取的 SGLang 代码遵循其 [Apache 2.0 许可](../../LICENSE)。
