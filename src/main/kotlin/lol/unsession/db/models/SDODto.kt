package lol.unsession.db.models

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import kotlinx.serialization.Serializable

data class SDOTest(
    val title: String,
    val resultId: String,
    val questions: List<Question>,
    val completionDate: String,
    val timeSpent: String,
    val personName: String,
    val score: Int,
    val maxScore: Int,
    val percentage: Int
) {
    companion object {
        fun parseSDOTest(html: String): SDOTest {
            val document: Document = Ksoup.parse(html)

            val title = document.select("title").text()
            val resultId = document.select(".otp-item-res-number .number").text().substringAfter('#')
            val completionDate = document.select("#lblEndTime").text()
            val timeSpent = document.select(".otp-item-res-times span").eq(3).text()
            val personName = document.select(".result-username-container span").text()
            val score =
                document.select(".otp-item-result .content .item-table-results tbody tr").eq(0).select("td").last()!!.text()
                    .toInt()
            val maxScore =
                document.select(".otp-item-result .content .item-table-results tbody tr").eq(1).select("td").last()!!.text().toInt()
            val percentageText = document.select(".otp-item-result .content .dSolidGaugePercent-224362522 span").text()
            val percentage = """\d+""".toRegex().find(percentageText)?.value?.toInt() ?: 0

            val questionsElements = document.select("div.otp-item-view-question")
            val questions = questionsElements.map { element -> Question.parseQuestion(element.outerHtml()) }

            return SDOTest(
                title = title,
                resultId = resultId,
                questions = questions,
                completionDate = completionDate,
                timeSpent = timeSpent,
                personName = personName,
                score = score,
                maxScore = maxScore,
                percentage = percentage
            )
        }

    }

    data class Question(
        val number: Int,
        val imageUrl: String?,
        val answerOptions: List<AnswerOption>,
        val selectedAnswerIndex: Int,
        val correctAnswerIndex: Int,
        val pointsEarned: Int
    ) {
        companion object {
            fun parseQuestion(html: String): Question {
                val document: Document = Ksoup.parse(html)
                val questionElement = document.select("div.otp-item-view-question").first()

                val number = questionElement!!.select(".qnumber .num").text().toInt()
                val imageUrl = questionElement.select(".image img").attr("src")

                // Измененный селектор для вариантов ответа
                val answerElements = questionElement.select(".otp-input.s-view-input.otp-radiobutton")
                val answerOptions = answerElements.map { element ->
                    val isCorrect = element.select(".icon-rb-checked").isNotEmpty() // проверка наличия класса для правильного ответа
                    AnswerOption(
                        text = element.text(),
                        isCorrect = isCorrect
                    )
                }

                val selectedAnswerIndex = answerOptions.indexOfFirst { it.isCorrect }
                val correctAnswerIndex = selectedAnswerIndex // Если предполагается, что отмечен правильный ответ
                val pointsEarned = questionElement.select(".otp-item-rw-container .points span").last()?.text()?.toInt() ?: 0

                return Question(
                    number = number,
                    imageUrl = imageUrl,
                    answerOptions = answerOptions,
                    selectedAnswerIndex = selectedAnswerIndex,
                    correctAnswerIndex = correctAnswerIndex,
                    pointsEarned = pointsEarned
                )
            }
        }
    }

    data class AnswerOption(
        val text: String,
        val isCorrect: Boolean
    )

}
