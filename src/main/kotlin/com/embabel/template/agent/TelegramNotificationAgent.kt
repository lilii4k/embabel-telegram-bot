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
import com.embabel.template.tools.TelegramTools
import org.springframework.context.annotation.Profile

@Agent(
    description = "Send one-way notification messages via Telegram. Use this agent ONLY for sending messages that do NOT require a response. This is for notifications, announcements, alerts, or informing someone of something. Do NOT use this for asking questions or collecting responses - use SurveyAgent for that. Keywords: send message, notify, announce, tell, inform."
)
@Profile("!test")
class TelegramNotificationAgent(
    private val telegramTools: TelegramTools
) {

    @AchievesGoal(
        description = "A Telegram message has been sent to a user"
    )
    @Action
    fun messageUser(
        userInput: UserInput,
        context: OperationContext
    ): String {
        return context.ai()
            .withAutoLlm()
            .withToolObject(telegramTools)
            .generateText(
                """
                The user wants to send a Telegram message. Parse their request and use the sendTelegramMessage tool.

                User request: ${userInput.content}

                Extract the chat ID (phone number or ID) and message, then use the sendTelegramMessage tool to send it.
                After sending, respond with a confirmation message.
                """.trimIndent()
            )
    }
}