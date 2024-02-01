package lol.unsession

import com.google.gson.annotations.SerializedName


data class Usage (

  @SerializedName("inputTextTokens"  ) var inputTextTokens  : String? = null,
  @SerializedName("completionTokens" ) var completionTokens : String? = null,
  @SerializedName("totalTokens"      ) var totalTokens      : String? = null

)