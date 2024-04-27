package com.RowdyAvocado

// import android.util.Log

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.syncproviders.providers.SimklApi.Companion.getPosterUrl
import com.lagradost.cloudstream3.utils.*

class Simkl(override val plugin: RowdyPlugin) : MainAPI2(plugin) {
    override var name = "Simkl"
    override var mainUrl = "https://simkl.com"
    override var supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "en"
    override val supportedSyncNames = setOf(SyncIdName.Simkl)
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val type = Type.MEDIA
    override val api = AccountManager.simklApi
    override val syncId = "simkl"
    override val loginRequired = true
    private val clientId = "336bff735f15ad4045c6110f94a989a6d021174141126ed14bb24f5b4241dd58"
    private val limit = 20
    private val apiUrl = "https://api.simkl.com"

    private fun SimklMediaObject.toSearchResponse(): SearchResponse {
        val poster = getPosterUrl(poster ?: "")
        return newMovieSearchResponse(title ?: "", "$mainUrl/shows/${ids?.simkl}") {
            this.posterUrl = poster
        }
    }

    private fun SimklEpisodeObject.toEpisode(showName: String, year: Int?): Episode {
        val data = this
        val poster = "https://simkl.in/episodes/${img}_c.webp"
        val epData = EpisodeData(showName, year, season, episode).toStringData()
        return newEpisode(epData) {
            this.name = title
            this.description = desc
            this.season = data.season
            this.episode = data.episode
            this.posterUrl = poster
        }
    }

    override val mainPage =
            mainPageOf(
                    "$apiUrl/tv/trending/month?type=series&client_id=$clientId&extended=overview&limit=$limit&page=" to
                            "Trending TV Shows",
                    "$apiUrl/movies/trending/month?client_id=$clientId&extended=overview&limit=$limit&page=" to
                            "Trending Movies",
                    "$apiUrl/tv/best/all?type=series&client_id=$clientId&extended=overview&limit=$limit&page=" to
                            "Best TV Shows",
                    // "$apiUrl/movies/best/all?client_id=$clientId&extended=overview&limit=$limit&page=" to
                    //         "Best Movies",
                    "Personal" to "Personal"
            )

    override suspend fun MainPageRequest.toSearchResponseList(
            page: Int
    ): Pair<List<SearchResponse>, Boolean> {
        val emptyData = emptyList<SearchResponse>() to false
        val res =
                app.get(this.data + page).parsedSafe<Array<SimklMediaObject>>() ?: return emptyData
        return res.map {
            newMovieSearchResponse("${it.title}", "$mainUrl/shows/${it.ids?.simkl2}") {
                this.posterUrl = getPosterUrl(it.poster.toString())
            }
        } to res.size.equals(limit)
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.removeSuffix("/").substringAfterLast("/")
        val data =
                app.get("$apiUrl/tv/$id?client_id=$clientId&extended=full")
                        .parsedSafe<SimklMediaObject>()
                        ?: throw ErrorLoadingException("Unable to load data")
        val title = data.title ?: throw ErrorLoadingException("Unable to find title")
        val year = data.year
        val posterUrl = getPosterUrl(data.poster ?: "")
        return if (data.type.equals("movie")) {
            val dataUrl = EpisodeData(title, year, null, null).toStringData()
            newMovieLoadResponse(title, url, TvType.Movie, dataUrl) {
                this.syncData = mutableMapOf(syncId to id)
                this.year = year
                this.posterUrl = posterUrl
                this.plot = data.overview
                this.recommendations = data.recommendations?.map { it.toSearchResponse() }
            }
        } else {
            val test =
                    app.get("$apiUrl/tv/episodes/$id?client_id=$clientId&extended=full")
                            .parsedSafe<Array<SimklEpisodeObject>>()
                            ?: throw Exception("Unable to fetch episodes")
            val episodes =
                    test.filter { it.type.equals("episode") }.map { it.toEpisode(title, year) }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.syncData = mutableMapOf(syncId to id)
                this.year = year
                this.posterUrl = posterUrl
                this.plot = data.overview
                this.recommendations = data.recommendations?.map { it.toSearchResponse() }
            }
        }
    }
}

open class SimklMediaObject(
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("year") val year: Int? = null,
        @JsonProperty("ids") val ids: Ids?,
        @JsonProperty("total_episodes") val total_episodes: Int? = null,
        @JsonProperty("status") val status: String? = null,
        @JsonProperty("poster") val poster: String? = null,
        @JsonProperty("type") val type: String? = null,
        @JsonProperty("overview") val overview: String? = null,
        @JsonProperty("genres") val genres: List<String>? = null,
        @JsonProperty("users_recommendations") val recommendations: List<SimklMediaObject>? = null,
)

open class SimklEpisodeObject(
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("description") val desc: String? = null,
        @JsonProperty("season") val season: Int? = null,
        @JsonProperty("episode") val episode: Int? = null,
        @JsonProperty("type") val type: String? = null,
        @JsonProperty("aired") val aired: Boolean? = null,
        @JsonProperty("img") val img: String? = null,
        @JsonProperty("ids") val ids: Ids?,
)
