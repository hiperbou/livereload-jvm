package net.alchim31.livereload


import java.nio.file.FileSystems

object Main {

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {

        FileSystems.getDefault().use { fs ->

            var path: String? = "."
            var port: String? = "35729"
            var exclusions: Array<String>? = null

            for (i in args.indices) {
              if (hasOption("-h", args, i, false)) {
                  printHelp()
                    return
                }
              if (hasOption("-d", args, i, true)) {
                    path = getOption(args, i)
                }
              if (hasOption("-p", args, i, true)) {
                    port = getOption(args, i)
                }
              if (hasOption("-e", args, i, true)) {
                    val exclusionStr = getOption(args, i)
                    if (exclusionStr != null) {
                        exclusions = exclusionStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    }
                }

            }

            println("Using path: " + path!!)
            println("Using port: " + port!!)
            println("Exclude files matching: " + exclusions)

            val docroot = fs.getPath(path)

            val server = LRServer(Integer.parseInt(port), docroot, 1000)
            LRServer.exclusions = exclusions
            server.run()
        }
    }

    private fun printHelp() {
        println()
        println("Usage: " + Main::class.java.name)
        println()
        println("-h\tPrints this help message")
        println("-d\tSpecify the top level directory to watch for changes")
        println("-p\tSpecify an alternate port from the default Live Reload port")
        println("-e\tA comma separated list of Java regex patterns to exclude from triggering a refresh")
        println()
    }


    private fun hasOption(flag: String, args: Array<String>, i: Int, hasArgument: Boolean): Boolean {
        if (i < args.size && args[i] == flag) {
            if (!hasArgument) {
                return true
            } else if (i + 1 < args.size) {
                return true
            }
        }
        return false
    }

    private fun getOption(args: Array<String>, i: Int): String? {
        return if (i + 1 < args.size) {
            args[i + 1]
        } else null
    }
}
