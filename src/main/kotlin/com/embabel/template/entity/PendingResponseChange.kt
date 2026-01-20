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
package com.embabel.template.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "pending_response_changes")
data class PendingResponseChange(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val surveyId: Long,

    @Column(nullable = false)
    val chatId: Long,

    @Column(nullable = false)
    val userId: Long,

    @Column
    val userName: String?,

    @Column(nullable = false, columnDefinition = "TEXT")
    val oldResponse: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val newResponse: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)