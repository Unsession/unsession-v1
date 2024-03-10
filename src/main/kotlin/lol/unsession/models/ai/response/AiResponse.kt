package lol.unsession

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class AiResponse (

  @SerializedName("id"     ) var id      : String?            = null,
  @SerializedName("model"  ) var model   : String?            = null,
  @SerializedName("results") var results : ArrayList<Results> = arrayListOf()

)