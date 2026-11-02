# #856：Firebase 后端发送 response schema 与 penalty

## 状态与来源

- [上游 PR #856](https://github.com/google/adk-kotlin/pull/856)：`fix(firebase): send the response schema and penalties to Firebase`，Open、非草稿、无冲突。
- 状态核验：2026-11-02 23:20（Asia/Shanghai）。CLA 与 `check-changes` 通过；validation、标题、提交和安全工作流显示 `action_required`，等待维护者操作。
- 基线：`db97db5e3992ed531f3fa41b0be8ef4880191ca7`。发布时 main 已新增 2 个提交（[#825](https://github.com/google/adk-kotlin/pull/825) 的 ToolExecutionMode 与 AgentTool、workflow 示例文档），不涉及 Firebase，未变基。
- 修复提交：[a1cdb25b99615aa6c3a394774d7b8d9fa544e6c7](https://github.com/GCSimba/adk-kotlin/commit/a1cdb25b99615aa6c3a394774d7b8d9fa544e6c7)。首次推送的 `8e5e303` 带 `Co-authored-by: Copilot`，CLA 检查因 Copilot 没有签 CLA 而失败。经用户确认，amend 去掉这一行（tree 不变）后 `--force-with-lease` 推送，CLA 通过；PR 描述末尾注明了 AI 辅助。
- 分支：`fix/firebase-response-schema`。
- 选题依据：Firebase 后端只在 Kotlin/Android 中存在，Python ADK 没有对应实现，不会与 Google 内部按 Python 移植的修复重叠。发布前，44 个开放的内部导出 PR 都没有涉及 `firebase/`。
- 重复搜索：开放与已关闭的 PR、issue：firebase responseSchema / output schema / outputSchema / presencePenalty / generation config / structured output / schema，只命中版本发布 PR。`git log -S` 显示 Firebase 模块从未设置过这三个字段。
- [修复补丁](change.patch) · [验证摘要](validation.json) · [研究记录](../../research/README.md)。

## 问题与复现

`BasicRequestProcessor` 把 `LlmAgent.outputSchema` 写成 `config.responseSchema`，并设置 `responseMimeType = "application/json"`。Firebase 的 `Conversions.RequestConverter.generationConfigBuilder()` 只映射了 temperature、maxOutputTokens、topP、topK、stopSequences、candidateCount、responseMimeType、responseModalities 和 thinkingConfig，没有 `responseSchema`、`presencePenalty`、`frequencyPenalty`。

- firebase-ai 18.0.0（BOM 35.0.2）的 `GenerationConfig.Builder` 三个字段都有（javap 确认），Gemini 后端（`GenaiConverters.toGenaiSdk`）也都会发送。
- 结果：模型只收到“返回 JSON”的要求，没有结构约束，字段名和层级不受控制；两个 penalty 被静默忽略。
- `Model.canUseOutputSchemaWithTools` 只对 Gemini 2.x 为 false，所以使用 Gemini 3 时，带工具的 agent 也直接依赖 response schema，而不是走 `set_model_response`。

## 修复与回归覆盖

- 直接设置两个 penalty；`responseSchema` 通过新增的 `toFirebaseResponseSchema` 复用现有的 `toFirebaseSchema` 转换，`required` 通过 `optionalProperties` 保留，不支持的约束照常给出警告。
- 例外：没有属性的对象不设置 schema。`toFirebaseSchema` 会把它转换成“装着 JSON 的字符串”，适合工具参数，但用作响应 schema 时模型会返回带引号的字符串；不设置 schema 时，JSON MIME type 仍会要求返回对象。判断条件 `convertsToFreeFormObject` 与 `toFirebaseSchema` 进入 `freeFormObject` 分支的条件一致。
- 测试（`ConversionsTest`）：已有的 generation config 测试加上两个 penalty；新增对象 schema（含 required 字段）的映射测试；新增无属性对象被跳过的测试。前两个在修复前失败。

## 验证

Ubuntu 24.04 / WSL2、OpenJDK 21.0.12.1、Gradle wrapper 9.8.0、Android platform 37.0；独立 Linux 副本，先只套用测试补丁，再套用修复：

| 检查 | 结果 |
| --- | --- |
| 修复前 `ConversionsTest` | 77 个测试 2 个失败：`mapsResponseSchema`（期望 `OBJECT`，实际 `null`）、`returnsBuilder`（期望 `0.6`，实际 `null`） |
| 修复后 `ConversionsTest` | 77/77 通过 |
| `:google-adk-kotlin-firebase:build` 与 `:google-adk-kotlin-examples-android:build` | 通过，22 分 9 秒；Firebase 单元测试 112 个、0 失败，8 个是缺少 Firebase 凭据的集成测试被跳过 |
| 构建后检查 | 工作区只有两个改动文件，构建没有改动其他文件；`git diff --check` 通过；没有残留 Gradle 进程 |
| ktfmt 0.65 `--google-style` | 基线文件本身就不通过（尾随逗号差异）；新增的主代码行不需改动，测试中两处只会加尾随逗号，与文件里 43 处现有写法相同，保持现有风格 |

```sh
./gradlew -PjdkVersion=21 "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g" -Dorg.gradle.parallel=false --max-workers=2 --no-daemon --console=plain --stacktrace --continue :google-adk-kotlin-firebase:testDebugUnitTest --tests com.google.adk.firebase.utils.ConversionsTest
./gradlew <同上参数> :google-adk-kotlin-firebase:build :google-adk-kotlin-examples-android:build
```

`examples-android` 是唯一依赖 Firebase 模块的模块。构建期间用 `SetThreadExecutionState` 临时阻止 Windows 自动睡眠，进程结束后自动失效。未运行：真实 Firebase 请求、Android 设备测试、JDK 17/25 矩阵、Dokka（`Conversions` 是 internal 类，没有改动公开 KDoc）。

## 审核与后续

- 等待维护者批准工作流并审阅。#774 由 damianmomotgoogle 审阅批准。
- 收到意见后在 `fix/firebase-response-schema` 修改，保持单提交（amend + `--force-with-lease`），**不要加 `Co-authored-by` trailer**，否则 CLA 检查会失败。
