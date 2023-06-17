package com.asterekhin.retrofitgithub

import com.google.gson.annotations.SerializedName


data class GithubIssue(
    var id: String,
    var title: String,
    var comments_url: String,
    @SerializedName("body") var comment: String,
) {

    override fun toString(): String {
        return "$id - $title"
    }
}