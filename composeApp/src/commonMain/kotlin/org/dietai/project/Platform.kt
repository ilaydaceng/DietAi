package org.dietai.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform