/*
   Copyright © 2017-2023  Kynetics  LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.kynetics.android_i2c_example_app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.WindowCompat
import com.kynetics.android_i2c_example_app.databinding.ActivityMainBinding
import io.helins.linux.i2c.I2CBuffer
import io.helins.linux.i2c.I2CBus
import io.helins.linux.i2c.I2CFunctionality
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var i2c: I2CBus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        showBusSelectDialog()
    }

    private fun setupToolbar() {

        setSupportActionBar(binding.toolbar)

        binding.toolbar.navigationIcon = AppCompatResources.getDrawable(this,
            androidx.appcompat.R.drawable.abc_ic_ab_back_material)

        binding.toolbar.setNavigationOnClickListener {
            clearViews()
            showBusSelectDialog()
        }
    }

    private fun clearViews() {
        binding.main.textViewReadResult.text = ""
        binding.main.textViewWriteResult.text = ""
        binding.main.textViewFuncList.text = ""
        binding.main.textViewBus.text = ""
    }

    @SuppressLint("SetTextI18n")
    private fun showBusSelectDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog, null, false)

        val busDevices = findBuses()?.sorted()

        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        if (busDevices.isNullOrEmpty()) {
            val errorTv = dialogView.findViewById<TextView>(R.id.dialog_error)
            dialogView.findViewById<View>(R.id.dialog_layout).visibility = View.GONE
            errorTv.visibility = View.VISIBLE
            errorTv.text = "Could not find any i2c bus on the device!!!"

            dialogBuilder.apply {
                setCancelable(true)
                setNegativeButton("Close") { _, _ ->
                    finish()
                }

                setOnCancelListener {
                    finish()
                }
            }
        } else {

            val dropdown = dialogView.findViewById<Spinner>(R.id.dialog_spinner)
            val adapter = DeviceListAdapter(this, busDevices.toMutableList())
            dropdown.adapter = adapter

            dialogBuilder.setPositiveButton("OK") { _, _ ->
                val busName = dropdown.selectedItem.toString()
                val busPath = "/dev/$busName"
                try {
                    i2c = I2CBus(busPath)
                    binding.main.textViewBus.text = busPath
                    loadViews()
                } catch (e: IOException) {
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        dialogBuilder.create().show()
    }

    private fun loadViews() {
        loadFunctionalities()
        loadReadingFunctionality()
        loadWritingFunctionality()
    }

    private fun loadReadingFunctionality() {
        binding.main.buttonRead.setOnClickListener {
            val slaveET = binding.main.editTextReadChipAddress
            val addressET = binding.main.editTextReadAddress


            when {
                slaveET.text.toString().isBlank() -> {
                    slaveET.error = "Chip address can not be empty!"
                }
                addressET.text.toString().isBlank() -> {
                    addressET.error = "Address to read can not be empty!"
                }
                else -> {
                    val slave: Int
                    val readAddress: Int
                    try {
                        slave = Integer.decode(slaveET.text.toString())
                        readAddress = Integer.decode(addressET.text.toString())
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Failed to decode the input", Toast.LENGTH_SHORT)
                            .show()
                        return@setOnClickListener
                    }

                    try {
                        i2c?.selectSlave(slave)

                        val buffer = I2CBuffer(1).set(0, readAddress)

                        //Selecting the address to read
                        i2c?.write(buffer)
                        i2c?.read(buffer)

                        val result = """
                                    Read Result:
                                    Chip address: 0x${slave.toHexString()}
                                    Read address: 0x${readAddress.toHexString()}
                                    Value : 0x${buffer.toHexString()}
                                """.trimIndent()
                        binding.main.textViewReadResult.text = result

                    } catch (e: IOException) {
                        Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }

        }
    }

    private fun loadWritingFunctionality() {

        binding.main.buttonWrite.setOnClickListener {

            val slaveET = binding.main.editTextWriteChipAddress
            val addressET = binding.main.editTextWriteAddress
            val newValueET = binding.main.editTextWriteValue

            when {
                slaveET.text.toString().isBlank() -> {
                    slaveET.error = "Chip address can not be empty!"
                }
                addressET.text.toString().isBlank() -> {
                    addressET.error = "Address to read can not be empty!"
                }
                newValueET.text.toString().isBlank() -> {
                    newValueET.error = "Write value can not be empty!"
                }
                else -> {
                    val slave: Int
                    val writeAddress: Int
                    val writeValue: Int
                    try {
                        slave = Integer.decode(slaveET.text.toString())
                        writeAddress = Integer.decode(addressET.text.toString())
                        writeValue = Integer.decode(newValueET.text.toString())
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Failed to decode the input", Toast.LENGTH_SHORT)
                            .show()
                        return@setOnClickListener
                    }
                    binding.main.textViewWriteResult.text = ""
                    try {
                        i2c?.selectSlave(slave)

                        val buffer =
                            I2CBuffer(2).set(0, writeAddress).set(1, writeValue)

                        i2c?.write(buffer)

                        val readBuffer = I2CBuffer(1).set(0, writeAddress)

                        //Waits between IO operations so the device has time for printing to serial what is going on.
                        Thread.sleep(100)

                        //Selecting the address to read
                        i2c?.write(readBuffer)
                        i2c?.read(readBuffer)

                        val result = """
                                    Write Result:
                                    Chip address: 0x${slave.toHexString()}
                                    Write address: 0x${writeAddress.toHexString()}
                                    Current Value : 0x${readBuffer.toHexString()}
                                """.trimIndent()
                        binding.main.textViewWriteResult.text = result
                    } catch (e: IOException) {
                        Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun loadFunctionalities() {
        val list = availableFunctionalities()
        var text = ""
        list.forEach { func ->
            text += "\n $func"
        }
        Log.v(TAG, text)
        binding.main.textViewFuncList.text = text
    }


    private fun findBuses(): List<String>? {
        /* Read all available i2c bus names */
        val i2cBuses: List<String>?
        try {
            i2cBuses = File("/dev").list { _, name ->
                name.startsWith("i2c")
            }?.toList()
            Log.d(TAG, "Found i2c buses: $i2cBuses")
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        return i2cBuses
    }

    private fun availableFunctionalities(): List<String> {
        val available = mutableListOf<String>()
        i2c?.let {
            for (functionality in I2CFunctionality.values()) {
                if (it.functionalities.can(functionality))
                    available.add(functionality.toString())
            }
        }
        return available
    }

    override fun onBackPressed() {
        clearViews()
        showBusSelectDialog()
    }

    class DeviceListAdapter(context: Context, private val list: MutableList<String>) :
        ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, list) {

        override fun getDropDownView(position: Int, convertView: View?,
                                     parent: ViewGroup): View {
            val dropDownView = super.getDropDownView(position, convertView, parent) as TextView
            if (isEnabled(position)) {
                dropDownView.setTextColor(AppCompatResources.getColorStateList(
                    dropDownView.context,
                    androidx.appcompat.R.color.primary_text_default_material_light))
            } else {
                dropDownView.setTextColor(AppCompatResources.getColorStateList(
                    dropDownView.context,
                    androidx.appcompat.R.color.primary_text_disabled_material_light))
            }
            return dropDownView
        }

        override fun isEnabled(position: Int): Boolean {
            val item = File("/dev", list[position])
            return item.canRead() && item.canWrite()
        }
    }

    companion object {
        const val TAG: String = "KyneticsI2CExampleApplication:MainActivity"
    }
}