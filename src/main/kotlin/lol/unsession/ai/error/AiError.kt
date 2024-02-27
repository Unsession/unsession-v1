package lol.unsession

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class AiError (

  @SerializedName("error" ) var error : Error? = Error()

)