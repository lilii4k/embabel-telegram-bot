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

import com.embabel.template.domain.SurveyResults
import com.embabel.template.entity.PendingResponseChange
import com.embabel.template.entity.Survey
import com.embabel.template.entity.SurveyResponse
import com.embabel.template.entity.SurveyStatus
import com.embabel.template.repository.PendingResponseChangeRepository
import com.embabel.template.repository.SurveyRepository
import com.embabel.template.repository.SurveyResponseRepository
import com.embabel.template.tools.TelegramTools
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SurveyService(
    private val surveyRepository: SurveyRepository,
    private val surveyResponseRepository: SurveyResponseRepository,
    private val pendingResponseChangeRepository: PendingResponseChangeRepository,
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
        // Re-fetch survey to ensure we have the latest status (in case it was cancelled)
        val currentSurvey = getSurveyById(survey.id!!)

        // Check if survey is still active
        if (currentSurvey.status != SurveyStatus.ACTIVE) {
            logger.info("Survey ${currentSurvey.id} is not active (status: ${currentSurvey.status}), ignoring response from user $userId")
            return false
        }

        val existingResponse = surveyResponseRepository.findBySurveyIdAndUserId(currentSurvey.id!!, userId)

        if (existingResponse != null) {
            logger.info("User $userId already responded to survey ${currentSurvey.id}, initiating change confirmation")

            // Delete any existing pending change for this user
            pendingResponseChangeRepository.deleteByChatIdAndUserId(currentSurvey.chatId, userId)

            // Create a new pending change
            val pendingChange = PendingResponseChange(
                surveyId = currentSurvey.id!!,
                chatId = currentSurvey.chatId,
                userId = userId,
                userName = userName,
                oldResponse = existingResponse.response,
                newResponse = responseText
            )
            pendingResponseChangeRepository.save(pendingChange)

            // Send confirmation message
            val displayName = userName ?: "User $userId"
            val confirmationMessage = """
                $displayName has already submitted a response: "${existingResponse.response}"

                Would you like to change your answer to: "$responseText"?

                Reply with "yes" to change your answer or "no" to keep your original answer.
            """.trimIndent()

            telegramTools.sendTelegramMessage(currentSurvey.chatId, confirmationMessage)
            logger.info("Sent confirmation message to user $userId for response change")

            return false
        }

        val response = SurveyResponse(
            survey = currentSurvey,
            userId = userId,
            userName = userName,
            response = responseText
        )
        surveyResponseRepository.save(response)
        logger.info("Recorded response from user $userId for survey ${currentSurvey.id}")

        // Note: Survey completion is now handled by SurveyResultsAgent
        return true
    }

    fun getCompletedSurvey(chatId: Long): Survey? {
        return surveyRepository.findTopByChatIdAndStatusOrderByCreatedAtDesc(chatId, SurveyStatus.COMPLETED)
    }

    fun markSurveyCompleted(surveyId: Long, analysis: String) {
        val survey = getSurveyById(surveyId)
        survey.status = SurveyStatus.COMPLETED
        survey.completedAt = LocalDateTime.now()
        survey.summary = analysis
        surveyRepository.save(survey)
        logger.info("Survey $surveyId marked as completed")
    }

    fun getSurveyById(surveyId: Long): Survey {
        return surveyRepository.findById(surveyId)
            .orElseThrow { IllegalArgumentException("Survey not found: $surveyId") }
    }

    fun getResponseCount(surveyId: Long): Int {
        return surveyResponseRepository.countBySurveyId(surveyId)
    }

    fun getSurveyResults(surveyId: Long): SurveyResults {
        val survey = surveyRepository.findById(surveyId)
            .orElseThrow { IllegalArgumentException("Survey not found: $surveyId") }

        val responses = surveyResponseRepository.findBySurveyId(surveyId)

        return SurveyResults(
            surveyId = survey.id!!,
            chatId = survey.chatId,
            question = survey.question,
            responses = responses.map { response ->
                SurveyResults.UserResponse(
                    userId = response.userId,
                    userName = response.userName,
                    response = response.response
                )
            },
            summary = survey.summary ?: ""
        )
    }

    fun getPendingResponseChange(chatId: Long, userId: Long): PendingResponseChange? {
        return pendingResponseChangeRepository.findByChatIdAndUserId(chatId, userId)
    }

    @Transactional
    fun applyResponseChange(chatId: Long, userId: Long): Boolean {
        val pendingChange = pendingResponseChangeRepository.findByChatIdAndUserId(chatId, userId)
            ?: return false

        val existingResponse = surveyResponseRepository.findBySurveyIdAndUserId(
            pendingChange.surveyId,
            userId
        ) ?: return false

        // Update the existing response
        existingResponse.response = pendingChange.newResponse
        existingResponse.respondedAt = LocalDateTime.now()
        surveyResponseRepository.save(existingResponse)

        // Delete the pending change
        pendingResponseChangeRepository.deleteByChatIdAndUserId(chatId, userId)

        logger.info("Applied response change for user $userId in survey ${pendingChange.surveyId}")

        // Send confirmation
        val displayName = pendingChange.userName ?: "User $userId"
        val confirmationMessage = "✅ $displayName, your answer has been updated to: \"${pendingChange.newResponse}\""
        telegramTools.sendTelegramMessage(chatId, confirmationMessage)

        return true
    }

    @Transactional
    fun rejectResponseChange(chatId: Long, userId: Long): Boolean {
        val pendingChange = pendingResponseChangeRepository.findByChatIdAndUserId(chatId, userId)
            ?: return false

        // Delete the pending change
        pendingResponseChangeRepository.deleteByChatIdAndUserId(chatId, userId)

        logger.info("Rejected response change for user $userId in survey ${pendingChange.surveyId}")

        // Send confirmation
        val displayName = pendingChange.userName ?: "User $userId"
        val confirmationMessage = "✅ $displayName, your original answer has been kept: \"${pendingChange.oldResponse}\""
        telegramTools.sendTelegramMessage(chatId, confirmationMessage)

        return true
    }

    fun cancelSurvey(surveyId: Long, analysisText: String? = null, sendNotification: Boolean = true) {
        val survey = getSurveyById(surveyId)
        if (survey.status == SurveyStatus.ACTIVE) {
            survey.status = SurveyStatus.CANCELLED
            surveyRepository.save(survey)
            logger.info("Survey $surveyId has been cancelled")

            if (sendNotification) {
                // Get response information
                val responseCount = surveyResponseRepository.countBySurveyId(surveyId)
                val responses = surveyResponseRepository.findBySurveyId(surveyId)

                // Build the cancellation message
                val messageBuilder = StringBuilder()
                messageBuilder.append("❌ Survey Timed Out: $responseCount out of ${survey.expectedCount} responses collected.\n\n")
                messageBuilder.append("Question: ${survey.question}\n\n")

                // Add analysis if provided, otherwise add response list
                if (analysisText != null) {
                    messageBuilder.append(analysisText)
                } else if (responses.isNotEmpty()) {
                    messageBuilder.append("Responses received:\n")
                    responses.forEachIndexed { index, response ->
                        messageBuilder.append("${index + 1}. ${response.userName ?: "User ${response.userId}"}: ${response.response}\n")
                    }
                } else {
                    messageBuilder.append("No responses were received.")
                }

                telegramTools.sendTelegramMessage(survey.chatId, messageBuilder.toString())
            }
        }
    }

    fun sendCancelledSurveyNotification(surveyId: Long, analysisText: String? = null) {
        val survey = getSurveyById(surveyId)
        val responseCount = surveyResponseRepository.countBySurveyId(surveyId)
        val responses = surveyResponseRepository.findBySurveyId(surveyId)

        // Build the cancellation message
        val messageBuilder = StringBuilder()
        messageBuilder.append("❌ Survey Timed Out: $responseCount out of ${survey.expectedCount} responses collected.\n\n")
        messageBuilder.append("Question: ${survey.question}\n\n")

        // Add analysis if provided, otherwise add response list
        if (analysisText != null) {
            messageBuilder.append(analysisText)
        } else if (responses.isNotEmpty()) {
            messageBuilder.append("Responses received:\n")
            responses.forEachIndexed { index, response ->
                messageBuilder.append("${index + 1}. ${response.userName ?: "User ${response.userId}"}: ${response.response}\n")
            }
        } else {
            messageBuilder.append("No responses were received.")
        }

        telegramTools.sendTelegramMessage(survey.chatId, messageBuilder.toString())
    }
}
