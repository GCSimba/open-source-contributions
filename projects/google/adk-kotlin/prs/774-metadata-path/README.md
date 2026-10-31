# #774：拒绝与内部存储冲突的 artifact 路径

## 状态与来源

- [上游 PR #774](https://github.com/google/adk-kotlin/pull/774)：已于 **2026-10-29 21:36:20（Asia/Shanghai）合并**。
- 状态核验：2026-10-31 20:18（Asia/Shanghai）。
- 基线：`ae938afc1b157db5184e2ed40f4fcb9f2de8afd9`。
- 最终投稿：[2bd04807c64549190ba2d416d6b2ef056682164a](https://github.com/GCSimba/adk-kotlin/commit/2bd04807c64549190ba2d416d6b2ef056682164a)。
- 上游合并：[7a21a5e77f863768c4f978c75e804241b7ec42ab](https://github.com/google/adk-kotlin/commit/7a21a5e77f863768c4f978c75e804241b7ec42ab)。
- 分支：`codex/reject-metadata-artifact-filenames`。
- 行为参考：[Python FileArtifactService](https://github.com/google/adk-python/blob/6c672404ef96863963303b0a03000c35f7068044/src/google/adk/artifacts/file_artifact_service.py#L519-L537)。
- [修复补丁](change.patch) · [验证摘要](validation.json)。

## 问题与复现

`FileArtifactService` 将 artifact payload 与内部元数据写到文件系统。保存名为 `metadata.json` 的 artifact 时，内部元数据可能覆盖 payload；包含 `versions` 路径段的名称也会与版本布局冲突，使发现逻辑返回错误 key。

在未修复实现上，保存 `metadata.json` 后重新读取内容，可观察到原始 payload 没有按预期保留。保存 `report/versions/0/x.txt` 则会让版本目录被误识别为内部存储结构。普通相似文件名作为对照仍应正常往返。

## 最终修复与审核处理

仅在保存时校验规范化且移除作用域前缀的相对路径：拒绝末段 `metadata.json` 和任意 `versions` 段；比较忽略大小写，并裁剪末尾空格和点。旧数据的读取和删除能力保留。

根据审核意见，错误消息采用固定文本、不回显输入文件名；补充 `versions` 路径校验，将回归测试合入既有 `FileArtifactServiceTest`，并 rebase 到当时最新 main。最终投稿保持一个提交，已获审核批准并合并。

## 验证

Windows、JDK 21，针对最终投稿提交：

- `FileArtifactServiceTest`：JVM **44/44**、Android host **44/44**。
- `dokkaGenerate`、Google 风格 ktfmt、Git 差异检查通过。
- 全仓库 Windows 构建因 shell、符号链接相关测试及超时而**未完成**；不记为通过。

可移植命令，从对应源码 checkout 运行：

```sh
./gradlew -PjdkVersion=21 "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g" --max-workers=2 --no-daemon --console=plain --stacktrace :google-adk-kotlin-core:jvmTest --tests com.google.adk.kt.artifacts.FileArtifactServiceTest :google-adk-kotlin-core:testAndroidHostTest --tests com.google.adk.kt.artifacts.FileArtifactServiceTest
./gradlew -PjdkVersion=21 "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g" --max-workers=2 --no-daemon --console=plain --stacktrace dokkaGenerate
```

Windows 使用 `gradlew.bat`。上述计数来自项目原有任务图中的最终检查；研究记录中的早期孤立 harness 为 40 项检查，不能替代这里的 44+44 项结果。
