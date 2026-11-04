# #64935：批量 chat completions 报告 reasoning tokens

## 状态与来源

- [上游 PR #64935](https://github.com/vllm-project/vllm/pull/64935)：`[Bugfix][Frontend] Report reasoning tokens in batched chat completions`，**2026-11-04 22:58（Asia/Shanghai）已合并**：DarkLight1337 批准并开启自动合并后，以 squash 方式合入 main，提交 [e4991c1](https://github.com/vllm-project/vllm/commit/e4991c1feb8ce6a11c3fa7e9d63974ee3fe25968)。合入内容与本档案的[累计补丁](change.patch)逐行一致（4 个文件，+47 行）。标签 `bug`、`documentation`、`frontend`、`ready`、`ci/build`、`rust`；`ci/build`、`rust` 是首次提交改了 Buildkite YAML 时自动加的，撤回 YAML 后仍保留。
- 状态核验：2026-11-04 23:06（Asia/Shanghai）。DarkLight1337 在 11-02 14:22 提出 1 条行内审核意见，修改并回复后，于 21:09 批准、回复 “Thanks!” 并评论 `/ci run`，同时开启了自动合并（squash）。CI 被 main 上一个已修复的测试泄漏卡住两天；11-04 22:02 经用户确认评论说明原因后，他在 22:26 更新分支并 `/ci retry`，CI 通过后自动合并，见“审核与后续”。
- 检查：pre-run-check、pre-commit、Check format、DCO（含合并提交）、Mergify Summary 均通过。必需检查是 pre-run-check、pre-commit、DCO 和 Buildkite 汇总状态 `buildkite/ci/pr`。第一次完整 CI [Buildkite #99182](https://buildkite.com/vllm/ci/builds/99182)（合并提交 `2a40ba7`）只有一个 AMD MI355 模型分片失败，失败用例与本 PR 无关；Read the Docs（非必需）也因 main 上无关的 docstring 警告失败。维护者把最新 main 合入分支（`b068fa3`）后，[Buildkite #99937](https://buildkite.com/vllm/ci/builds/99937) 只重跑之前失败的步骤，24 分 18 秒通过（AMD 分片已拆成 3 个，全部通过），Read the Docs 也通过。
- 基线：`564ac0de1b228568cafcbfbe8ccaccc82d6023eb`。发布前从 `cfa9f96f5efbb45c5c02ab127b51eaf8b86bb4ca` 变基，两者之间的上游提交只修改 `recipes/models/*.yaml`；变基前提交 `4784d89d`，patch-id 相同。
- 提交：修复提交 [a36cacaf1209e9d69034bae92e52156b7903b3f6](https://github.com/GCSimba/vllm/commit/a36cacaf1209e9d69034bae92e52156b7903b3f6)；审核修改 [f062ee20a68955beaf9022299d9dea955c82a7df](https://github.com/GCSimba/vllm/commit/f062ee20a68955beaf9022299d9dea955c82a7df)。两个提交都含 `Co-authored-by: Copilot` 和 `Signed-off-by: GCSimba`，DCO 签名均经用户确认后添加。分支上另有两个合并 main 的提交：`/ci run` 时机器人生成的 `2a40ba7d5f566fc3cd420d79789f9dcda6177bf2`（作者 vllm-agent），以及 11-04 维护者更新分支生成的 `b068fa37c053c2d8049e5a63ebb5f60a86da69b3`（作者 Cyrus Leung）。合并到 main 的 squash 提交 `e4991c1` 作者为 GCSimba，保留了 `Signed-off-by` 和 `Co-authored-by: Copilot` 两个 trailer。
- 分支：`fix/batch-chat-reasoning-tokens`。
- 先例：[#64757](https://github.com/vllm-project/vllm/pull/64757) 于 10 月 31 日合并，为同一端点报告 `prompt_tokens_details`，未处理 completion 侧。
- 重复搜索：发布前搜索开放与已关闭的 PR、issue：`batch reasoning tokens`、`completions/batch usage`、`batched chat completions usage`、`reasoning_tokens`、`completion_tokens_details`、`OpenAIServingChatBatch`、`batch_serving`，没有同题 PR。相关 PR 及关系见[研究记录](../../research/README.md)。
- [累计补丁](change.patch)（基线到 `f062ee2`，4 个文件）· [验证摘要](validation.json) · [usage 探针](../../research/repros/README.md)。

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
- CI：Rust 前端对 `/v1/chat/completions/batch` 返回 not_supported。按审核意见，新测试用 `@pytest.mark.skipif(envs.VLLM_USE_RUST_FRONTEND, ...)` 在 Rust 前端下跳过（与同目录 `test_thinking_token_budget.py` 写法相同）；Rust 前端的两个 CI 步骤都会导出 `VLLM_USE_RUST_FRONTEND=1`，Python 前端步骤照常运行。首次提交曾在 `rust_frontend.yaml` 和 `test-amd.yaml` 中用 `-k` 排除该测试，已在 `f062ee2` 中撤回，PR 不再修改 Buildkite 文件。
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
| 审核修改后（11-02，`f062ee2` 内容），`VLLM_USE_RUST_FRONTEND=1` 只运行新测试 | 1 skipped、6 deselected，未启动服务，5 秒 | 11-02 20:36 |
| 审核修改后，不设置该变量运行整个文件 | 7 passed | 11-02 20:41 |
| 审核修改后，pre-commit | 全部通过（含“Check new tests are wired into Buildkite CI”）；shellcheck 以 `SKIP` 跳过 | 11-02 20:49 |
| 官方 CI | Buildkite #99182 只有 AMD MI355 basic-models 分片失败（main 上已由 #65140 修复的测试泄漏），Read the Docs 因 main 上无关的 docstring 警告失败（已由 #65157 修复）；维护者合入最新 main 后，Buildkite #99937 重跑失败步骤通过，Read the Docs 通过，PR 自动合并 | 11-04 22:58 |

回归测试在 07:26 加入 `"temperature": 0`。在此之前的草稿曾在修复前失败于第 117 行（06:58），修复后整文件 7 passed（07:04）。表中修复前和整文件的结果是最终版本的重跑，PR 描述中的行号已据此更正为 118。

命令从 vLLM 源码根目录、在 vLLM CPU 环境中执行：

```sh
pytest tests/entrypoints/openai/chat_completion/test_include_reasoning.py -k test_batch_reasoning_tokens_in_usage
pytest tests/entrypoints/openai/chat_completion/test_include_reasoning.py
VLLM_USE_RUST_FRONTEND=1 pytest tests/entrypoints/openai/chat_completion/test_include_reasoning.py \
  -k test_batch_reasoning_tokens_in_usage -rs
pytest tests/entrypoints/openai/chat_completion/test_batched_chat_completions.py \
  -k "echo or harmony or kv_transfer or parser_receives or reasoning_wiring"
SKIP=shellcheck pre-commit run --files <改动文件>
```

第四条为等价的选择表达式，选中 `test_batched_echo_does_not_prepend_user_prompt`、`test_batched_echo_prepends_matching_assistant_prefix`、`test_batched_harmony_response_format_uses_structural_tag`、`test_batch_rejects_kv_transfer_prompt_token_ids`、`test_batched_parser_receives_each_prompt_prefix`、`test_batched_reasoning_wiring_matches_single_chat`。

未运行：`test_batched_chat_completions.py` 中需要 `Qwen/Qwen2.5-1.5B-Instruct` 的 6 个服务测试（该服务未配置 reasoning parser，本补丁只让响应多一个 `null` 字段）；GPU 与 ROCm CI；实际的 Rust 前端服务（新测试在该模式下自动跳过，已在本地设置 `VLLM_USE_RUST_FRONTEND=1` 确认）。模型输出不变，只改变响应 usage，因此没有做模型评测。

## 验证过程中的环境问题

- Windows 无法直接运行 vLLM。WSL 中没有 GPU，也不做 C++ 构建：把本分支的 `vllm/`、`tests/` 同步到 Linux 文件系统镜像，再接入 nightly CPU wheel 的编译扩展。
- triton CPU wheel 在 WSL 中下载超时，改为在 Windows 下载后本地安装；libnuma 没有用 apt 安装，解压到用户目录后通过 `LD_LIBRARY_PATH` 引用。
- vLLM 服务运行时 Windows 提交内存紧张，git 曾报 `Out of memory`；等服务退出后再执行 git。
- 本机 pre-commit 下载 shellcheck 超时，以 `SKIP=shellcheck` 跳过；本 PR 没有 shell 文件。
- 探针日志关闭阶段的 `AsyncLLM output_handler failed` / `EngineDeadError` 出现在两个请求返回 200 之后，是 SIGTERM 强制停止引擎导致的。

## 审核与后续

- 发布后按 PR 机器人的要求，依据 AGENTS.md 和 `/pr-checklist` 复核：改动不是琐碎修改，不重复，有修复前后证据，保留 PR。
- 2026-11-02 14:22，DarkLight1337 对 `rust_frontend.yaml` 的[行内意见](https://github.com/vllm-project/vllm/pull/64935#discussion_r4439453409)：不要修改 YAML，改为在测试中按 `VLLM_USE_RUST_FRONTEND` 跳过。经用户确认，提交 `f062ee2`（带 DCO 签名）完成修改并普通推送，未改写历史；随后在该讨论下[回复](https://github.com/vllm-project/vllm/pull/64935#discussion_r4442587487)说明修改和本地验证结果，并更新 PR 描述的 Details 与验证表。
- 2026-11-02 21:09，DarkLight1337 [批准](https://github.com/vllm-project/vllm/pull/64935#pullrequestreview-5737022364)，回复 “Thanks!”，并评论 `/ci run`；PR 获得 `ready` 标签。21:10 机器人报告分支落后 main 50 个提交，先把 main 合入分支（`2a40ba7`），再启动 [Buildkite #99182](https://buildkite.com/vllm/ci/builds/99182)。
- Read the Docs 在 `2a40ba7` 上失败：`mkdocs build --strict` 有 11 条警告，全部来自 main 上 [#63440](https://github.com/vllm-project/vllm/pull/63440)（11-01）和 [#64029](https://github.com/vllm-project/vllm/pull/64029)（11-02）新增的 `vllm/models/muse_spark/amd/...`、`vllm/models/kimi_k3/amd/...` 中的 docstring 方括号交叉引用，与本 PR 的 4 个文件无关，未在 PR 中评论。11-03 重新读取构建日志确认：11 条 WARNING 分别来自 `muse_spark/amd/mono/kernels/attn_frag`（6 条）和 `kimi_k3/amd/specialized_decode/mono` 下的 3 个文件（5 条），没有一条涉及本 PR 的文档。
- Buildkite #99182（11-02 22:01 结束）只有 `amd-mi355-basic-models-other-shard-2-slash-2` 失败，汇总状态因此显示失败。该分片 26 failed、597 passed，26 个失败全部是 `models/test_muse_spark_rocm_mono.py::test_mono_step_matches_the_stock_loop` 的参数组合，错误相同：HIP kernel `fused_muse_spark_qknorm_rope_kv_insert` 报 `cos_sin_cache must be a contiguous 2-D CUDA tensor`。这是 AMD 上的模型 kernel 测试，与本 PR 的前端批量接口无关（11-03 13:40 核验）。
- 2026-11-03 19:28，ldkhang1201 的 [#65383](https://github.com/vllm-project/vllm/pull/65383) 引用了本 PR：它给 `/inference/v1/generate` 的 derender 模式补 `usage.completion_tokens_details.reasoning_tokens`（叠在 #64041 上），是不同接口的同类修复，不冲突。
- 2026-11-04 查明卡住的原因：
  - DarkLight1337 在 11-02 21:09 批准时开启了自动合并（squash），评审已满足（reviewDecision `APPROVED`）。必需检查中只有 `buildkite/ci/pr` 失败，AMD 分片和 Read the Docs 本身都不是必需检查。
  - 两个失败在 main 上都已修复，只是我们的 CI 正好落在修复前的几分钟：机器人在 21:10 把 main 合入分支（`2a40ba7`）；21:18 合入的 [#65140](https://github.com/vllm-project/vllm/pull/65140) 修掉的正是这 26 个 `test_muse_spark_rocm_mono.py` 失败（`test_muse_spark_rocm_shared_rope.py` 把 CPU 上的 RoPE 留在 `get_rope` 的全局缓存里，同一 pytest 进程中的 mono 测试拿到它）；21:24 合入的 [#65157](https://github.com/vllm-project/vllm/pull/65157) 修掉了 Read the Docs 的 11 条警告。11-04 合并的 13 个 PR 上，该 AMD 分片（现已拆成 3 个）全部通过。
  - 作者自己无法解开：`/ci run` 在已经跑过 CI 的提交上只允许有写权限的人重跑；`/ci retry` 只会在 `2a40ba7` 这个旧快照上重试，测试泄漏还在，会再次失败；作者自己推送合并 main 虽然可以再发 `/ci run`，但 GitHub 会在没有写权限的人推送时自动关闭自动合并，之后仍要维护者手动合并。mergify 的自动同步规则要求没有失败检查，所以也不会自动更新分支。
  - 2026-11-04 22:02，经用户确认[评论](https://github.com/vllm-project/vllm/pull/64935#issuecomment-6632638523)请 DarkLight1337 再 `/ci run` 一次，说明了以上原因。
- 2026-11-04 22:26，DarkLight1337 用 GitHub 的更新分支把最新 main 合入分支（`b068fa3`），并评论 `/ci retry`。新提交上还没有 CI 构建，机器人按规则新建 [Buildkite #99937](https://buildkite.com/vllm/ci/builds/99937)，只运行 #99182 中失败的 1 个步骤；因为是有写权限的人更新的分支，自动合并没有被关闭。22:57 构建通过（AMD 分片现已拆成 3 个，全部通过），Read the Docs 也通过。
- **2026-11-04 22:58 自动合并**：以 squash 方式合入 main（[e4991c1](https://github.com/vllm-project/vllm/commit/e4991c1feb8ce6a11c3fa7e9d63974ee3fe25968)，合并人显示为开启自动合并的 DarkLight1337）。从发布（11-01 08:13）到合并约 3.6 天，从批准到合并约 2 天。
- 经验：
  - 维护者开启自动合并后，作者不要往分支推送，否则 GitHub 会关闭自动合并。
  - CI 因无关失败卡住时，先查失败用例在 main 上是否已修复。若已修复，说明原因并请有写权限的人更新分支后重跑，比单纯说明“与本 PR 无关”更容易推进。
- 后续：PR 已合并，没有待办。fork 上的 `fix/batch-chat-reasoning-tokens` 分支可在用户确认后删除。
