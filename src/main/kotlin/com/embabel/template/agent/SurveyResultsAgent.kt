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
import com.embabel.template.domain.SurveyCheckInput
import com.embabel.template.domain.SurveyResults
import com.embabel.template.entity.SurveyStatus
import com.embabel.template.service.SurveyService
import com.embabel.template.tools.TelegramTools
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile

@Agent(description = "Processes survey responses and publishes results when complete. This agent is triggered when a user submits a survey response.")
@Profile("!test")
class SurveyResultsAgent(
    private val surveyService: SurveyService,
    private val telegramTools: TelegramTools
) {
    private val logger = LoggerFactory.getLogger(SurveyResultsAgent::class.java)

    @Action
    fun checkCompletion(input: SurveyCheckInput): SurveyResults? {
        val surveyId = input.surveyId
        logger.info("Checking completion status for survey $surveyId")

        val survey = surveyService.getSurveyById(surveyId)

        if (survey.status != SurveyStatus.ACTIVE) {
            logger.info("Survey $surveyId is not active (status: ${survey.status}), agent exiting")
            return null
        }

        val responseCount = surveyService.getResponseCount(surveyId)
        logger.info("Survey $surveyId has $responseCount/${survey.expectedCount} responses")

        if (responseCount >= survey.expectedCount) {
            logger.info("Survey $surveyId is complete, proceeding to publish results")
            return surveyService.getSurveyResults(surveyId)
        }

        logger.info("Survey $surveyId is incomplete, agent exiting")
        return null
    }

    @AchievesGoal(description = "Survey results analyzed and published to Telegram")
    @Action
    fun publishResults(
        surveyResults: SurveyResults,
        context: OperationContext
    ): String {
        logger.info("Publishing results for survey ${surveyResults.surveyId}")

        val analysis = context.ai()
            .withAutoLlm()
            .generateText(
                """
                Analyze the following survey results and provide intelligent insights based on the question asked.

                Question: ${surveyResults.question}

                Responses:
                ${surveyResults.responses.joinToString("\n") { response ->
                    "- ${response.userName ?: "User ${response.userId}"}: ${response.response}"
                }}

                Your task:
                1. Analyze the responses in relation to the question
                2. Process the data to extract meaningful insights
                3. If the question is about unavailability/availability (e.g., "When is everyone unavailable in January"), analyze all the responses and find dates when everyone IS available
                4. If the question is about preferences (e.g., favorite colors, food), summarize patterns or commonalities
                5. If the question requires aggregation or comparison, perform that analysis
                6. Provide a clear, concise summary that directly answers the underlying question

                Format your response as a natural summary that provides actionable information, not just a list of responses. It should be concise and precise, as it is a text message (<50 words). No markup should be used.

                Examples:
                - For availability questions: "Based on everyone's unavailability, the following dates work for everyone: January 5th, 12th, and 20th"
                - For preference questions: "The most popular color is blue (3 people), followed by green (2 people)"
                - For opinion questions: "Most people agree that... while some mentioned..."
                """.trimIndent()
            )

        logger.info("Generated analysis for survey ${surveyResults.surveyId}: $analysis")

        val telegramMessage = """
            📊 Survey Results

            Question: ${surveyResults.question}

            $analysis
        """.trimIndent()

        val sendResult = telegramTools.sendTelegramMessage(surveyResults.chatId, telegramMessage)
        logger.info("Sent results to Telegram: $sendResult")

        surveyService.markSurveyCompleted(surveyResults.surveyId, analysis)
        logger.info("Survey ${surveyResults.surveyId} marked as completed")

        return analysis
    }
}
