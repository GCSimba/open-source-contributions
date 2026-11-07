# SGLang

SGLang 是面向大语言模型和多模态模型的高性能推理与服务框架。这里记录其贡献与排查过程。

- 上游：[sgl-project/sglang](https://github.com/sgl-project/sglang)
- Fork：[GCSimba/sglang](https://github.com/GCSimba/sglang)
- 贡献规范：[贡献指南](https://github.com/sgl-project/sglang/blob/main/docs/docs/developer_guide/contribution_guide.mdx)、[PR 模板](https://github.com/sgl-project/sglang/blob/main/.github/pull_request_template.md)、[维护与合并流程](https://github.com/sgl-project/sglang/blob/main/.github/MAINTAINER.md)
- CPU 单元测试规范：[test/registered/unit/README.md](https://github.com/sgl-project/sglang/blob/main/test/registered/unit/README.md)
- 代码材料许可：[Apache 2.0](LICENSE)

## 贡献要点

- PR 标题格式 `[Type] scope: title`，类型为 Feat、Fix、Refactor、Perf、Doc、Test、CI、Chore。
- 修改需附单元测试；CPU 测试放在 `test/registered/unit/` 并调用 `register_cpu_ci`，不启动服务、不加载模型权重。
- 外部贡献者不能自行触发 CI，需要有权限的人添加 `run-ci` 标签；PR 作者可对自己的 PR 使用 `/rerun-failed-ci`。
- 合并需要 Codeowner 审批和 CI 通过，由 Merge Oncall 或有写权限的人合并。
- 找推动人：先查 `.github/CODEOWNERS` 中改动文件的负责人，再对照 `.github/CI_PERMISSIONS.json` 看谁能加 `run-ci`；功能原作者通常最合适。流程中“机器人分配 Merge Oncall”并不总会发生。

## PR 记录

| PR | 主题 | 档案 |
| --- | --- | --- |
| [#47649](https://github.com/sgl-project/sglang/pull/47649) | setwise 评分返回 token_logprobs | [问题、回归与验证](prs/47649-setwise-token-logprobs/README.md) |
| [#49002](https://github.com/sgl-project/sglang/pull/49002) | pplx FP8 分发导入已迁移的量化函数 | [问题、回归与验证](prs/49002-pplx-fp8-quant-import/README.md) |

## 研究与复现

[研究记录](research/README.md) 汇总候选筛选、已排除的重复选题和 PR 节奏分析；[复现目录](research/repros/README.md) 保存修复前的 CPU 组件复现脚本。

各 PR 的 `change.patch` 来自修复提交与其父提交之间的 Git 差异；SHA、来源见记录及 [artifacts.json](artifacts.json)。最终源码和后续变化以对应上游 PR 为准。

## 本地工作位置

实际路径记录在仓库根目录忽略的 `workspace.local.json`；本地 `.workspaces/sgl-project/sglang` 与 `.cache/sgl-project/sglang` 分别提供源码和验证工具的入口。

SGLang 依赖 Unix 专用模块并要求 Python ≥ 3.11，测试需在 Linux（如 WSL）中运行。记录中的命令从 SGLang 源码根目录、在已安装依赖的 Python 虚拟环境中执行；本档案仓库本身不包含构建配置。
