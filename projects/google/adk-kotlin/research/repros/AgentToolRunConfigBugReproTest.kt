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
import com.google.adk.kt.agents.LlmCallsLimitExceededException
import com.google.adk.kt.agents.RunConfig
import com.google.adk.kt.models.LlmResponse
import com.google.adk.kt.runners.InMemoryRunner
import com.google.adk.kt.sessions.SessionKey
import com.google.adk.kt.testing.DummyModel
import com.google.adk.kt.testing.DummyTool
import com.google.adk.kt.testing.modelFunctionCallResponse
import com.google.adk.kt.testing.modelMessage
import com.google.adk.kt.testing.testInvocationContext
import com.google.adk.kt.testing.testToolContext
import com.google.adk.kt.testing.userMessage
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

class AgentToolRunConfigBugReproTest {
  @Test
  fun directRunnerStopsChildBeforeSecondModelCall(): Unit = runBlocking {
    val runner = InMemoryRunner(agent = twoCallAgent(), appName = "run_config_control")
    val session =
      runner.sessionService.createSession(
        SessionKey(appName = "run_config_control", userId = "user", id = "control")
      )

    assertFailsWith<LlmCallsLimitExceededException> {
      runner
        .runAsync(
          userId = session.key.userId,
          sessionId = session.key.id!!,
          newMessage = userMessage("Find the answer"),
          runConfig = RunConfig(maxLlmCalls = 1),
        )
        .toList()
    }
  }

  @Test
  fun agentToolStopsChildBeforeSecondModelCall(): Unit = runBlocking {
    val context = testToolContext(testInvocationContext(runConfig = RunConfig(maxLlmCalls = 1)))

    assertFailsWith<LlmCallsLimitExceededException> {
      AgentTool(twoCallAgent()).run(context, mapOf("request" to "Find the answer"))
    }
  }

  private fun twoCallAgent(): LlmAgent =
    LlmAgent(
      name = "lookup_agent",
      model =
        DummyModel.createSequential(
          "lookup-model",
          listOf(
            modelFunctionCallResponse("lookup", id = "lookup-1"),
            LlmResponse(content = modelMessage("Lookup completed")),
          ),
        ),
      tools = listOf(DummyTool(name = "lookup", declares = true)),
    )
}
