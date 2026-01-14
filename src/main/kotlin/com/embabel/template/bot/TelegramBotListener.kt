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
package com.embabel.template.bot

import com.embabel.template.service.SurveyResponseService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@Component
class TelegramBotListener(
    @Value("\${telegram.bot.token}") private val botToken: String,
    @Value("\${telegram.bot.username:EmbabelBot}") private val botUsername: String,
    private val surveyResponseService: SurveyResponseService
) : TelegramLongPollingBot(botToken) {

    private val logger = LoggerFactory.getLogger(TelegramBotListener::class.java)

    @PostConstruct
    fun startBot() {
        try {
            val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
            botsApi.registerBot(this)
            logger.info("Telegram bot listener started. Bot: @$botUsername")
        } catch (e: TelegramApiException) {
            logger.error("Failed to start Telegram bot listener", e)
        }
    }

    override fun getBotUsername(): String = botUsername

    override fun onUpdateReceived(update: Update) {
        try {
            if (update.hasMessage() && update.message.hasText()) {
                val message = update.message
                val chatId = message.chatId
                val userId = message.from?.id ?: return
                val userName = message.from?.firstName
                val messageText = message.text

                surveyResponseService.processMessage(chatId, userId, userName, messageText)

                logger.info("Chat: $chatId | User: $userId ($userName) | Message: '$messageText'")
            }
        } catch (e: Exception) {
            logger.error("Error processing update: ${update.updateId}", e)
        }
    }
}