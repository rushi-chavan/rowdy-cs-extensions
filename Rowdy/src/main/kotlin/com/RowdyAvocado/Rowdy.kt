package com.RowdyAvocado

// import android.util.Log

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.utils.*

abstract class Rowdy(open val plugin: RowdyPlugin) : MainAPI() {
    open override var lang = "en"
    open override val hasMainPage = true
    override val instantLinkLoading = true
    abstract val api: SyncAPI
    abstract val type: List<Type>
    abstract val syncId: String
    abstract val loginRequired: Boolean

    protected fun Any.toStringData(): String {
        return mapper.writeValueAsString(this)
    }

    open override val mainPage = mainPageOf("Personal" to "Personal")

    // This needs to be implemented by every Service provider,
    // if they are introducing external main page section other than Personal
    open suspend fun MainPageRequest.toSearchResponseList(
            page: Int
    ): Pair<List<SearchResponse>, Boolean> {
        return emptyList<SearchResponse>() to false
    }

    // This method can be overridden in child service to modify search
    override open suspend fun search(query: String): List<SearchResponse>? {
        return api.search(query)
    }

    open override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (request.name.equals("Personal")) {
            // Reading and manipulating personal library
            api.loginInfo()
                    ?: return newHomePageResponse(
                            "Login required for personal content.",
                            emptyList<SearchResponse>(),
                            false
                    )
            var homePageList =
                    api.getPersonalLibrary()?.allLibraryLists?.mapNotNull {
                        if (it.items.isEmpty()) return@mapNotNull null
                        val libraryName =
                                it.name.asString(plugin.activity ?: return@mapNotNull null)
                        HomePageList("${request.name}: $libraryName", it.items)
                    }
                            ?: return null
            return newHomePageResponse(homePageList, false)
        } else {
            // Other new sections will be generated if toSearchResponseList() is overridden
            val data = request.toSearchResponseList(page)
            return newHomePageResponse(request.name, data.first, data.second)
        }
    }

    // This load() only supports animes and for movies you can override this function
    open override suspend fun load(url: String): LoadResponse {
        val id = url.removeSuffix("/").substringAfterLast("/")
        val data = api.getResult(id) ?: throw ErrorLoadingException("Unable to fetch show details")
        var year = data.startDate?.div(1000)?.div(86400)?.div(365)?.plus(1970)?.toInt()
        val epCount = data.nextAiring?.episode?.minus(1) ?: data.totalEpisodes ?: 0
        val episodes =
                (1..epCount).map { i ->
                    val linkData =
                            LinkData(
                                            title = data.title,
                                            year = year,
                                            season = 1,
                                            episode = i,
                                            isAnime = true
                                    )
                                    .toStringData()
                    Episode(linkData, season = 1, episode = i)
                }
        return newAnimeLoadResponse(data.title ?: "", url, TvType.Anime) {
            this.syncData = mutableMapOf(syncId to id)
            addEpisodes(DubStatus.Subbed, episodes)
            this.recommendations = data.recommendations
        }
    }

    open override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val mediaData = AppUtils.parseJson<LinkData>(data)
        type
                .filter {
                    (mediaData.isAnime && it == Type.ANIME) ||
                            (!mediaData.isAnime && it == Type.MEDIA)
                }
                .amap { t ->
                    RowdyExtractor(t, plugin).getUrl(data, null, subtitleCallback, callback)
                }
        return true
    }
}
