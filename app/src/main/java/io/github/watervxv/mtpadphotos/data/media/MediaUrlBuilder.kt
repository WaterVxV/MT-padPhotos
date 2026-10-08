package io.github.watervxv.mtpadphotos.data.media

import java.net.URLEncoder

class MediaUrlBuilder(private val baseUrl: String) {

    enum class ThumbnailType(val path: String) {
        H220("h220"),
        S260("s260"),
        PREVIEW("preview"),
        POSTER("poster"),
        PROXY("proxy"),
        PORTRAIT("portrait"),
        LIVE_PREVIEW("live_preview")
    }

    private fun base(): String = baseUrl.trimEnd('/')

    private fun authQuery(authCode: String): String =
        "auth_code=${URLEncoder.encode(authCode, "UTF-8")}"

    fun thumbnailUrl(type: ThumbnailType, md5: String, authCode: String): String {
        return "${base()}/gateway/${type.path}/${md5}?${authQuery(authCode)}"
    }

    fun originalUrl(id: Long, md5: String, authCode: String): String {
        return "${base()}/gateway/file/$id/$md5?type=ori&${authQuery(authCode)}"
    }

    fun videoTranscodeUrl(id: Long, md5: String, authCode: String): String {
        return "${base()}/gateway/file/$id/$md5?type=transcode&${authQuery(authCode)}"
    }

    fun flvUrl(id: Long, md5: String, authCode: String): String {
        return "${base()}/gateway/flv/$id/$md5?${authQuery(authCode)}"
    }
}
