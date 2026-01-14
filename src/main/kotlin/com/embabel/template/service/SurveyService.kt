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
package com.embabel.template.service

import com.embabel.template.entity.Survey
import com.embabel.template.entity.SurveyResponse
import com.embabel.template.entity.SurveyStatus
import com.embabel.template.repository.SurveyRepository
import com.embabel.template.repository.SurveyResponseRepository
import com.embabel.template.tools.TelegramTools
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SurveyService(
    private val surveyRepository: SurveyRepository,
    private val surveyResponseRepository: SurveyResponseRepository,
    private val telegramTools: TelegramTools
) {
    private val logger = LoggerFactory.getLogger(SurveyService::class.java)

    fun createSurvey(chatId: Long, question: String, expectedCount: Int): Survey {
        logger.info("Creating survey for chat $chatId with question: $question, expecting $expectedCount responses")
        val survey = Survey(
            chatId = chatId,
            question = question,
            expectedCount = expectedCount
        )
        return surveyRepository.save(survey)
    }

    fun getActiveSurvey(chatId: Long): Survey? {
        return surveyRepository.findTopByChatIdAndStatusOrderByCreatedAtDesc(chatId, SurveyStatus.ACTIVE)
    }

    fun recordResponse(survey: Survey, userId: Long, userName: String?, responseText: String): Boolean {
        if (surveyResponseRepository.existsBySurveyIdAndUserId(survey.id!!, userId)) {
            logger.info("User $userId already responded to survey ${survey.id}")
            return false
        }

        val response = SurveyResponse(
            survey = survey,
            userId = userId,
            userName = userName,
            response = responseText
        )
        surveyResponseRepository.save(response)
        logger.info("Recorded response from user $userId for survey ${survey.id}")

        checkAndCompleteSurvey(survey)
        return true
    }

    private fun checkAndCompleteSurvey(survey: Survey) {
        val responseCount = surveyResponseRepository.countBySurveyId(survey.id!!)
        logger.debug("Survey ${survey.id} has $responseCount/${survey.expectedCount} responses")

        if (responseCount >= survey.expectedCount) {
            completeSurvey(survey)
        }
    }

    private fun completeSurvey(survey: Survey) {
        logger.info("Completing survey ${survey.id}")
        survey.status = SurveyStatus.COMPLETED
        survey.completedAt = LocalDateTime.now()

        val responses = surveyResponseRepository.findBySurveyId(survey.id!!)
        val summary = generateSummary(survey, responses)
        survey.summary = summary

        surveyRepository.save(survey)

        println("\n" + "=".repeat(80))
        println("📊 SURVEY COMPLETE - ID: ${survey.id}")
        println("=".repeat(80))
        println(summary)
        println("=".repeat(80) + "\n")

        logger.info("Survey ${survey.id} completed. Summary displayed in console.")
    }

    private fun generateSummary(survey: Survey, responses: List<SurveyResponse>): String {
        val builder = StringBuilder()
        builder.append("Question: ${survey.question}\n\n")
        builder.append("Responses (${responses.size}/${survey.expectedCount}):\n")
        responses.forEachIndexed { index, response ->
            builder.append("${index + 1}. ${response.userName ?: "User ${response.userId}"}: ${response.response}\n")
        }
        return builder.toString()
    }
}
