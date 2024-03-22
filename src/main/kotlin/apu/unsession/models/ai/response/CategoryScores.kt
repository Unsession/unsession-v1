package apu.unsession

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryScores (

  @SerializedName("sexual"                ) var sexual                 : Double? = null,
  @SerializedName("hate"                  ) var hate                   : Double? = null,
  @SerializedName("harassment"            ) var harassment             : Double? = null,
  @SerializedName("self-harm"             ) var selfHarm               : Double? = null,
  @SerializedName("sexual/minors"         ) var sexualMinors           : Double? = null,
  @SerializedName("hate/threatening"      ) var hateThreatening        : Double? = null,
  @SerializedName("violence/graphic"      ) var violenceGraphic        : Double? = null,
  @SerializedName("self-harm/intent"      ) var selfHarmIntent         : Double? = null,
  @SerializedName("self-harm/instructions") var selfHarmInstructions   : Double? = null,
  @SerializedName("harassment/threatening") var harassmentThreatening  : Double? = null,
  @SerializedName("violence"              ) var violence               : Double? = null

)