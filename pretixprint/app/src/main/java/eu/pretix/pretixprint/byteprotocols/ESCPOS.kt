package eu.pretix.pretixprint.byteprotocols

import eu.pretix.pretixprint.R
import eu.pretix.pretixprint.ui.ESCPOSSettingsFragment
import eu.pretix.pretixprint.ui.SetupFragment
import java8.util.concurrent.CompletableFuture
import java.io.InputStream
import java.io.OutputStream


class ESCPOS : StreamByteProtocol<ByteArray> {
    override val identifier = "ESC/POS"
    override val nameResource = R.string.protocol_escpos
    override val defaultDPI = 200
    override val demopage = "demopage.txt"

    override fun allowedForUsecase(type: String): Boolean {
        return type == "receipt"
    }

    override fun convertPageToBytes(img: ByteArray, isLastPage: Boolean, previousPage: ByteArray?, conf: Map<String, String>, type: String): ByteArray {
        return img
    }

    override fun send(pages: List<CompletableFuture<ByteArray>>, istream: InputStream, ostream: OutputStream, conf: Map<String, String>, type: String) {
        for (f in pages) {
            ostream.write(f.get())
            ostream.flush()

            val wap = Integer.valueOf(conf.get("hardware_${type}printer_waitafterpage") ?: "100")
            Thread.sleep(wap.toLong())
        }
   }

    override fun createSettingsFragment(): SetupFragment? {
        return ESCPOSSettingsFragment()
    }

    override fun inputClass(): Class<ByteArray> {
        return ByteArray::class.java
    }
}