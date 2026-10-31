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
package com.google.adk.kt.tools

import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.artifacts.InMemoryArtifactService
import com.google.adk.kt.models.LlmRequest
import com.google.adk.kt.models.LlmResponse
import com.google.adk.kt.runners.InMemoryRunner
import com.google.adk.kt.sessions.SessionKey
import com.google.adk.kt.testing.DummyModel
import com.google.adk.kt.testing.DummyTool
import com.google.adk.kt.testing.modelMessage
import com.google.adk.kt.testing.userMessage
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.FunctionCall
import com.google.adk.kt.types.Part
import com.google.adk.kt.types.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

class LoadArtifactsParallelResponseBugReproTest {
  @Test
  fun run_loadArtifactsAfterAnotherTool_includesRequestedArtifactInNextModelRequest() =
    runBlocking {
      assertArtifactReachesAnsweringModel(loadArtifactsFirst = false)
    }

  @Test
  fun run_loadArtifactsBeforeAnotherTool_includesRequestedArtifactInNextModelRequest() =
    runBlocking {
      assertArtifactReachesAnsweringModel(loadArtifactsFirst = true)
    }

  private suspend fun assertArtifactReachesAnsweringModel(loadArtifactsFirst: Boolean) {
    val artifacts = InMemoryArtifactService()
    val filename = "invoice.txt"
    val payload = "Invoice total is 1234.56 USD"
    artifacts.saveArtifact(
      SessionKey("artifact_app", "user1", "session1"),
      filename,
      Part(text = payload),
    )
    val currencyCall = Part(functionCall = FunctionCall("get_currency", id = "currency-1"))
    val artifactCall =
      Part(
        functionCall =
          FunctionCall(
            "load_artifacts",
            args = mapOf("artifact_names" to listOf(filename)),
            id = "artifact-1",
          ),
      )
    val orderedCalls =
      if (loadArtifactsFirst) listOf(artifactCall, currencyCall)
      else listOf(currencyCall, artifactCall)
    var calls = 0
    var answerRequest: LlmRequest? = null
    val model =
      DummyModel("artifact-model") { request ->
        calls++
        if (calls == 1) {
          flowOf(
            LlmResponse(
              content = Content(role = Role.MODEL, parts = orderedCalls),
            ),
          )
        } else {
          answerRequest = request
          flowOf(LlmResponse(content = modelMessage("Finished")))
        }
      }
    val agent =
      LlmAgent(
        name = "invoice_agent",
        model = model,
        tools =
          listOf(
            DummyTool(name = "get_currency", declares = true) { _, _ ->
              mapOf("currency" to "USD")
            },
            LoadArtifactsTool(),
          ),
      )
    val runner =
      InMemoryRunner(agent = agent, appName = "artifact_app", artifactService = artifacts)
    try {
      val events =
        runner.runAsync("user1", "session1", newMessage = userMessage("Read the invoice")).toList()
      val responses = events.flatMap { it.functionResponses() }
      assertEquals(orderedCalls.map { it.functionCall!!.name }, responses.map { it.name })
      val artifactResponse = responses.single { it.name == "load_artifacts" }
      assertEquals(listOf(filename), artifactResponse.response["artifact_names"])
      val nextRequest = assertNotNull(answerRequest)
      assertTrue(
        nextRequest.contents.flatMap { it.parts }.any { it.text == payload },
        "A successful load_artifacts response must supply the file to the answering model; " +
          "loadArtifactsFirst=$loadArtifactsFirst",
      )
    } finally {
      runner.close()
    }
  }
}
