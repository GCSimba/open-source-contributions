/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.adk.kt.processors

import com.google.adk.kt.agents.toCallbackContext
import com.google.adk.kt.artifacts.InMemoryArtifactService
import com.google.adk.kt.testing.testInvocationContext
import com.google.adk.kt.testing.testSession
import com.google.adk.kt.types.Blob
import com.google.adk.kt.types.Part
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class InstructionArtifactTextReproductionTest {
  @Test
  fun textArtifactPlaceholderIncludesDocumentText() = runBlocking {
    val session = testSession()
    val artifacts = InMemoryArtifactService()
    artifacts.saveArtifact(session.key, "report.txt", Part(text = "Revenue: 42 million"))
    val context =
      testInvocationContext(session = session, artifactService = artifacts).toCallbackContext()

    val rendered =
      InstructionStateInjector.injectSessionState(context, "Summarize: {artifact.report.txt}")

    assertEquals("Summarize: Revenue: 42 million", rendered)
  }

  @Test
  fun inlineTextArtifactPlaceholderDecodesDocumentBytes() = runBlocking {
    val session = testSession()
    val artifacts = InMemoryArtifactService()
    val document = "Revenue: 42 million\nRegion: Europe"
    artifacts.saveArtifact(
      session.key,
      "report.txt",
      Part(inlineData = Blob(mimeType = "text/plain", data = document.encodeToByteArray())),
    )
    val context =
      testInvocationContext(session = session, artifactService = artifacts).toCallbackContext()

    val rendered =
      InstructionStateInjector.injectSessionState(context, "Summarize: {artifact.report.txt}")

    assertEquals("Summarize: $document", rendered)
  }
}
