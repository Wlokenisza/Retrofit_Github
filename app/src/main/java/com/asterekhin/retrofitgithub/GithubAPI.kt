package com.asterekhin.retrofitgithub

import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Url


interface GithubAPI {
    @GET("user/repos?per_page=100")
    fun getRepos(): Single<List<GithubRepo>>

    @GET("repos/{owner}/{repo}/issues")
    fun getIssues(@Path("owner") owner: String?, @Path("repo") repository: String?): Single<List<GithubIssue>>

    @POST
    fun postComment(@Url url: String, @Body issue: GithubIssue): Single<ResponseBody>

    companion object {
        const val ENDPOINT = "https://api.github.com/"
    }
}