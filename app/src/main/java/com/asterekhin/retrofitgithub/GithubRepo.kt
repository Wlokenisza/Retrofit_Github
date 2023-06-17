package com.asterekhin.retrofitgithub

class GithubRepo(
    var name: String,
    var owner: String,
    var url: String,
) {
    override fun toString(): String {
        return "$name $url"
    }
}