package com.RowdyAvocado

// import android.util.Log

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addSimklId
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.syncproviders.providers.SimklApi.Companion.MediaObject
import com.lagradost.cloudstream3.syncproviders.providers.SimklApi.Companion.SyncServices
import com.lagradost.cloudstream3.syncproviders.providers.SimklApi.Companion.getPosterUrl
import com.lagradost.cloudstream3.utils.*

class Simkl(override val plugin: RowdyPlugin) : Rowdy(plugin) {
    override var name = "Simkl"
    override var mainUrl = "https://simkl.com"
    override var supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "en"
    override val supportedSyncNames = setOf(SyncIdName.Simkl)
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val type = listOf(Type.MEDIA, Type.ANIME)
    override val api = AccountManager.simklApi
    override val syncId = "simkl"
    override val loginRequired = true
    private val auth = BuildConfig.SIMKL_API
    private val limit = 20
    private val apiUrl = "https://api.simkl.com"

    private fun SimklMediaObject.toSearchResponse(): SearchResponse {
        val poster = getPosterUrl(poster ?: "")
        return newMovieSearchResponse(title, "$mainUrl/shows/${ids?.simkl}") {
            this.posterUrl = poster
        }
    }

    private fun SimklMediaObject.toLinkData(): LinkData {
        return LinkData(
                simklId = ids?.simkl,
                imdbId = ids?.imdb,
                tmdbId = ids?.tmdb,
                aniId = ids?.anilist,
                malId = ids?.mal,
                title = title,
                year = year,
                type = type,
                isAnime = type.equals("anime")
        )
    }

    private fun SimklEpisodeObject.toLinkData(
            showName: String,
            ids: SimklIds?,
            year: Int?,
            isAnime: Boolean
    ): LinkData {
        return LinkData(
                simklId = ids?.simkl,
                imdbId = ids?.imdb,
                tmdbId = ids?.tmdb,
                aniId = ids?.anilist,
                malId = ids?.mal,
                title = showName,
                year = year,
                season = season,
                episode = episode,
                type = type,
                isAnime = isAnime
        )
    }

    private fun SimklEpisodeObject.toEpisode(
            showName: String,
            ids: SimklIds?,
            year: Int?,
            isAnime: Boolean
    ): Episode {
        val poster = "https://simkl.in/episodes/${img}_c.webp"
        val linkData = this.toLinkData(showName, ids, year, isAnime).toStringData()
        return Episode(
                data = linkData,
                name = title,
                description = desc,
                posterUrl = poster,
                season = season,
                episode = episode
        )
    }

    // this method is added to tackle current API limitation of 100 req per day
    private fun MediaObject.toSimklMediaObject(): SimklMediaObject? {
        return AppUtils.parseJson<SimklMediaObject>(this.toStringData())
    }

    // this method is added to tackle current API limitation of 100 req per day
    private fun buildSimklEpisodes(total: Int?): Array<SimklEpisodeObject>? {
        if (total == null) return null
        var data = emptyArray<SimklEpisodeObject>()
        (1..total).forEach {
            data += SimklEpisodeObject(season = 1, episode = it, ids = null, type = "episode")
        }
        return data
    }

    override val mainPage =
            mainPageOf(
                    "$apiUrl/tv/trending/month?type=series&client_id=$auth&extended=overview&limit=$limit&page=" to
                            "Trending TV Shows",
                    "$apiUrl/movies/trending/month?client_id=$auth&extended=overview&limit=$limit&page=" to
                            "Trending Movies",
                    "$apiUrl/tv/best/all?type=series&client_id=$auth&extended=overview&limit=$limit&page=" to
                            "Best TV Shows",
                    // "$apiUrl/movies/best/all?client_id=$auth&extended=overview&limit=$limit&page=" to
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
                app.get("$apiUrl/tv/$id?client_id=$auth&extended=full")
                        .parsedSafe<SimklMediaObject>()
                        ?: api.searchByIds(mapOf(SyncServices.Simkl to id))
                                ?.get(0)
                                ?.toSimklMediaObject()
                                ?: throw ErrorLoadingException("Unable to load data")
        val year = data.year
        val posterUrl = getPosterUrl(data.poster ?: "")
        return if (data.type.equals("movie")) {
            val linkData = data.toLinkData().toStringData()
            newMovieLoadResponse(data.title, url, TvType.Movie, linkData) {
                this.addSimklId(id.toInt())
                this.year = year
                this.posterUrl = posterUrl
                this.plot = data.overview
                this.recommendations = data.recommendations?.map { it.toSearchResponse() }
            }
        } else {
            val eps =
                    app.get("$apiUrl/tv/episodes/$id?client_id=$auth&extended=full")
                            .parsedSafe<Array<SimklEpisodeObject>>()
                            ?: buildSimklEpisodes(data.total_episodes)
                                    ?: throw Exception("Unable to fetch episodes")
            val episodes =
                    eps.filter { it.type.equals("episode") }.map {
                        it.toEpisode(data.title, data.ids, year, data.type.equals("anime"))
                    }
            newTvSeriesLoadResponse(data.title, url, TvType.TvSeries, episodes) {
                this.addSimklId(id.toInt())
                this.year = year
                this.posterUrl = posterUrl
                this.plot = data.overview
                this.recommendations = data.recommendations?.map { it.toSearchResponse() }
            }
        }
    }

    open class SimklMediaObject(
            @JsonProperty("title") val title: String,
            @JsonProperty("year") val year: Int? = null,
            @JsonProperty("ids") val ids: SimklIds?,
            @JsonProperty("total_episodes") val total_episodes: Int? = null,
            @JsonProperty("status") val status: String? = null,
            @JsonProperty("poster") val poster: String? = null,
            @JsonProperty("type") val type: String? = null,
            @JsonProperty("overview") val overview: String? = null,
            @JsonProperty("genres") val genres: List<String>? = null,
            @JsonProperty("users_recommendations")
            val recommendations: List<SimklMediaObject>? = null,
    )

    open class SimklEpisodeObject(
            @JsonProperty("title") val title: String? = null,
            @JsonProperty("description") val desc: String? = null,
            @JsonProperty("season") val season: Int? = null,
            @JsonProperty("episode") val episode: Int? = null,
            @JsonProperty("type") val type: String? = null,
            @JsonProperty("aired") val aired: Boolean? = null,
            @JsonProperty("img") val img: String? = null,
            @JsonProperty("ids") val ids: SimklIds?,
    )

    data class SimklIds(
            @JsonProperty("simkl") val simkl: Int? = null,
            @JsonProperty("simkl_id") val simkl2: Int? = null,
            @JsonProperty("imdb") val imdb: String? = null,
            @JsonProperty("tmdb") val tmdb: Int? = null,
            @JsonProperty("mal") val mal: String? = null,
            @JsonProperty("anilist") val anilist: String? = null,
    )
}
