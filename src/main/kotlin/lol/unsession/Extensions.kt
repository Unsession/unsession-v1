package lol.unsession

import java.io.File
import kotlin.random.Random

fun createFileAndWrite(uri: String, actions: (File) -> Unit) {
    val file = File(uri)
    file.createNewFile()
    actions(file)
}