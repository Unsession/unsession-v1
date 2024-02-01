package lol.unsession

import com.google.gson.annotations.SerializedName


data class Result (

  @SerializedName("alternatives" ) var alternatives : ArrayList<Alternatives> = arrayListOf(),
  @SerializedName("usage"        ) var usage        : Usage?                  = Usage(),
  @SerializedName("modelVersion" ) var modelVersion : String?                 = null

)