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
import com.embabel.template.tools.SurveyTools
import com.embabel.template.tools.TelegramTools
import org.springframework.context.annotation.Profile

@Agent(description = "Ask questions and collect responses from Telegram users (one or more people). Use this agent whenever the user wants to ASK someone a question and get their response back. This includes surveys, polls, collecting feedback, or asking any individual or group for their input. Keywords: ask, question, survey, poll, fetch, collect, response, answer, what is, tell me.")
@Profile("!test")
class SurveyAgent(
    private val surveyTools: SurveyTools,
    private val telegramTools: TelegramTools
) {

    @AchievesGoal(description = "A survey has been created and sent to users")
    @Action
    fun createSurvey(
        userInput: UserInput,
        context: OperationContext
    ): String {
        return context.ai()
            .withAutoLlm()
            .withToolObject(surveyTools)
            .withToolObject(telegramTools)
            .generateText(
                """
                The user wants to ask a question and collect responses in Telegram. Parse their request and extract:
                1. The chat ID (can be positive for individuals, negative for groups)
                2. The survey question (what they want to ask)
                3. The expected number of responses (how many users should respond)

                User request: ${userInput.content}

                Examples:
                - "Ask user 8360446449 what their favourite colour is"
                  → chatId: 8360446449, question: "What is your favourite colour?", expectedCount: 1
                - "Fetch 1 user (8360446449)'s favourite colour"
                  → chatId: 8360446449, question: "What is your favourite colour?", expectedCount: 1
                - "fetch 5 users in group -123456's favourite colour"
                  → chatId: -123456, question: "What is your favourite colour?", expectedCount: 5
                - "ask 10 people in chat -789 what their favorite food is"
                  → chatId: -789, question: "What is your favorite food?", expectedCount: 10
                - "survey 3 users in -555 about their preferred programming language"
                  → chatId: -555, question: "What is your preferred programming language?", expectedCount: 3

                If the user mentions asking ONE person or ONE user, set expectedCount to 1.
                If they mention a specific number, use that number.
                If they say "everyone" or "all users", ask the user to specify a number.

                Use the createSurvey tool to create and send the survey.
                After creating, confirm with the user that the survey was created successfully.
                """.trimIndent()
            )
    }

    @Action
    fun checkSurveyStatus(
        userInput: UserInput,
        context: OperationContext
    ): String {
        return context.ai()
            .withAutoLlm()
            .withToolObject(surveyTools)
            .generateText(
                """
                The user wants to check the status of an active survey.

                User request: ${userInput.content}

                Extract the chat ID and use the getSurveyStatus tool to check the current status.
                """.trimIndent()
            )
    }
}
