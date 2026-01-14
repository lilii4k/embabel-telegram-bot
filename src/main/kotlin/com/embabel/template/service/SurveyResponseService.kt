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

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SurveyResponseService(
    private val surveyService: SurveyService
) {
    private val logger = LoggerFactory.getLogger(SurveyResponseService::class.java)

    fun processMessage(chatId: Long, userId: Long, userName: String?, messageText: String) {
        val activeSurvey = surveyService.getActiveSurvey(chatId) ?: return

        val recorded = surveyService.recordResponse(activeSurvey, userId, userName, messageText)

        if (recorded) {
            logger.info("Processed survey response from user $userId for survey ${activeSurvey.id}")
        }
    }
}
