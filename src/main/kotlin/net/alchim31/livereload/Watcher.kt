package net.alchim31.livereload

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger
import java.util.regex.Pattern

import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardWatchEventKinds.*

/**
 * @author dwayne
 * @see http://docs.oracle.com/javase/tutorial/essential/io/notification.html
 */
// TODO make a start/stop/join method
class Watcher @Throws(Exception::class)
constructor(private val _docroot: Path, private val delay:Long) : Runnable {
    private val _watcher: WatchService
    private val _keys: MutableMap<WatchKey, Path>
    private val _running = AtomicBoolean(false)

    var listener: LRWebSocketHandler? = null
    var _patterns: List<Pattern>? = null

    var ignoreUpdates = false

    init {
        this._watcher = _docroot.fileSystem.newWatchService()
        this._keys = HashMap()

        // System.out.format("Scanning %s ...\n", _docroot);
        registerAll(_docroot)
        // System.out.println("Done.");
    }

    @Throws(Exception::class)
    private fun notify(path: String) {
        if(ignoreUpdates) return
        if (_patterns != null) {
            for (p in _patterns!!) {
                LOG.finer("Testing pattern: $p against string: $path")
                if (p.matcher(path).matches()) {
                    LOG.fine("Skipping file: $path thanks to pattern: $p")
                    return
                }
            }
        }
        launch {
          ignoreUpdates = true
          sleep(delay)
          LOG.fine("File $path changed, triggering refresh")
          val l = listener
          l?.notifyChange(path)
          ignoreUpdates = false
        }
    }

    /**
     * Register the given directory with the WatchService
     */
    @Throws(IOException::class)
    private fun register(dir: Path) {
        val key = dir.register(_watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        _keys[key] = dir
    }

    /**
     * Register the given directory, and all its sub-directories, with the WatchService.
     */
    @Throws(IOException::class)
    private fun registerAll(start: Path) {
        // register directory and sub-directories
        Files.walkFileTree(start, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                register(dir)
                return FileVisitResult.CONTINUE
            }
        })
    }

    @Throws(Exception::class)
    internal fun start() {
        if (_running.compareAndSet(false, true)) {
            val t = Thread(this)
            t.isDaemon = true
            t.start()
        }
    }

    @Throws(Exception::class)
    internal fun stop() {
        _running.set(false)
        _watcher.close()
    }

    /**
     * Process all events for keys queued to the watcher
     *
     * @throws Exception
     */
    override fun run() {
        try {
            while (_running.get()) {

                // wait for key to be signalled
                val key = _watcher.take()

                val dir = _keys[key]
                if (dir == null) {
                    System.err.println("WatchKey not recognized!!")
                    continue
                }

                for (event in key.pollEvents()) {
                    val kind = event.kind()

                    // TBD - provide example of how OVERFLOW event is handled
                    if (kind === OVERFLOW) {
                        continue
                    }

                    // Context for directory entry event is the file name of entry
                    val ev = cast<Path>(event)
                    val name = ev.context()
                    val child = dir.resolve(name)

                    // System.out.format("%s: %s ++ %s\n", event.kind().name(), name, _docroot.relativize(child));
                    if (kind === ENTRY_MODIFY) {
                        notify(_docroot.relativize(child).toString())
                    } else if (kind === ENTRY_CREATE) {
                        // if directory is created, and watching recursively, then
                        // register it and its sub-directories
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child)
                        }
                    }
                }

                // reset key and remove from set if directory no longer accessible
                val valid = key.reset()
                if (!valid) {
                    _keys.remove(key)

                    // all directories are inaccessible
                    if (_keys.isEmpty()) {
                        break
                    }
                }
            }
        } catch (exc: InterruptedException) {
            // stop
        } catch (exc: ClosedWatchServiceException) {
        } catch (exc: Exception) {
            exc.printStackTrace()
        } finally {
            _running.set(false)
        }
    }

    companion object {
        private val LOG = Logger.getLogger(Watcher::class.java.name)

        internal fun <T> cast(event: WatchEvent<*>): WatchEvent<T> {
            return event as WatchEvent<T>
        }
    }
}
