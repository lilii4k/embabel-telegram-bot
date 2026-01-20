/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.template.agent

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.domain.io.UserInput
import com.embabel.template.domain.SurveyInitiated
import com.embabel.template.service.SurveyService
import com.embabel.template.tools.TelegramTools
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile

@Agent(description = "Creates and sends surveys to Telegram users. Use this agent to initiate a new survey. Keywords: ask, question, survey, poll, create, send.")
@Profile("!test")
class SurveyInitiationAgent(
    private val surveyService: SurveyService,
    private val telegramTools: TelegramTools
) {
    private val logger = LoggerFactory.getLogger(SurveyInitiationAgent::class.java)

    @AchievesGoal(description = "Survey created and sent to Telegram")
    @Action
    fun initiateSurvey(
        userInput: UserInput,
        context: OperationContext
    ): SurveyInitiated {
        val extractionPrompt = """
            Parse this survey request and extract the parameters in this exact format:
            chatId: <number>
            question: <the question>
            expectedCount: <number>

            User request: ${userInput.content}

            Examples:
            - "Ask user 8360446449 what their favourite colour is"
              → chatId: 8360446449
              → question: What is your favourite colour?
              → expectedCount: 1

            - "Ask 5 users in group -123456 what their favorite food is"
              → chatId: -123456
              → question: What is your favorite food?
              → expectedCount: 5

            If the user mentions asking ONE person or ONE user, set expectedCount to 1.
            If they mention a specific number, use that number.

            Respond with ONLY the three lines in the format shown above, nothing else.
        """.trimIndent()

        val extracted = context.ai()
            .withAutoLlm()
            .generateText(extractionPrompt)

        logger.info("Extracted parameters: $extracted")

        val lines = extracted.trim().lines()
        val chatId = lines.find { it.startsWith("chatId:") }
            ?.substringAfter("chatId:")?.trim()?.toLongOrNull()
            ?: throw IllegalArgumentException("Could not extract chatId from: $extracted")

        val question = lines.find { it.startsWith("question:") }
            ?.substringAfter("question:")?.trim()
            ?: throw IllegalArgumentException("Could not extract question from: $extracted")

        val expectedCount = lines.find { it.startsWith("expectedCount:") }
            ?.substringAfter("expectedCount:")?.trim()?.toIntOrNull()
            ?: throw IllegalArgumentException("Could not extract expectedCount from: $extracted")

        logger.info("Parsed - chatId: $chatId, question: $question, expectedCount: $expectedCount")

        val survey = surveyService.createSurvey(chatId, question, expectedCount)
        logger.info("Survey created with ID: ${survey.id}")

        val message = """
            📊 New Survey!

            Question: $question

            Please reply with your answer. Survey completes after $expectedCount responses.
        """.trimIndent()

        val sendResult = telegramTools.sendTelegramMessage(chatId, message)
        logger.info("Survey sent to Telegram: $sendResult")

        return SurveyInitiated(
            surveyId = survey.id!!,
            chatId = chatId,
            question = question,
            expectedCount = expectedCount
        )
    }
}
