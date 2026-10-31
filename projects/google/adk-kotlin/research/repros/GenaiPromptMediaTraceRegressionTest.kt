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
package com.google.adk.kt.mlkit

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.adk.kt.models.LlmRequest
import com.google.adk.kt.types.Blob
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.FileData
import com.google.adk.kt.types.Part
import java.util.Base64
import kotlin.test.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Regression evidence for excluding uploaded media payloads and file URIs from traces. */
@RunWith(AndroidJUnit4::class)
class GenaiPromptMediaTraceRegressionTest {
  @Test
  fun uploadedImageBytesAreRedacted() {
    val imageBytes =
      Base64.getDecoder()
        .decode(
          "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII="
        )
    val request =
      LlmRequest(
        contents =
          listOf(
            Content(
              role = "user",
              parts = listOf(Part(inlineData = Blob(mimeType = "image/png", data = imageBytes))),
            )
          )
      )

    val trace = GenaiPromptTracing.format(request)

    assertFalse(trace.contains(imageBytes.contentToString()), trace)
  }

  @Test
  fun fileUriQueryCredentialsAreRedacted() {
    val request =
      LlmRequest(
        contents =
          listOf(
            Content(
              role = "user",
              parts =
                listOf(
                  Part(
                    fileData =
                      FileData(
                        mimeType = "image/png",
                        fileUri = "https://example.invalid/image.png?token=private-test-token",
                      )
                  )
                ),
            )
          )
      )

    val trace = GenaiPromptTracing.format(request)

    assertFalse(trace.contains("private-test-token"), trace)
  }
}
