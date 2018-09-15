package club.antigfw.utils

import mu.KLogger
import mu.KotlinLogging

fun <R : Any> R.logger(): KLogger =
        KotlinLogging.logger(this::class.java.name)

