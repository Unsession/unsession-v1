package lol.unsession.services

import lol.unsession.db.Repository
import lol.unsession.security.user.User

object ReferralService {
    suspend fun referralRegister(user: User, code: String): Boolean {
        val referrer = Repository.Users.getUserCodeCreator(code) ?: return false
        if (!Repository.Codes.activateCode(code, user.id)) return false
        return Repository.Users.addReferrer(user.id, referrer.id)
    }
}
