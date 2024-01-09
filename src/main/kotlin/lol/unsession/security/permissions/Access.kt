package lol.unsession.security.permissions

/**
 * ПОРЯДОК ИМЕЕТ ЗНАЧЕНИЕ
 * */
enum class Access {
    BRB, // Big Red Button

    HW, // homeworks
    HW_A, // homeworks adding

    T, // tests
    T_A, // tests adding
    T_R, // tests rating
    T_AN, // tests answers
    T_S, // tests answers

    TE, // teachers
    TE_A, // teachers adding
    TE_R, // teachers rating
    TE_RE, // teachers reviewing

    U, // users
    U_A, // users adding
    U_RM, // users removing
    U_B, // users blocking
    U_RC, // users roles changing
    U_IE, // users info editing

    SS, // superuser
}