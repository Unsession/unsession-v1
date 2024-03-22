package apu.unsession.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

fun getLogger(tag: String): Logger = LoggerFactory.getLogger(tag).apply {
    atLevel(Level.INFO)
}