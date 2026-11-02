# vLLM 复现草稿

## batch_usage_probe.py

对同一个 reasoning 服务，分别通过 `/v1/chat/completions` 和 `/v1/chat/completions/batch` 发送同一对话，打印两者的 `usage`，用于 #64935 的前后对照。

- 适用提交：修复提交 `a36cacaf1209e9d69034bae92e52156b7903b3f6`（实际运行于内容相同的变基前版本，基线 `cfa9f96f5efbb45c5c02ab127b51eaf8b86bb4ca`）；审核修改 `f062ee2` 只改测试的跳过条件和 Buildkite 文件，没有改 `batch_serving.py`。修复前的批量 usage 缺失由回归测试的失败输出证明，探针只在修复后运行过。
- 做法：用 vLLM 测试工具 `RemoteOpenAIServer` 启动 `Qwen/Qwen3-0.6B --reasoning-parser qwen3 --max-model-len 2048 --enforce-eager`，请求参数为 `max_tokens=64`、`temperature=0`、`include_reasoning=false`，批量请求包含同一对话两次。
- 运行：从 vLLM 源码根目录、在已安装 vLLM 的 Linux 环境中执行，脚本需导入源码中的 `tests.utils`：

  ```sh
  PYTHONPATH=. python /path/to/batch_usage_probe.py
  ```

- 2026-11-01 结果（CPU，修复后）：单对话 `completion_tokens=64`、`reasoning_tokens=63`；批量两次 `completion_tokens=128`、`reasoning_tokens=126`。
- 服务关闭时日志中的 `AsyncLLM output_handler failed` / `EngineDeadError` 出现在两个请求都返回 200 之后，是 SIGTERM 强制停止引擎产生的，不影响结果。

脚本为本档案原创，不包含上游源码，与实际运行的版本内容相同，仅换行符统一为 LF。运行时导入的 vLLM 代码遵循其 [Apache 2.0 许可](../../LICENSE)。
