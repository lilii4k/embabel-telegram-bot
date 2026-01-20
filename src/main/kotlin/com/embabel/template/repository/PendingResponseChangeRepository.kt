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
package com.embabel.template.repository

import com.embabel.template.entity.PendingResponseChange
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PendingResponseChangeRepository : JpaRepository<PendingResponseChange, Long> {
    fun findByChatIdAndUserId(chatId: Long, userId: Long): PendingResponseChange?
    fun deleteByChatIdAndUserId(chatId: Long, userId: Long)
}