package lol.unsession

import com.google.gson.annotations.SerializedName


data class Alternatives (

  @SerializedName("message" ) var message : Message? = Message(),
  @SerializedName("status"  ) var status  : String?  = null

)