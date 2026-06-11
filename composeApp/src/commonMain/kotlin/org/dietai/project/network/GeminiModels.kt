package org.dietai.project.network

import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    val systemInstruction: SystemInstruction? = null,
    val contents: List<Content>
)

@Serializable
data class SystemInstruction(val parts: List<Part>)

@Serializable
data class Content(val parts: List<Part>)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String // Base64
)
