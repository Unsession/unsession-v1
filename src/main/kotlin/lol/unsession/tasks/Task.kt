package lol.unsession.tasks

import lol.unsession.utils.getLogger
import org.slf4j.Logger

enum class TaskPriority {
    CRITICAL, // executing always
    HIGH, // executing if no critical tasks
    MEDIUM, // executing if needed
    INFO // may not execute if system is overloaded
}

abstract class Task<T>(val taskName: String, val priority: TaskPriority = TaskPriority.INFO) {
    private val logger: Logger = getLogger(taskName)

    fun execute(): Result<T> {
        logger.info("Task:${taskName}, priority:$priority - start executing")
        val result = task()
        when {
            result.isSuccess -> {
                logger.info("Task executed successfully with result:${result.getOrNull()}")
            }
            result.isFailure -> {
                logger.error("Task failed with error:${result.exceptionOrNull()?.message}")
            }
        }
        return result
    }
    abstract fun task() : Result<T>
}
