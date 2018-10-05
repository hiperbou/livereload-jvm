package net.alchim31.livereload

import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue

import javax.servlet.http.HttpServletRequest

import org.eclipse.jetty.websocket.WebSocket
import org.eclipse.jetty.websocket.WebSocketHandler

class LRWebSocketHandler : WebSocketHandler() {
    val _broadcast = ConcurrentLinkedQueue<LRWebSocket>()
    val _protocol = LRProtocol()

    override fun doWebSocketConnect(request: HttpServletRequest, protocol: String?): WebSocket {
        return if ("/livereload" == request.pathInfo) {
            LRWebSocket()
        } else object : WebSocket {
            override fun onOpen(connection: WebSocket.Connection) {
                connection.close(-1, "")
            }

            override fun onClose(code: Int, msg: String?) {}
        }
    }

    @Throws(Exception::class)
    fun notifyChange(path: String) {
        val msg = _protocol.reload(path)
        for (ws in _broadcast) {
            try {
                ws._connection.sendMessage(msg)
            } catch (e: IOException) {
                _broadcast.remove(ws)
                e.printStackTrace()
            }

        }
    }

    inner class LRWebSocket : WebSocket.OnTextMessage {
        lateinit var _connection: WebSocket.Connection

        override fun onOpen(connection: WebSocket.Connection) {
            _connection = connection
            _broadcast.add(this)
        }

        override fun onClose(code: Int, message: String?) {
            _broadcast.remove(this)
        }

        override fun onMessage(data: String?) {
            try {
                if (_protocol.isHello(data)) {
                    _connection.sendMessage(_protocol.hello())
                }
            } catch (exc: Exception) {
                exc.printStackTrace()
            }

        }
    }
}
