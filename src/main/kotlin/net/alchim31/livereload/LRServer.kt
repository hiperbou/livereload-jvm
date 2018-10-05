package net.alchim31.livereload

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.util.resource.Resource

import java.io.IOException
import java.net.MalformedURLException
import java.nio.file.Path
import java.util.ArrayList
import java.util.regex.Pattern

class LRServer(private val _port: Int, private val _docroot: Path, private val _delay:Int) {
    private var _server: Server? = null
    private var _watcher: Watcher? = null

    @Throws(Exception::class)
    private fun init() {
        val connector = SelectChannelConnector()
        connector.port = _port

        val rHandler = object : ResourceHandler() {
            @Throws(MalformedURLException::class)
            override fun getResource(path: String?): Resource? {
                if ("/livereload.js" == path) {
                    try {
                        return Resource.newResource(LRServer::class.java.getResource(path))
                    } catch (e: IOException) {
                        e.printStackTrace()
                        throw RuntimeException(e)
                    }

                }
                return super.getResource(path)
            }
        }
        rHandler.isDirectoriesListed = true
        rHandler.welcomeFiles = arrayOf("index.html")
        rHandler.resourceBase = _docroot.toString()

        val wsHandler = LRWebSocketHandler()
        wsHandler.handler = rHandler

        _server = Server()
        _server!!.handler = wsHandler
        _server!!.addConnector(connector)

        _watcher = Watcher(_docroot, _delay.toLong())
        if (exclusions != null && exclusions!!.size > 0) {
            val patterns = ArrayList<Pattern>()
            for (exclusion in exclusions!!) {
                patterns.add(Pattern.compile(exclusion))
            }
            _watcher!!._patterns = patterns
        }
        _watcher!!.listener = wsHandler

    }

    @Throws(Exception::class)
    fun start() {
        this.init()
        _server!!.start()
        _watcher!!.start()
    }

    @Throws(Exception::class)
    fun run() {
        try {
            start()
            join()
        } catch (t: Throwable) {
            System.err.println("Caught unexpected exception: " + t)
            System.err.println()
            t.printStackTrace(System.err)
        } finally {
            stop()
        }
    }

    @Throws(Exception::class)
    fun join() {
        _server!!.join()
    }

    @Throws(Exception::class)
    fun stop() {
        _watcher!!.stop()
        _server!!.stop()
    }

    companion object {
        var exclusions: Array<String>? = null
    }
}
