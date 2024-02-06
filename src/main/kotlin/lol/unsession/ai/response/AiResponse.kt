package lol.unsession

import com.google.gson.annotations.SerializedName


data class AiResponse (

  @SerializedName("result" ) var aiResult : AiResult? = AiResult()

)