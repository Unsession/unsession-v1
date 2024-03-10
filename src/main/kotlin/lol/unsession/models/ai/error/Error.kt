package lol.unsession

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Error (

  @SerializedName("message" ) var message : String? = null,
  @SerializedName("type"    ) var type    : String? = null,
  @SerializedName("param"   ) var param   : String? = null,
  @SerializedName("code"    ) var code    : String? = null

)