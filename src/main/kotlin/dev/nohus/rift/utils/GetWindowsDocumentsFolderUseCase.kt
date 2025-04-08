package dev.nohus.rift.utils

import com.sun.jna.platform.win32.Shell32Util
import com.sun.jna.platform.win32.ShlObj
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class GetWindowsDocumentsFolderUseCase {

    operator fun invoke(): String? {
        return try {
            Shell32Util.getFolderPath(ShlObj.CSIDL_MYDOCUMENTS)
        } catch (e: RuntimeException) {
            logger.error { "Failed checking Windows documents folder location: ${e.message}" }
            null
        }
    }
}
