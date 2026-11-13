package com.camerasuite.quest.share

import android.content.Context
import android.util.Log
import com.camerasuite.quest.storage.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLEncoder
import kotlin.concurrent.thread

/**
 * Zero-dependency local-network share server.
 *
 * Serves the suite's MediaStore library over plain HTTP on the local Wi-Fi so
 * a PC or phone browser can pull originals — no internet, no account, no cloud.
 * Clearly opt-in: started and stopped by the user, disabled by Local-only mode.
 */
class HttpShareServer(
    private val context: Context,
    private val repo: MediaRepository,
    private val port: Int = 8137
) {
    private var server: ServerSocket? = null
    @Volatile private var running = false

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _shareUrl = MutableStateFlow<String?>(null)
    val shareUrl: StateFlow<String?> = _shareUrl

    fun start(): Boolean {
        if (running) return true
        return try {
            val s = ServerSocket(port)
            server = s
            running = true
            _isRunning.value = true
            val ip = localIpAddress()
            _shareUrl.value = if (ip != null) "http://$ip:$port" else null
            thread(name = "qcs-share", isDaemon = true) { acceptLoop(s) }
            true
        } catch (t: Throwable) {
            Log.e("HttpShare", "start failed", t)
            false
        }
    }

    fun stop() {
        running = false
        runCatching { server?.close() }
        server = null
        _isRunning.value = false
        _shareUrl.value = null
    }

    private fun acceptLoop(s: ServerSocket) {
        while (running) {
            try {
                val client = s.accept()
                thread(name = "qcs-share-client", isDaemon = true) { handle(client) }
            } catch (_: Throwable) {
                if (running) Log.w("HttpShare", "accept failed")
            }
        }
    }

    private fun handle(client: Socket) {
        client.use { sock ->
            sock.soTimeout = 10_000
            val reader = BufferedReader(InputStreamReader(sock.getInputStream()))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(' ')
            if (parts.size < 2 || parts[0] != "GET") return sendText(sock, 405, "Method Not Allowed")

            val path = parts[1].substringBefore('?')
            val out = PrintWriter(sock.getOutputStream(), false)

            when {
                path == "/" || path == "/index.html" -> {
                    val items = repo.items.value
                    val html = buildString {
                        append("<!doctype html><html><head><meta charset='utf-8'>")
                        append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
                        append("<title>Quest Camera Suite — Local Share</title>")
                        append("<style>body{background:#05070d;color:#e8f6ff;font-family:system-ui;padding:24px;max-width:860px;margin:auto}")
                        append("h1{color:#45e8ff;font-size:22px}.row{display:flex;justify-content:space-between;padding:10px 14px;margin:6px 0;")
                        append("background:#0b0f1a;border:1px solid #1d2a44;border-radius:12px}a{color:#45e8ff;text-decoration:none;font-size:15px}")
                        append("span{color:#7f92b8;font-size:12px}</style></head><body>")
                        append("<h1>Quest Camera Suite — Local Share</h1>")
                        append("<p><span>${items.size} files · served on your local network only</span></p>")
                        items.forEachIndexed { i, it ->
                            val mb = String.format("%.1f MB", it.sizeBytes / 1048576.0)
                            append("<div class='row'><a href='/media/$i'>${escape(it.name)}</a><span>$mb</span></div>")
                        }
                        append("</body></html>")
                    }
                    sendText(sock, 200, html, "text/html; charset=utf-8")
                }

                path.startsWith("/media/") -> {
                    val index = path.removePrefix("/media/").toIntOrNull()
                    val item = repo.items.value.getOrNull(index ?: -1)
                    if (item == null) return sendText(sock, 404, "Not found")
                    val mime = if (item.isVideo) "video/mp4" else "image/*"
                    val bytes = runCatching {
                        context.contentResolver.openInputStream(item.uri)?.use { it.readBytes() }
                    }.getOrNull() ?: return sendText(sock, 500, "Read failed")

                    out.print("HTTP/1.1 200 OK\r\n")
                    out.print("Content-Type: $mime\r\n")
                    out.print("Content-Length: ${bytes.size}\r\n")
                    out.print(
                        "Content-Disposition: attachment; filename*=UTF-8''" +
                                URLEncoder.encode(item.name, "UTF-8") + "\r\n"
                    )
                    out.print("Connection: close\r\n\r\n")
                    out.flush()
                    sock.getOutputStream().write(bytes)
                    sock.getOutputStream().flush()
                }

                else -> sendText(sock, 404, "Not found")
            }
        }
    }

    private fun sendText(sock: Socket, code: Int, body: String, contentType: String = "text/plain; charset=utf-8") {
        val bytes = body.toByteArray()
        val out = PrintWriter(sock.getOutputStream(), false)
        val reason = if (code == 200) "OK" else "Error"
        out.print("HTTP/1.1 $code $reason\r\n")
        out.print("Content-Type: $contentType\r\n")
        out.print("Content-Length: ${bytes.size}\r\n")
        out.print("Connection: close\r\n\r\n")
        out.flush()
        sock.getOutputStream().write(bytes)
        sock.getOutputStream().flush()
    }

    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun localIpAddress(): String? {
        return try {
            val ifaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return null
            ifaces.flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
                ?.hostAddress
        } catch (_: Throwable) {
            null
        }
    }
}
