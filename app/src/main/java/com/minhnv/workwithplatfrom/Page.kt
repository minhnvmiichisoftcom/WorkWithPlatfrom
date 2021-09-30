package com.minhnv.workwithplatfrom

import com.google.gson.annotations.SerializedName
import java.math.BigInteger

data class PageData(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("id")
    val id: BigInteger
)

data class Data(
    @SerializedName("data")
    val data: PageData
)