package eu.pretix.pretixprint.ui

import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.zebra.sdk.common.card.enumerations.CardDestination
import com.zebra.sdk.common.card.enumerations.CardSource
import eu.pretix.pretixprint.R
import eu.pretix.pretixprint.byteprotocols.LinkOSCard
import org.jetbrains.anko.support.v4.defaultSharedPreferences


class LinkOSCardSettingsFragment : SetupFragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_linkoscard_settings, container, false)
        val proto = LinkOSCard()

        val currentDoubleSided = ((activity as PrinterSetupActivity).settingsStagingArea.get(
                "hardware_${useCase}printer_doublesided"
        )?.toBoolean() ) ?: defaultSharedPreferences.getString("hardware_${useCase}printer_doublesided", "false")!!.toBoolean()
        view.findViewById<SwitchMaterial>(R.id.swDoubleSided).isChecked = currentDoubleSided

        val currentCardSource = ((activity as PrinterSetupActivity).settingsStagingArea.get(
                "hardware_${useCase}printer_cardsource"
        ) as String?) ?: defaultSharedPreferences.getString("hardware_${useCase}printer_cardsource", "AutoDetect")
        view.findViewById<TextInputLayout>(R.id.tilCardSource).editText?.setText(currentCardSource)

        val currentCardDestination = ((activity as PrinterSetupActivity).settingsStagingArea.get(
                "hardware_${useCase}printer_carddestination"
        ) as String?) ?: defaultSharedPreferences.getString("hardware_${useCase}printer_carddestination", "Eject")
        view.findViewById<TextInputLayout>(R.id.tilCardDestination).editText?.setText(currentCardDestination)

        val currentDPI = ((activity as PrinterSetupActivity).settingsStagingArea.get(
                "hardware_${useCase}printer_dpi"
        ) as String?) ?: defaultSharedPreferences.getString("hardware_${useCase}printer_dpi", proto.defaultDPI.toString())
        view.findViewById<TextInputEditText>(R.id.teDPI).setText(currentDPI)


        val cardSourcesAdapter = ArrayAdapter(requireContext(), R.layout.list_item, CardSource.values())
        (view.findViewById<TextInputLayout>(R.id.tilCardSource).editText as? AutoCompleteTextView)?.setAdapter(cardSourcesAdapter)

        val cardDestinationAdapter = ArrayAdapter(requireContext(), R.layout.list_item, CardDestination.values())
        (view.findViewById<TextInputLayout>(R.id.tilCardDestination).editText as? AutoCompleteTextView)?.setAdapter(cardDestinationAdapter)

        view.findViewById<Button>(R.id.btnPrev).setOnClickListener {
            back()
        }
        view.findViewById<Button>(R.id.btnNext).setOnClickListener {
            val dpi = view.findViewById<TextInputEditText>(R.id.teDPI).text.toString()
            if (TextUtils.isEmpty(dpi)) {
                view.findViewById<TextInputEditText>(R.id.teDPI).error = getString(R.string.err_field_required)
            } else if (!TextUtils.isDigitsOnly(dpi)) {
                view.findViewById<TextInputEditText>(R.id.teDPI).error = getString(R.string.err_field_invalid)
            } else {
                view.findViewById<TextInputEditText>(R.id.teDPI).error = null

                val doubleSided = view.findViewById<SwitchMaterial>(R.id.swDoubleSided).isChecked
                val cardSource = view.findViewById<TextInputLayout>(R.id.tilCardSource).editText?.text.toString()
                val cardDestination = view.findViewById<TextInputLayout>(R.id.tilCardDestination).editText?.text.toString()

                (activity as PrinterSetupActivity).settingsStagingArea.put("hardware_${useCase}printer_dpi", dpi)
                (activity as PrinterSetupActivity).settingsStagingArea.put("hardware_${useCase}printer_doublesided", doubleSided.toString())
                (activity as PrinterSetupActivity).settingsStagingArea.put("hardware_${useCase}printer_cardsource", cardSource)
                (activity as PrinterSetupActivity).settingsStagingArea.put("hardware_${useCase}printer_carddestination", cardDestination)
                (activity as PrinterSetupActivity).startFinalPage()
            }
        }

        return view
    }

    override fun back() {
        (activity as PrinterSetupActivity).startProtocolChoice()
    }
}