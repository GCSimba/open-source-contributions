# #837：加载同批并行工具响应中的全部 artifact

## 状态与来源

- [上游 PR #837](https://github.com/google/adk-kotlin/pull/837)：Open、非草稿、无冲突。
- 状态核验：2026-10-31 20:18（Asia/Shanghai）；暂无评论或审核意见。
- CLA 与 `check-changes` 通过；[validation](https://github.com/google/adk-kotlin/actions/runs/41161626827) 及标题、提交、安全工作流显示 `action_required`，等待维护者操作。
- 基线：`2809a837752644f3720eaa759d2cb5144ba1a84e`。
- 修复提交：[433fbd2bdbe250ee9d19da73e2be7da92f19e0f7](https://github.com/GCSimba/adk-kotlin/commit/433fbd2bdbe250ee9d19da73e2be7da92f19e0f7)。
- 分支：`codex/fix-parallel-artifact-loading`。
- 行为参考：[Python LoadArtifactsTool](https://github.com/google/adk-python/blob/e9e8f6156fb73cdfe799af0a0a57786e807732df/src/google/adk/tools/load_artifacts_tool.py#L530-L539)。
- [修复补丁](change.patch) · [验证摘要](validation.json) · [基线复现](../../research/repros/README.md)。

## 问题与复现

运行时把并行工具响应按顺序放入同一消息，旧 `LoadArtifactsTool` 只检查第一个 part。另一工具排在 `load_artifacts` 前面时，工具虽返回文件名，下一次模型请求却缺少 artifact 的实际内容。

用同一真实 Runner 场景交换 `get_currency` 和 `load_artifacts` 的顺序：加载工具在首位时，对照通过；加载工具在后面时，下一次模型请求应包含相同文件内容，但未修复基线上的断言失败。JVM、Android host 均复现该差异。

## 修复与回归覆盖

遍历原始最后一条消息的全部 part，处理所有匹配的 function response；保留响应和文件顺序，跳过无关 part 与缺失文件，并保持原请求不变。

在既有 `LoadArtifactsToolTest` 的 14 项测试基础上增加 3 项回归：Runner 中两种排列顺序，以及文本、其他工具响应、多个加载响应和缺失文件混合的情况。该分支只包含 artifact 加载修复。

## 验证

Windows、JDK 21：JVM **17/17**、Android host **17/17**；ktfmt 与差异检查通过。

```sh
./gradlew -PjdkVersion=21 "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g" --max-workers=2 --no-daemon --console=plain --stacktrace --continue :google-adk-kotlin-core:jvmTest --tests com.google.adk.kt.tools.LoadArtifactsToolTest :google-adk-kotlin-core:testAndroidHostTest --tests com.google.adk.kt.tools.LoadArtifactsToolTest
```

全仓库 Windows 构建在 20 分钟期限后**未完成**。本地外部 init 脚本只排除 `LocalEnvironmentTest.execute_cancellation_killsProcessTree*` 的已知无限等待，其余失败保留；未把排除写入项目源码。记录到 shell/符号链接失败及 MCP 初始化超时，MCP 根因尚未确认。定向测试 XML 不作为完整构建完成的证据。

#838 的 Linux 全量成功结果来自另一个独立分支，不包含本补丁，不用于证明本 PR 的完整构建通过。后续需要在本分支单独补齐 Linux 全量验证。
