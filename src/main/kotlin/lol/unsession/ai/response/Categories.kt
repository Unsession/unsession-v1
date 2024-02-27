package lol.unsession

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Categories (

  @SerializedName("sexual"                ) var sexual                 : Boolean,
  @SerializedName("hate"                  ) var hate                   : Boolean,
  @SerializedName("harassment"            ) var harassment             : Boolean,
  @SerializedName("self-harm"             ) var selfHarm               : Boolean,
  @SerializedName("sexual/minors"         ) var sexualMinors           : Boolean,
  @SerializedName("hate/threatening"      ) var hateThreatening        : Boolean,
  @SerializedName("violence/graphic"      ) var violenceGraphic        : Boolean,
  @SerializedName("self-harm/intent"      ) var selfHarmIntent         : Boolean,
  @SerializedName("self-harm/instructions") var selfHarmInstructions   : Boolean,
  @SerializedName("harassment/threatening") var harassmentThreatening  : Boolean,
  @SerializedName("violence"              ) var violence               : Boolean

)