package io.github.watervxv.mtpadphotos.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 服务器连通性状态（主界面横幅展示用）。
 * 任何一次收到 HTTP 响应即视为可达（无论状态码，Key 无效不代表连不上）；
 * 连接类失败（ConnectException/UnknownHost/超时等 IOException）置为不可达。
 */
class ConnectionMonitor {
    private val _serverUnreachable = MutableStateFlow(false)
    val serverUnreachable: StateFlow<Boolean> = _serverUnreachable.asStateFlow()

    fun onConnected() { _serverUnreachable.value = false }

    fun onUnreachable() { _serverUnreachable.value = true }
}
