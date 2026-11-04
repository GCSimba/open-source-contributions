# #856：Firebase 后端发送 response schema 与 penalty

## 状态与来源

- [上游 PR #856](https://github.com/google/adk-kotlin/pull/856)：`fix(firebase): send the response schema and penalties to Firebase`，Open、非草稿、无冲突。
- 状态核验：2026-11-04 11:45（Asia/Shanghai）。第 1 轮审阅意见已处理；11-04 维护者要求再次变基，已完成并回复。标签已由 `waiting on author` 换成 `google support`（“已升级给 Google 内部支持或工程团队”），等待内部处理。CLA 与 `check-changes` 在新提交上通过；validation、标题、提交和安全工作流显示 `action_required`，等待维护者批准运行。
- 基线：`e863facc7faa6ec0b91b2d9080e68bd5d22fda2f`，即 11-04 时最新的 main。按审阅要求变基过两次：首次提交基于 `db97db5`，第 1 轮审阅后基于 `b961c9e`；期间 main 新增的提交都没有改 `Conversions.kt` 和 `ConversionsTest.kt`。
- 修复提交：[fc2216574ea7b1a9e5732992fb65a251986dd0be](https://github.com/GCSimba/adk-kotlin/commit/fc2216574ea7b1a9e5732992fb65a251986dd0be)。提交历史：
  - 首次推送 `8e5e303`，带 `Co-authored-by: Copilot`，CLA 检查因 Copilot 没有签 CLA 而失败；
  - 经用户确认，amend 去掉这一行得到 `a1cdb25`（tree 不变）；
  - 第 1 轮审阅后变基、修改，得到 `5ddf267`；
  - 11-04 按要求再次变基，得到 `fc22165`，diff 与 `5ddf267` 逐字节相同。
  
  PR 描述末尾注明了 AI 辅助。
- 分支：`fix/firebase-response-schema`。
- 选题依据：Firebase 后端只在 Kotlin/Android 中存在，Python ADK 没有对应实现，不会与 Google 内部按 Python 移植的修复重叠。发布前，44 个开放的内部导出 PR 都没有涉及 `firebase/`。
- 重复搜索：在开放和已关闭的 PR、issue 中搜索 firebase responseSchema、output schema、outputSchema、presencePenalty、generation config、structured output、schema，只命中版本发布 PR。`git log -S` 显示 Firebase 模块从未设置过这三个字段。
- [修复补丁](change.patch) · [验证摘要](validation.json) · [研究记录](../../research/README.md)。

## 问题与复现

`BasicRequestProcessor` 把 `LlmAgent.outputSchema` 写成 `config.responseSchema`，并设置 `responseMimeType = "application/json"`。Firebase 的 `Conversions.RequestConverter.generationConfigBuilder()` 只映射了 temperature、maxOutputTokens、topP、topK、stopSequences、candidateCount、responseMimeType、responseModalities 和 thinkingConfig，没有映射 `responseSchema`、`presencePenalty`、`frequencyPenalty`。

- firebase-ai 18.0.0（BOM 35.0.2）的 `GenerationConfig.Builder` 三个字段都有（javap 确认），Gemini 后端（`GenaiConverters.toGenaiSdk`）也都会发送。
- 结果：模型只收到“返回 JSON”的要求，没有结构约束，字段名和层级不受控制；两个 penalty 被静默忽略。
- `Model.canUseOutputSchemaWithTools` 只对 Gemini 2.x 为 false，所以使用 Gemini 3 时，带工具的 agent 也直接依赖 response schema，而不是走 `set_model_response`。

## 修复与回归覆盖

- 直接设置两个 penalty。`responseSchema` 通过新增的 `toFirebaseResponseSchema` 复用现有的 `toFirebaseSchema` 转换：`required` 通过 `optionalProperties` 保留，不支持的约束照常给出警告。
- 例外：没有属性的对象不设置 schema。`toFirebaseSchema` 会把它转换成“装着 JSON 的字符串”，适合工具参数；但用作响应 schema 时，模型会返回带引号的字符串。不设置 schema 时，JSON MIME type 仍会要求返回对象。
- 判断条件 `convertsToFreeFormObject()` 现在也被 `toFirebaseSchema` 的 `OBJECT` / `TYPE_UNSPECIFIED` / `null` 分支调用，两处共用一个定义（审阅意见 1）。
- 测试（`ConversionsTest`）：
  - 已有的 generation config 测试加上两个 penalty；
  - `mapsResponseSchema`：映射含 required 字段的对象 schema；
  - `freeFormResponseSchema_isLeftOut`：覆盖四种情况，`OBJECT`、`OBJECT` 加空 `properties`、`TYPE_UNSPECIFIED`、没有类型；
  - `untypedUnionAndEnumResponseSchemas_areKept`：确认没有类型的 `anyOf` 联合与 `enum` 仍作为 response schema 发送（审阅意见 2）。
  
  除 free-form 测试外，其余三个在修复前失败。

## 审阅记录

- 2026-11-03 07:50–07:52（Asia/Shanghai），aravindmurali22 给出 CHANGES_REQUESTED，并加上 `waiting on author` 标签：
  1. [复用 `convertsToFreeFormObject()`](https://github.com/google/adk-kotlin/pull/856#discussion_r4449178774)；
  2. [补充测试](https://github.com/google/adk-kotlin/pull/856#discussion_r4449186176)：空 properties 和 `TYPE_UNSPECIFIED` 应被跳过，没有类型的 `anyOf`、`enum` 应被保留；
  3. 变基到最新 main。
- 2026-11-03 13:51 用 `--force-with-lease` 推送 `5ddf267`（远端旧值 `a1cdb25`），并更新了 PR 描述的测试与验证部分。
- 随后在两个讨论串回复（[回复 1](https://github.com/google/adk-kotlin/pull/856#discussion_r4451337559)、[回复 2](https://github.com/google/adk-kotlin/pull/856#discussion_r4451337948)），13:53 发了[顶层评论](https://github.com/google/adk-kotlin/pull/856#issuecomment-6596543959)，说明三项都已处理。
- 外部贡献者不能重新请求审阅：REST `requested_reviewers` 返回 404，GraphQL `requestReviews` 返回 FORBIDDEN。
- 2026-11-04 03:03（Asia/Shanghai），aravindmurali22 [请求再次变基到最新 main](https://github.com/google/adk-kotlin/pull/856#issuecomment-6612909000)，并把 `waiting on author` 换成 `google support`。main 新增 13 个提交（e863fac），其中 `11d6139` 改了 Firebase 集成测试和示例，但没有碰本 PR 的两个文件，变基没有冲突。
- 经用户确认，11:39 用 `--force-with-lease` 推送 `fc22165`（远端旧值 `5ddf267`），在 PR 描述的验证部分补了一句变基后的重跑结果，并发了[回复](https://github.com/google/adk-kotlin/pull/856#issuecomment-6621904397)。

## 验证

### 第 2 次变基后（`fc22165`，基线 `e863fac`）

diff 与 `5ddf267` 逐字节相同（SHA-256 一致），`Conversions.kt` 在新 main 上也没有变化，所以只在新基线上重跑 `ConversionsTest`。复用第 1 轮的 Linux 副本，检出新提交并确认工作区干净、父提交为 `e863fac`，用 `--rerun` 强制重跑：78/78 通过。这次运行会编译整个 Firebase 模块的主代码和测试代码，包括上游刚改过的集成测试。`git diff --check` 通过，运行后工作区干净。

模块构建和反证没有重跑：diff 和 `Conversions.kt` 都没有变化，上一次结果在 `5ddf267`（基线 `b961c9e`）上。

### 第 1 轮审阅后（`5ddf267`）

环境：Ubuntu 24.04 / WSL2、OpenJDK 21.0.12.1、Gradle wrapper 9.8.0、Android platform 37.0。从本地仓库新克隆一个独立 Linux 副本，检出该提交（父提交为基线），diff 哈希见 [validation.json](validation.json)。

| 检查 | 结果 |
| --- | --- |
| `ConversionsTest` | 78/78 通过（新增 1 个测试） |
| 反证：`Conversions.kt` 换回 main 版本 | 78 个测试 3 个失败：`mapsResponseSchema`（期望 `OBJECT`）、`returnsBuilder`（期望 `0.6`）、`untypedUnionAndEnumResponseSchemas_areKept`。free-form 测试在修复前本就通过 |
| `:google-adk-kotlin-firebase:build` 与 `:google-adk-kotlin-examples-android:build` | 通过，耗时 19 分 44 秒；Firebase 单元测试 113 个、0 失败，跳过的 8 个是缺少 Firebase 凭据的集成测试 |
| 构建后检查 | 工作区干净；`git diff --check` 通过；没有残留 Gradle 进程 |
| ktfmt 0.65 `--google-style` | 忽略右括号前的尾逗号后，新增行没有差异；原始比较有 4 处，都是尾逗号。校准：基线 `Conversions.kt` 的 4 处差异都是尾逗号；`ConversionsTest.kt` 的 43 处里 41 处是尾逗号，另 2 处是已有行的折行 |

上游的格式化器不会给“单个多行参数”加尾逗号，ktfmt 0.65 会加，所以直接比较时基线文件本身就不通过。这里比较的是去掉这类尾逗号之后的结果，并先在基线文件上校准。

```sh
./gradlew -PjdkVersion=21 "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g" -Dorg.gradle.parallel=false --max-workers=2 --no-daemon --console=plain --stacktrace --continue :google-adk-kotlin-firebase:testDebugUnitTest --tests com.google.adk.firebase.utils.ConversionsTest
./gradlew <同上参数> :google-adk-kotlin-firebase:build :google-adk-kotlin-examples-android:build
```

### 首次提交（`a1cdb25`，基线 `db97db5`）

- 先只套用测试补丁：77 个测试 2 个失败（`mapsResponseSchema`、`returnsBuilder`）。
- 套用修复后 77/77 通过。
- 模块构建通过，耗时 22 分 9 秒；Firebase 单元测试 112 个、0 失败、8 个跳过。

`examples-android` 是唯一依赖 Firebase 模块的模块。构建期间用 `SetThreadExecutionState` 临时阻止 Windows 自动睡眠，进程结束后自动失效。

未运行：真实 Firebase 请求、Android 设备测试、JDK 17/25 矩阵、Dokka（`Conversions` 是 internal 类，没有改动公开 KDoc）。

## 审核与后续

- 等待 Google 内部处理（`google support`）和 aravindmurali22 复审，以及维护者批准工作流。
- 维护者可能在导入前再次要求变基：main 一有新提交，PR 就会落后。变基后先确认 diff 哈希不变，再在新基线上重跑 `ConversionsTest`。
- 后续修改仍在 `fix/firebase-response-schema` 上进行，保持单提交（amend + `--force-with-lease`），**不要加 `Co-authored-by` trailer**，否则 CLA 检查会失败。