package com.asterekhin.retrofitgithub

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type


class GithubRepoDeserializer : JsonDeserializer<GithubRepo> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): GithubRepo {
        val repoJsonObject = json.asJsonObject
        val name = repoJsonObject["name"].asString
        val url = repoJsonObject["url"].asString
        val ownerJsonElement = repoJsonObject["owner"]
        val ownerJsonObject = ownerJsonElement.asJsonObject
        val owner = ownerJsonObject["login"].asString
        return GithubRepo(name, owner, url)
    }
}