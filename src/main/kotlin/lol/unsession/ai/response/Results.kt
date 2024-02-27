package lol.unsession

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Results (

  @SerializedName("flagged"        ) var flagged        : Boolean?        = null,
  @SerializedName("categories"     ) var categories     : Categories,
  @SerializedName("category_scores") var categoryScores : CategoryScores? = CategoryScores()

)