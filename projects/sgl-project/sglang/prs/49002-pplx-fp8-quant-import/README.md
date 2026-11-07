# #49002：pplx FP8 分发导入已迁移的量化函数

## 状态与来源

- [上游 PR #49002](https://github.com/sgl-project/sglang/pull/49002)：2026-11-07 10:02（Asia/Shanghai）创建，Open、非草稿、无冲突；GitHub 自动加上 `quant` 标签。
- 状态核验：2026-11-07 10:04。审核状态为 `REVIEW_REQUIRED`，CODEOWNERS 自动请求 merrymercy、Ying1123、Fridge003、ispobock、HaiShaw、ch-wan、BBuf、Edwardf0t1 审核；没有分配 Merge Oncall。
- 官方 CI：未运行。18 个失败都是门禁：11 个 `pr-gate`/`call-gate` 因缺少 `run-ci` 标签失败，7 个 `*-finish` 汇总任务随之失败；PR 标题检查、`check-changes` 等通过，`lint` 当时仍在运行。
- 基线：`5e1774d137c6560e375632ba8594c04aeb510e11`（发布前从 `0afb736d` 变基，变基前提交 `59b8065`，patch-id 相同）。
- 修复提交：[9fdcee947fca0cf0f734ae1d4714f60c18af22d9](https://github.com/GCSimba/sglang/commit/9fdcee947fca0cf0f734ae1d4714f60c18af22d9)。
- 分支：`fix/pplx-fp8-quant-import`。
- 相关历史：[#30784](https://github.com/sgl-project/sglang/pull/30784)（7 月 13 日把 FP8 量化函数迁到 `sglang.kernels`）、[#31089](https://github.com/sgl-project/sglang/pull/31089)（迁移后修正其他调用方的导入）、[#30756](https://github.com/sgl-project/sglang/pull/30756)（7 月 30 日接入 pplx 后端，沿用了旧路径）。
- 重复搜索：发布前（11-07 00:28 与 10:01 两次）搜索 pplx、`fp8_kernel`、`sglang_per_token_group_quant_fp8`、旧模块路径及所有提到 pplx 的开放 PR 的改动文件，没有同题 Issue 或 PR。唯一也改 `pplx.py` 的开放 PR [#43436](https://github.com/sgl-project/sglang/pull/43436) 处理 `topk_ids=-1` 填充，不涉及这个导入。
- [修复补丁](change.patch) · [验证摘要](validation.json)。

## 问题与复现

`_PplxDispatcherImpl._quantize` 在 FP8 分支里延迟导入：

```python
from sglang.srt.layers.quantization.fp8_kernel import sglang_per_token_group_quant_fp8
```

该模块在 #30784 中迁到 `sglang.kernels.ops.quantization.fp8_kernel`，旧路径没有兼容别名。pplx 后端在迁移之后才合入，因此这条路径从接入起就无法执行。

`get_deepep_output_dtype` 在没有显式指定 BF16、量化配置也没有覆盖时默认返回 FP8，所以 `--moe-a2a-backend pplx` 在默认配置下第一次 MoE 分发就报 `ModuleNotFoundError`。导入写在函数内，加载 `pplx.py` 不会失败；pplx 的端到端测试只在 `test/manual/ep/`，CI 不覆盖。

CPU 复现（`0afb736d`）：在虚拟环境中导入旧模块报 `ModuleNotFoundError`，新模块导出同名函数；直接构造 `_PplxDispatcherImpl` 并以 `use_fp8=True` 调用 `_quantize` 得到同一错误。

## 修复与回归覆盖

- 改为从 `sglang.kernels.ops.quantization.fp8_kernel` 导入，与 DeepEP、DeepEP v2 分发器一致。
- 新增 `test/registered/unit/layers/moe/test_pplx_quantize.py`（`register_cpu_ci`，`base-a-test-cpu`）：
  - FP8：以 group size 128 调用量化函数，返回量化结果和 float32 scale。
  - BF16：不调用量化函数，原样返回 hidden states、scale 为 `None`。

## 验证

Ubuntu 24.04 / WSL2、Python 3.12、仅 CPU；torch 2.14.1+cpu，依赖为 `python/pyproject.toml` 的 CPU 子集，另按新 main 的需要补装 `blake3`：

| 检查 | 结果 |
| --- | --- |
| 修复前运行新测试 | 1 失败（FP8，`ModuleNotFoundError`）、1 通过（BF16 对照） |
| 修复后：新测试及 3 个相邻分发器测试文件 | 32 通过、53 子测试通过 |
| 格式与静态检查 | isort、ruff check/format、codespell 及仓库 5 个 lint 脚本通过 |
| 官方 CI | 未运行，等待 `run-ci` 标签 |

测试在变基前的 `59b8065` 上运行；变基后 patch-id 相同，`pplx.py`、量化函数模块和分发 dtype 选择在两个基线之间均未变化，未重跑。

```sh
PYTHONPATH=python python -m pytest -q -p no:cacheprovider \
  test/registered/unit/layers/moe/test_pplx_quantize.py \
  test/registered/unit/layers/moe/test_deepep_v2_wire_dtype.py \
  test/registered/unit/layers/moe/test_standard_dispatcher.py \
  test/registered/unit/layers/moe/test_deepep_import_guard.py
```

未运行：完整 pre-commit、完整 `base-a-test-cpu`、需要 pplx-kernels（sm_90、attention DP ≥ 2）的多 GPU pplx 测试及 GPU CI。

## 发现过程

在 SGLang 第二轮筛选中，用 AST 扫描整个 `python/sglang`，逐条解析 `from sglang... import ...`（含函数内延迟导入，跳过 try/except 保护的导入），找出指向不存在模块或名字的语句。13 处命中中，这是唯一会在默认配置运行、尚无人报告的一处；其余见[研究记录](../../research/README.md)。

## 审核与后续

- 可能的推动人：ch-wan（#30756 的合并者，已被自动请求审阅）、#30756 作者 trangdough。
- 下一步：等待审阅和 `run-ci` 标签；评论、@ 维护者或在 Slack 询问先经用户确认。
- 收到审核意见后在 `fix/pplx-fp8-quant-import` 修改并推送到 `fork/fix/pplx-fp8-quant-import`。
