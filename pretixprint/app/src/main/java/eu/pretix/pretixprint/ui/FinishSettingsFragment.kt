package eu.pretix.pretixprint.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import eu.pretix.pretixprint.PrintException
import eu.pretix.pretixprint.R
import eu.pretix.pretixprint.byteprotocols.ESCPOS
import eu.pretix.pretixprint.byteprotocols.StarPRNT
import eu.pretix.pretixprint.byteprotocols.ePOSPrintXML
import eu.pretix.pretixprint.byteprotocols.getProtoClass
import eu.pretix.pretixprint.connections.*
import eu.pretix.pretixprint.print.ESCPOSRenderer
import io.sentry.Sentry
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.ctx
import org.jetbrains.anko.support.v4.indeterminateProgressDialog
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class FinishSettingsFragment : SetupFragment() {

    fun writeDemoPage(proto: String, filename: String): File {
        val file = File(ctx.cacheDir, filename)
        if (file.exists()) {
            file.delete()
        }
        val asset = ctx.assets.open(filename)
        val output = FileOutputStream(file)

        val buffer = ByteArray(1024)
        var size = asset.read(buffer)
        while (size != -1) {
            output.write(buffer, 0, size)
            size = asset.read(buffer)
        }
        asset.close()

        if (proto in listOf(ESCPOS().identifier, ePOSPrintXML().identifier, StarPRNT().identifier)) {
            // For ESC/POS, in addition to our static test page explaining printer width, we also
            // print a dynamically generated test page testing features such as text formatting and
            // QR code printing

            val activity = activity as PrinterSetupActivity
            var dialect = ESCPOSRenderer.Companion.Dialect.values().find {
                it.name == activity.settingsStagingArea.get("hardware_${activity.useCase}printer_dialect")
            } ?: ESCPOSRenderer.Companion.Dialect.EpsonDefault

            if (proto == StarPRNT().identifier) {
                dialect = ESCPOSRenderer.Companion.Dialect.StarPRNT
            }

            val testpage = ESCPOSRenderer(dialect, JSONObject(), 32, requireContext()).renderTestPage()
            output.write(testpage)
        }

        output.close()
        return file
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val activity = activity as PrinterSetupActivity
        val view = inflater.inflate(R.layout.fragment_finish_settings, container, false)

        view.findViewById<Button>(R.id.btnTestPage).setOnClickListener {
            val pb = indeterminateProgressDialog(R.string.testing)
            pb.setCancelable(true)
            pb.show()
            doAsync {
                try {
                    testprint()
                    uiThread {
                        if (this@FinishSettingsFragment.activity == null)
                            return@uiThread
                        alert(R.string.test_success).show()
                    }
                } catch (e: PrintException) {
                    Sentry.captureException(e)
                    uiThread {
                        if (this@FinishSettingsFragment.activity == null)
                            return@uiThread
                        alert(e.message).show()
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    Sentry.captureException(e)
                    uiThread {
                        if (this@FinishSettingsFragment.activity == null)
                            return@uiThread
                        alert(e.toString()).show()
                    }
                } finally {
                    uiThread {
                        if (this@FinishSettingsFragment.activity == null)
                            return@uiThread
                        pb.dismiss()
                    }
                }

            }
        }
        view.findViewById<Button>(R.id.btnPrev).setOnClickListener {
            back()
        }
        view.findViewById<Button>(R.id.btnNext).setOnClickListener {
            activity.save()
            activity.finish()
        }

        return view
    }

    override fun back() {
        (activity as PrinterSetupActivity).startProtocolSettings(true)
    }

    fun testprint() {
        val activity = activity as PrinterSetupActivity
        val proto = getProtoClass(activity.proto())

        val file = writeDemoPage(proto.identifier, proto.demopage)

        Sentry.configureScope { scope ->
            scope.setTag("printer.test", "true")
        }

        when (activity.mode()) {
            NetworkConnection().identifier -> {
                NetworkConnection().print(file, 1, activity!!, activity.useCase, activity.settingsStagingArea)
            }
            BluetoothConnection().identifier -> {
                BluetoothConnection().print(file, 1, activity!!, activity.useCase, activity.settingsStagingArea)
            }
            CUPSConnection().identifier -> {
                CUPSConnection().print(file, 1, activity!!, activity.useCase, activity.settingsStagingArea)
            }
            SunmiInternalConnection().identifier -> {
                SunmiInternalConnection().print(file, 1, activity!!, activity.useCase, activity.settingsStagingArea)
            }
            USBConnection().identifier -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    USBConnection().print(file, 1, activity!!, activity.useCase, activity.settingsStagingArea)
                } else {
                    throw Exception("USB not supported on this Android version.")
                }
            }
            SystemConnection().identifier -> {
                SystemConnection().print(file, 1, activity!!, activity.useCase, activity.settingsStagingArea)
            }
        }
    }
}
