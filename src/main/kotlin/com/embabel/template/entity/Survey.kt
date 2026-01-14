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
@Table(name = "surveys")
data class Survey(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val chatId: Long,

    @Column(nullable = false, columnDefinition = "TEXT")
    val question: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SurveyStatus = SurveyStatus.ACTIVE,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var completedAt: LocalDateTime? = null,

    @Column(nullable = false)
    val expectedCount: Int,

    @Column(columnDefinition = "TEXT")
    var summary: String? = null
)
