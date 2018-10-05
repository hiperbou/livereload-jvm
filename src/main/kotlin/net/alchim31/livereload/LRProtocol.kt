package net.alchim31.livereload

import java.util.LinkedHashMap
import java.util.LinkedList

import org.json.simple.JSONValue

/**
 * @see http://feedback.livereload.com/knowledgebase/articles/86174-livereload-protocol
 *
 * @author dwayne
 */
class LRProtocol {

    fun hello(): String {
        val protocols = LinkedList<String>()
        protocols.add("http://livereload.com/protocols/official-7")

        val obj = LinkedHashMap<String, Any>()
        obj["command"] = "hello"
        obj["protocols"] = protocols
        obj["serverName"] = "livereload-jvm"
        return JSONValue.toJSONString(obj)
    }

    @Throws(Exception::class)
    fun alert(msg: String): String {
        val obj = LinkedHashMap<String, Any>()
        obj["command"] = "alert"
        obj["message"] = msg
        return JSONValue.toJSONString(obj)
    }

    @Throws(Exception::class)
    fun reload(path: String): String {
        val obj = LinkedHashMap<String, Any>()
        obj["command"] = "reload"
        obj["path"] = path
        obj["liveCSS"] = true
        return JSONValue.toJSONString(obj)
    }

    @Throws(Exception::class)
    fun isHello(data: String?): Boolean {
        val obj = JSONValue.parse(data)
        var back = obj is Map<*, *>
        back = back && "hello" == (obj as Map<Any, Any>)["command"]
        return back
    }

}
