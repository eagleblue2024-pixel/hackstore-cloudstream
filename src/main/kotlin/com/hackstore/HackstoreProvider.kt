package com.hackstore

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * CloudStream 3 Provider for Hackstore (hackstore2.com)
 * Scrapes movies, series, and anime with Spanish audio (Latino / Castellano / Subtitulado).
 * Uses Hackstore's official REST API for high performance and stability.
 */
class HackstoreProvider : MainAPI() {
    override var mainUrl = "https://hackstore2.com"
    override var name = "Hackstore"
    override var lang = "es"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime
    )

    companion object {
        private const val API_PATH = "/api/rest"
        private const val UPLOADS_BASE = "https://hackstore2.com/wp-content/uploads"

        fun getFullImageUrl(url: String?): String? {
            if (url.isNullOrBlank()) return null
            return if (url.startsWith("http")) url else "$UPLOADS_BASE$url"
        }
    }

    override val mainPage = mainPageOf(
        "$API_PATH/sliders?type=movies&listing=0" to "Películas Destacadas",
        "$API_PATH/sliders?type=tvshows&listing=0" to "Series Destacadas",
        "$API_PATH/sliders?type=animes&listing=0" to "Anime Destacado",
        "$API_PATH/listing?post_type=movies&page=1" to "Últimas Películas",
        "$API_PATH/listing?post_type=tvshows&page=1" to "Últimas Series",
        "$API_PATH/listing?post_type=animes&page=1" to "Últimos Animes"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val url = if (page > 1) {
            if (request.data.contains("page=")) {
                request.data.replace(Regex("""page=\d+"""), "page=$page")
            } else {
                "${request.data}&page=$page"
            }
        } else {
            request.data
        }

        val fullUrl = "$mainUrl$url"
        val response = app.get(fullUrl).text

        val items = mutableListOf<SearchResponse>()

        if (url.contains("/sliders")) {
            val json = parseJson<SliderResponse>(response)
            json.data?.forEach { item ->
                items.add(toSearchResponse(item))
            }
        } else {
            val json = parseJson<ListingResponse>(response)
            json.data?.posts?.forEach { item ->
                items.add(toSearchResponse(item))
            }
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = items,
                isHorizontalImages = false
            ),
            hasNext = items.isNotEmpty()
        )
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun search(query: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()
        val types = listOf("movies", "tvshows", "animes")

        for (type in types) {
            try {
                val url = "$mainUrl$API_PATH/search?post_type=$type&query=${query.trim()}"
                val response = app.get(url).text
                val json = parseJson<SearchDataResponse>(response)
                json.data?.posts?.forEach { item ->
                    results.add(toSearchResponse(item, type))
                }
            } catch (_: Exception) {
                // Continue with other categories if one fails
            }
        }

        return results
    }

    private fun toSearchResponse(item: PostItem, forcedType: String? = null): SearchResponse {
        val typeStr = forcedType ?: item.type ?: "movies"
        val poster = getFullImageUrl(item.images?.poster)
        val title = item.title ?: "Sin título"
        val slug = item.slug ?: ""

        val dataUrl = "$mainUrl$API_PATH/single?post_name=$slug&post_type=$typeStr"

        return when (typeStr) {
            "tvshows" -> {
                newTvSeriesSearchResponse(title, dataUrl, TvType.TvSeries) {
                    this.posterUrl = poster
                    this.year = item.releaseDate?.take(4)?.toIntOrNull()
                }
            }
            "animes" -> {
                newAnimeSearchResponse(title, dataUrl, TvType.Anime) {
                    this.posterUrl = poster
                    this.year = item.releaseDate?.take(4)?.toIntOrNull()
                }
            }
            else -> {
                newMovieSearchResponse(title, dataUrl, TvType.Movie) {
                    this.posterUrl = poster
                    this.year = item.releaseDate?.take(4)?.toIntOrNull()
                }
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val response = app.get(url).text
        val json = parseJson<SingleResponse>(response)
        val data = json.data ?: return null

        val title = data.title ?: "Sin título"
        val plot = data.overview
        val poster = getFullImageUrl(data.images?.poster)
        val backdrop = getFullImageUrl(data.images?.backdrop)
        val year = data.releaseDate?.take(4)?.toIntOrNull()
        val rating = data.rating?.toRatingInt()
        val type = data.type ?: "movies"
        val postId = data.id ?: return null

        if (type == "tvshows" || type == "animes") {
            val epUrl = "$mainUrl$API_PATH/episodes?post_id=$postId"
            val epResponse = app.get(epUrl).text
            val epJson = parseJson<EpisodeListResponse>(epResponse)

            val episodes = epJson.data?.mapNotNull { ep ->
                val epId = ep.id ?: return@mapNotNull null
                val epTitle = ep.title ?: "Episodio ${ep.episodeNumber}"
                val epLink = "$mainUrl$API_PATH/player?post_id=$epId"
                newEpisode(epLink) {
                    this.name = epTitle
                    this.season = ep.seasonNumber
                    this.episode = ep.episodeNumber
                    this.posterUrl = if (!ep.stillPath.isNullOrBlank()) {
                        "https://image.tmdb.org/t/p/w500${ep.stillPath}"
                    } else {
                        poster
                    }
                }
            } ?: emptyList()

            return newTvSeriesLoadResponse(
                title,
                url,
                if (type == "animes") TvType.Anime else TvType.TvSeries,
                episodes
            ) {
                this.posterUrl = poster
                this.backgroundPosterUrl = backdrop
                this.plot = plot
                this.year = year
                this.rating = rating
                addTrailer(data.trailer)
            }
        } else {
            val loadData = "$mainUrl$API_PATH/player?post_id=$postId"
            return newMovieLoadResponse(title, url, TvType.Movie, loadData) {
                this.posterUrl = poster
                this.backgroundPosterUrl = backdrop
                this.plot = plot
                this.year = year
                this.rating = rating
                addTrailer(data.trailer)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val postId = Regex("""post_id=(\d+)""").find(data)?.groupValues?.get(1) ?: return false

        // 1. Cargar reproductores de vídeo embebidos (Vimeos, Streamwish, Filemoon, etc.)
        try {
            val playerUrl = "$mainUrl$API_PATH/player?post_id=$postId"
            val playerRes = app.get(playerUrl).text
            val playerJson = parseJson<PlayerResponse>(playerRes)

            playerJson.data?.forEach { player ->
                val embedUrl = player.url ?: return@forEach
                val lang = player.lang ?: "Latino"
                val quality = player.quality ?: "1080p"

                loadExtractor(embedUrl, subtitleCallback) { link ->
                    callback.invoke(
                        ExtractorLink(
                            source = "$name (${link.name})",
                            name = "$name - $lang [${link.name} $quality]",
                            url = link.url,
                            referer = link.referer,
                            quality = link.quality,
                            type = link.type,
                            headers = link.headers
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Continúa si falla un extractor
        }

        // 2. Cargar servidores de descarga compatibles (Mega, Mediafire, etc.)
        try {
            val downloadsUrl = "$mainUrl$API_PATH/downloads?post_id=$postId"
            val dlRes = app.get(downloadsUrl).text
            val dlJson = parseJson<DownloadsResponse>(dlRes)

            dlJson.data?.forEach { dl ->
                val dlUrl = dl.url ?: return@forEach
                val server = dl.server ?: "Directo"
                val lang = dl.lang ?: "Audio"
                val quality = dl.quality ?: "HD"

                if (!dlUrl.startsWith("magnet:")) {
                    loadExtractor(dlUrl, subtitleCallback) { link ->
                        callback.invoke(
                            ExtractorLink(
                                source = "$name ($server)",
                                name = "$name - $server $quality ($lang)",
                                url = link.url,
                                referer = link.referer,
                                quality = link.quality,
                                type = link.type
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Continúa
        }

        return true
    }

    // Modelos de datos JSON correspondientes a la API REST de Hackstore2
    data class SliderResponse(
        @JsonProperty("data") val data: List<PostItem>? = null
    )

    data class ListingResponse(
        @JsonProperty("data") val data: ListingData? = null
    )

    data class ListingData(
        @JsonProperty("posts") val posts: List<PostItem>? = null
    )

    data class SearchDataResponse(
        @JsonProperty("data") val data: ListingData? = null
    )

    data class SingleResponse(
        @JsonProperty("data") val data: PostItem? = null
    )

    data class EpisodeListResponse(
        @JsonProperty("data") val data: List<EpisodeItem>? = null
    )

    data class PlayerResponse(
        @JsonProperty("data") val data: List<PlayerItem>? = null
    )

    data class DownloadsResponse(
        @JsonProperty("data") val data: List<DownloadItem>? = null
    )

    data class PostItem(
        @JsonProperty("_id") val id: Long? = null,
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("overview") val overview: String? = null,
        @JsonProperty("slug") val slug: String? = null,
        @JsonProperty("images") val images: ImagesItem? = null,
        @JsonProperty("rating") val rating: String? = null,
        @JsonProperty("type") val type: String? = null,
        @JsonProperty("release_date") val releaseDate: String? = null,
        @JsonProperty("trailer") val trailer: String? = null,
        @JsonProperty("runtime") val runtime: String? = null
    )

    data class ImagesItem(
        @JsonProperty("poster") val poster: String? = null,
        @JsonProperty("backdrop") val backdrop: String? = null
    )

    data class EpisodeItem(
        @JsonProperty("_id") val id: Long? = null,
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("season_number") val seasonNumber: Int? = null,
        @JsonProperty("episode_number") val episodeNumber: Int? = null,
        @JsonProperty("still_path") val stillPath: String? = null
    )

    data class PlayerItem(
        @JsonProperty("lang") val lang: String? = null,
        @JsonProperty("quality") val quality: String? = null,
        @JsonProperty("url") val url: String? = null
    )

    data class DownloadItem(
        @JsonProperty("server") val server: String? = null,
        @JsonProperty("lang") val lang: String? = null,
        @JsonProperty("quality") val quality: String? = null,
        @JsonProperty("url") val url: String? = null,
        @JsonProperty("size") val size: String? = null
    )
}
