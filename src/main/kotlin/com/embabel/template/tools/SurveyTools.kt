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
package com.embabel.template.tools

import com.embabel.template.service.SurveyService
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Component

@Component
class SurveyTools(
    private val surveyService: SurveyService,
    private val telegramTools: TelegramTools
) {
    private val logger = LoggerFactory.getLogger(SurveyTools::class.java)

    @Tool(description = "Create and send a survey to a Telegram chat, waiting for a specific number of responses")
    fun createSurvey(chatId: Long, question: String, expectedCount: Int): String {
        logger.info("Creating survey for chat $chatId with question: $question, expectedCount: $expectedCount")

        val survey = surveyService.createSurvey(chatId, question, expectedCount)

        val message = """
            📊 New Survey!

            Question: $question

            Please reply with your answer. Survey completes after $expectedCount responses.
        """.trimIndent()

        val result = telegramTools.sendTelegramMessage(chatId, message)

        return "Survey created successfully (ID: ${survey.id}). Waiting for $expectedCount responses. $result"
    }

    @Tool(description = "Get the status of the current active survey in a Telegram group")
    fun getSurveyStatus(chatId: Long): String {
        val survey = surveyService.getActiveSurvey(chatId)
            ?: return "No active survey in this group"

        val responseCount = surveyService.getActiveSurvey(chatId)?.let {
            "Active"
        } ?: "Unknown"

        return "Active survey: ${survey.question}\nStatus: Waiting for ${survey.expectedCount} responses"
    }
}
