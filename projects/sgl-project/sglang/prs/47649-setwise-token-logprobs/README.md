# #47649：setwise 评分返回 token_logprobs

## 状态与来源

- [上游 PR #47649](https://github.com/sgl-project/sglang/pull/47649)：Open、非草稿；GitHub 自动加上 `documentation` 标签。
- 状态核验：2026-10-31 23:46（Asia/Shanghai）。审核状态为 `REVIEW_REQUIRED`，已自动请求 sundar24295s、JustinTong0323、wisclmy0611、zijiexia、sogalin 审核；暂无人工评论或审核意见。
- `claude[bot]` 提示 fork PR 不自动审查，需要维护者评论 `@claude review`；`mintlify[bot]` 提示文档预览需维护者批准。
- 官方 CI：`pr-gate` / `call-gate` 失败，原因是缺少 `run-ci` 标签，不是代码问题。外部贡献者需要有权限的人添加标签后 CI 才会运行，见[贡献指南](https://github.com/sgl-project/sglang/blob/main/docs/docs/developer_guide/contribution_guide.mdx)。
- 基线：`67bd749ada5bebf3f1ed9be16a814a3243f348bd`（发布前从 `fb9846d3` 变基，变基前提交 `ab321a06`）。
- 修复提交：[6d9731914faaae91dae901f27794ed3696a7c76a](https://github.com/GCSimba/sglang/commit/6d9731914faaae91dae901f27794ed3696a7c76a)。
- 分支：`fix/setwise-token-logprobs`。
- 重复搜索：发布前搜索 `setwise token_logprobs`、`return_token_logprobs`、setwise 缺失 logprobs，以及 10 月 30 日以后新建的评分相关 PR，均无同题 PR。原始功能 [#41188](https://github.com/sgl-project/sglang/pull/41188) 未说明此组合有意不支持。
- 同模块已排除的重复选题：[#47427](https://github.com/sgl-project/sglang/pull/47427)（reranker yes/no token 校验）、[#41267](https://github.com/sgl-project/sglang/pull/41267)（SequenceClassification setwise 忽略 temperature）。
- [修复补丁](change.patch) · [验证摘要](validation.json) · [CPU 组件复现](../../research/repros/README.md)。

## 问题与复现

CausalLM 模型的 `/v1/score` 同时传 `score_extraction_token`（setwise）和 `return_token_logprobs=True` 时，参数通过校验，但原始标签 logprobs 被静默丢弃：

| 路径 | 修复前 `token_logprobs` |
| --- | --- |
| Pointwise | 每个 item 一行（正常） |
| Setwise，默认 batched | `[]` |
| Setwise，`--enable-mis` fused | `null` |

调度器已经返回每个 anchor 的 `input_token_ids_logprobs`，数据在结果后处理中丢失：

- `_process_single_item_scoring_results` 的 setwise 分支构建评分矩阵后直接 `continue`，没有记录原始 logprobs。
- `score_request` 没有把开关传给 `_process_multi_item_extraction_results`，该函数也没有这个参数。
- 即使收集到数据，`ScoringResponse.token_logprobs` 的类型是二维列表，嵌套的 setwise 结构会在响应校验时失败。

## 修复与回归覆盖

- 新增 `_score_anchor_rows`，两条 setwise CausalLM 路径共用：按 anchor 计算分数（行为不变），仅在请求时额外保留原始标签 logprobs，读取方式与 pointwise 相同。
- fused 路径接收并转发 `return_token_logprobs`。
- `ScoreResult.token_logprobs` 与 `ScoringResponse.token_logprobs` 改为“与 `scores` 同形”：pointwise 为 `[num_rows x num_labels]`，setwise 为 `[num_items][Nᵢ x num_labels]`，沿用仓库中 `pooled_hidden_states` 的既有约定。
- 同步更新 `engine_score_mixin.py` docstring 和 `native_api.mdx`。

不请求该参数时行为不变；使用该字段的 decisions、systemone 接口走 pointwise 路径，不受影响。

新增 2 项回归：

- `test_token_scoring.py::test_http_setwise_token_logprobs_follow_scores`：从 `OpenAIServingScore` 经 `score_request` 到结果处理，覆盖 batched 与 `--enable-mis`、每个 item 不同的标签、1 个和 2 个 anchor 的 item。只伪造模型执行，伪造结果随 anchor 位置变化，分组或顺序错误都会失败；同时校验 `scores == softmax(token_logprobs / T)`。
- `test_setwise_score_mixin.py::test_generation_setwise_token_logprobs_grouped_per_item`：两个处理函数、标签顺序与行顺序不同，以及未请求时为 `None`。

## 验证

Ubuntu 24.04 / WSL2、Python 3.12、仅 CPU；torch 2.14.1+cpu、transformers 5.19.0、tokenizers 0.23.2，依赖按 `python/pyproject.toml` 版本安装其 CPU 子集：

| 检查 | 结果 |
| --- | --- |
| 修复前基线（`fb9846d3`），两个评分测试文件 | 60 通过、25 子测试通过 |
| 修复前运行新回归 | 4 个子测试失败，症状与缺陷一致 |
| 修复后，两个评分测试文件 | 62 通过、29 子测试通过（变基前后各运行一次） |
| 下游 decisions / systemone / embed overrides 测试 | 75 通过、109 子测试通过（变基前提交，补丁逐字节相同） |
| 格式与静态检查 | isort、ruff check/format、codespell 及仓库 3 个 lint 脚本通过 |
| 官方 CI | 未运行，等待 `run-ci` 标签 |

定向测试命令，从 SGLang 源码根目录在虚拟环境中执行：

```sh
PYTHONPATH=python python -m pytest -q -p no:cacheprovider \
  test/registered/unit/managers/test_setwise_score_mixin.py \
  test/registered/unit/test_token_scoring.py
```

未运行：完整 pre-commit 套件、完整 `base-a-test-cpu` 套件、真实 CausalLM 的端到端 `/v1/score` 请求、GPU CI。检查工具直接对 6 个改动文件运行，版本取自 `.pre-commit-config.yaml`。

## 验证过程中的环境问题

- SGLang 依赖 Unix 专用的 `resource` 模块并要求 Python ≥ 3.11，Windows 本机无法运行测试，改用 WSL。
- WSL 系统 Python 缺少 `python3-venv`，改用用户级 uv 创建独立虚拟环境，没有修改系统包。
- 在 WSL 中对 Windows checkout 运行 pre-commit 框架时，换行符差异使 Git 把整个仓库视为已修改，hook 扫描了全部文件。已中止，并核实没有文件被改写、没有遗留 stash；之后改为直接对改动文件运行同版本工具。

## 审核与后续

- 下一步：请有权限的维护者添加 `run-ci` 标签或评论 `/tag-and-rerun-ci`，并按 [MAINTAINER.md](https://github.com/sgl-project/sglang/blob/main/.github/MAINTAINER.md) 联系 Merge Oncall；也可在 SGLang Slack `#pull-request` 频道询问。
- 收到审核意见后在源码分支修改，运行受影响测试，再推送到 `fork/fix/setwise-token-logprobs`。
