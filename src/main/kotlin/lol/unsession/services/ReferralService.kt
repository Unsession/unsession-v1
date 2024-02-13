package lol.unsession.services

import lol.unsession.db.Repository
import lol.unsession.db.models.UserDto

object ReferralService {
    suspend fun afterRegisterNewUser(userId: Int) {
        Repository.Codes.create(userId, 10)
    }
    suspend fun referralRegister(user: UserDto, code: String): Boolean {
        val referrer = Repository.Users.getUserCodeCreator(code) ?: return false
        if (!Repository.Codes.activateCode(code, user.id)) return false
        return Repository.Users.addReferrer(user.id, referrer.id)
    }
}
