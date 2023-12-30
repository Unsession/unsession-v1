package lol.unsession.security

enum class Access {
    BRB, // Big Red Button

    G, // general

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
    U_IE; // users info editing

    companion object {
        fun getAccessCategory(byte: Byte): Access {
            return Access.entries[byte.toInt()]
        }

        fun serialize(roles: Set<String>): BooleanArray {
            val bits = BooleanArray(Access.entries.size)
            for (role in roles) {
                val access = Access.valueOf(role)
                bits[access.ordinal] = true
            }
            return bits
        }

        fun deserialize(bits: BooleanArray): Set<Access> {
            val roles = mutableSetOf<Access>()
            for (i in bits.indices) {
                if (bits[i]) {
                    roles.add(Access.entries[i])
                }
            }
            return roles
        }
    }
}