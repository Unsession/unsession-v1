package lol.unsession.test

object TestData {
    val names = listOf(
        "Мария",
        "Анджэлла",
        "Анна",
        "Александра",
        "Елизавета",
        "Алина",
        "Анастасия",
        "Антонина",
        "Анфиса",
        "Арина",
        "Валентина",
        "Валерия",
        "Варвара",
        "Василиса",
        "Вера",
        "Вероника",
        "Виктория",
        "Галина",
        "Дарья",
        "Евгения",
        "Екатерина",
        "Елена",
        "Елизавета",
        "Жанна",
        "Зинаида",
        "Зоя",
        "Инна",
        "Ирина",
        "Карина",
        "Кира",
        "Клавдия",
        "Ксения",
        "Лариса",
        "Лидия",
        "Любовь",
        "Людмила",
        "Маргарита",
        "Марина",
        "Мария",
        "Надежда",
        "Наталья",
        "Нина",
        "Оксана",
        "Ольга",
        "Полина",
        "Раиса",
        "Регина",
        "Римма",
        "Светлана",
        "София",
        "Таисия",
        "Тамара",
        "Татьяна",
        "Ульяна",
        "Юлия",
        "Яна"
    )
    val patronymic = listOf("Петровна", "Иванова", "Сергеевна", "Владимировна", "Владислэйвовна", "Александровна")
    val username = listOf(
        "nauka",
        "enjoyer",
        "stalker",
        "gamer",
        "proton",
        "elizaveta",
        "studenter",
        "justme",
        "mazafaker",
        "adminofitmo",
        "princess",
        "qwerty",
        "login",
        "admin",
        "username",
        "angel",
        "sunshine",
        "master",
        "letmein",
        "bailey",
        "dungeonmaster",
        "shadow",
        "monkey",
        "lolkek",
        "itsmylogin",
    )
    val departments = listOf("КТ", "КТиУ", "ПИ", "ИВТ", "ФТМИ", "ФКТИ", "ФИТиКС", "ФИТиУ", "ФИТиП")
    val passwords = listOf(
        "0000",
        "1234",
        "qwerty",
        "password",
        "123456",
        "12345678",
        "12345",
        "111111",
        "1234567",
        "sunshine",
        "qwertyuiop",
        "princess",
        "admin",
        "welcome",
        "666666",
        "abc123",
        "football",
        "123123",
        "monkey",
        "654321",
        "!",
        "1234567890",
        "123456789",
        "dragon",
        "passw0rd",
        "master",
        "hello",
        "freedom",
        "whatever",
        "qazwsx",
        "trustno1",
        "654321",
        "jordan23",
        "harley",
        "password1",
        "1234qwer",
        "michael",
        "football",
        "123abc",
        "letmein",
        "1234567890",
        "1234567",
        "monkey",
        "1qaz2wsx",
        "dragon",
        "baseball",
        "iloveyou",
        "trustno1",
        "123456789",
        "123123",
        "welcome",
        "login",
        "admin",
        "princess",
        "qwertyuiop",
        "solo",
        "flower",
        "zaq1zaq1",
        "zxcvbnm",
        "zaq12wsx",
        "password1",
        "qazwsx",
        "starwars",
        "football",
        "baseball",
        "welcome",
        "1234567890",
        "abc123",
        "111111",
        "1qaz2wsx",
        "dragon",
        "master",
        "monkey",
        "letmein",
        "login",
        "princess",
        "qwertyuiop",
        "solo",
        "passw0rd",
        "starwars",
        "admin",
        "welcome",
        "login",
        "princess",
        "qwertyuiop",
        "solo",
        "flower",
        "zaq1zaq1",
        "zxcvbnm",
        "zaq12wsx",
        "password1",
        "qazwsx",
        "starwars",
        "football",
        "baseball",
        "welcome",
        "1234567890",
        "abc123",
        "111111",
        "1qaz2wsx",
        "dragon",
        "master",
        "monkey",
        "letmein",
        "login",
        "princess",
        "qwertyuiop",
        "solo",
        "passw0rd",
        "starwars",
        "admin",
        "welcome",
        "login",
        "princess",
        "qwertyuiop"
    )
}

object TestSDK {
    val name: String
        get() = TestData.names.random()
    val patronymic: String
        get() = TestData.patronymic.random()
    val username: String
        get() = TestData.username.random()
    /**
     * username@niuitmo.ru*/
    val email: String
        get() = TestData.username.random() + "@niuitmo.ru"
    val department: String
        get() = TestData.departments.random()
    val password: String
       get() = TestData.passwords.random()
    val globalPersonId: Int
        get() = (100000..999999).random()

    fun lorem(words: Int): String {
        val lorem = """
            Lorem ipsum dolor sit amet, consectetur adipiscing elit. 
            Suspendisse at felis felis. Aenean condimentum, erat eget consequat aliquet, magna metus rutrum diam, in dapibus dolor orci eu massa. Mauris ut sem nec massa dapibus blandit. 
            Aenean rhoncus sapien nec justo imperdiet interdum. Praesent suscipit semper justo et lobortis. 
            Morbi finibus semper mi, vel mattis tellus mollis sed. Sed sollicitudin, urna id sollicitudin venenatis, diam metus suscipit massa, nec fermentum nunc massa sit amet ex. 
            Curabitur congue.
        """.trimIndent()
        val wordsSequence = lorem.split(" ")
        val wordsList = mutableListOf<String>()
        while(wordsList.size < words) {
            wordsList.add(wordsSequence[wordsList.size + 1])
        }
        return wordsList.joinToString(" ")
    }
}