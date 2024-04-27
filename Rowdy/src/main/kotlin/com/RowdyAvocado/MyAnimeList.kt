package com.RowdyAvocado

// import android.util.Log

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.syncproviders.providers.MALApi
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class MyAnimeList(override val plugin: RowdyPlugin) : MainAPI2(plugin) {
    override var name = "MyAnimeList"
    override var mainUrl = "https://myanimelist.net"
    override var supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)
    override var lang = "en"
    override val supportedSyncNames = setOf(SyncIdName.Anilist)
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val api: SyncAPI = MALApi(1)
    override val type = Type.ANIME
    override val syncId = "MAL"
    override val loginRequired = true
    private final val mediaLimit = 50
    private val apiUrl = "https://api.myanimelist.net/v2"

    override val mainPage =
            mainPageOf(
                    "$mainUrl/topanime.php?type=airing&limit=" to "Top Airing",
                    "$mainUrl/topanime.php?type=bypopularity&limit=" to "Most Popular",
                    "$mainUrl/topanime.php?type=favorite&limit=" to "Top Favorites",
                    "Personal" to "Personal"
            )

    private fun Element.toSearchResponse(): SearchResponse {
        val data = this.select("div.detail a.hoverinfo_trigger")
        val name = data.text()
        val url = data.attr("href").substringBeforeLast("/")
        val posterUrl = this.select("img").attr("data-src")
        return newAnimeSearchResponse(name, url, TvType.Anime) { this.posterUrl = posterUrl }
    }

    override suspend fun MainPageRequest.toSearchResponseList(
            page: Int
    ): Pair<List<SearchResponse>, Boolean> {
        val url = this.data + page.minus(1).times(mediaLimit)
        val res = app.get(url).document
        val data = res.select("tr.ranking-list").map { it.toSearchResponse() }
        return data to true
    }
}
