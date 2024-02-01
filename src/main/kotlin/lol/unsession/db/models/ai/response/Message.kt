package lol.unsession

import com.google.gson.annotations.SerializedName


data class Message (

  @SerializedName("role" ) var role : String? = null,
  @SerializedName("text" ) var text : String? = null

)