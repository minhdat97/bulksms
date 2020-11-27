package com.example.bulksms

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.opencsv.CSVReader
import kotlinx.android.synthetic.main.activity_main.*
import org.apache.commons.io.FileUtils
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.charset.Charset
import java.util.*


class MainActivity : AppCompatActivity() {
    private val asciiEncoder = Charset.forName("US-ASCII").newEncoder()
    private val context: Context = this
    private var adapter: ArrayAdapter<String>? = null
    private val permissionRequest = 101
    private var dataFile: Vector<Array<String>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1)
        btn_open.setOnClickListener {
            val mimeTypes = arrayOf("text/csv", "text/comma-separated-values")
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
                .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            startActivityForResult(Intent.createChooser(intent, "Select a file"), 0)

        }
    }

    // When file selected,
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == RESULT_OK) {
            val uri = data!!.data
            val csv = uri?.let { readCSV(context, it) }
            adapter!!.clear()
            dataFile = csv
            dataFile?.map {
                try {
                    var num = it[0].toString()
                    var message = it[1].toString()

                    val log = "SĐT : $num \nNội dung : $message"
                    Log.d("sendSMS", log)

                    adapter!!.add(log)
                    lv_sendList.adapter = adapter
                } catch (e: java.lang.Exception) {
                    Log.e("sendSMS", "tel $it.tel \nmessage $it.message", e)
                }
            }
        }
    }

    private fun readCSV(context: Context, uri: Uri): Vector<Array<String>> {
        var fileReader: BufferedReader? = null
        var csvReader: CSVReader? = null
        val data = Vector<Array<String>>()

        try {
            println("--- Read line by line ---")
            // Copy InputStream data to tmp file
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, "tmp.csv")
            FileUtils.copyInputStreamToFile(inputStream, file)
            inputStream?.close()

            fileReader = BufferedReader(FileReader(file))
            csvReader = CSVReader(fileReader)

            var record: Array<String>?
            csvReader.readNext() // skip Header

            record = csvReader.readNext()
            while (record != null) {
                data.add(record)
                record = csvReader.readNext()
            }

            csvReader.close()
        } catch (e: Exception) {
            println("Reading CSV Error!")
            e.printStackTrace()
        } finally {
            try {
                fileReader!!.close()
                csvReader!!.close()
            } catch (e: IOException) {
                println("Closing fileReader/csvParser Error!")
                e.printStackTrace()
            }
        }
        return data
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun sendSMS(view: View) {
        val permissionCheckSendSMS =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
        val permissionCheckReadStatePhone =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
        val permissionCheckReceiveSMS =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS)
        if (permissionCheckSendSMS == PackageManager.PERMISSION_GRANTED && permissionCheckReadStatePhone === PackageManager.PERMISSION_GRANTED && permissionCheckReceiveSMS === PackageManager.PERMISSION_GRANTED) {
            myMessage()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.SEND_SMS,
                    android.Manifest.permission.READ_PHONE_STATE,
                    android.Manifest.permission.RECEIVE_SMS
                ),
                permissionRequest
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun myMessage() {
        Log.d("dataFile", dataFile.toString())
        dataFile?.map {
            try {
                var num = it[0].toString()
                var message = it[1].toString()
                //      TODO Send process feedback
                val sentIntent: PendingIntent =
                    PendingIntent.getBroadcast(this, 0, Intent("SMS_SENT_ACTION"), 0)
                val deliveredIntent: PendingIntent =
                    PendingIntent.getBroadcast(this, 0, Intent("SMS_DELIVERED_ACTION"), 0)
                var state = ""
//                adapter!!.clear()
                registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        Log.d("test SMS sent", " $resultCode")
                        when (resultCode) {
                            Activity.RESULT_OK ->
                                state = "SMS gửi thành công"
                            SmsManager.RESULT_ERROR_GENERIC_FAILURE ->
                                state = "Generic failure cause"
                            SmsManager.RESULT_ERROR_NO_SERVICE ->
                                state = "Service is currently unavailable"
                            SmsManager.RESULT_ERROR_RADIO_OFF ->
                                state = "Radio was explicitly turned off"
                            SmsManager.RESULT_ERROR_NULL_PDU ->
                                state = "No PDU provided"
                        }
//                        val log = "SĐT : $num \nNội dung : $message\nTrạng thái: $state"
//                        adapter!!.notifyItem
//                        adapter!!.add(log)
//                        adapter!!.notifyDataSetChanged()
//                        lv_sendList.adapter = adapter
                        Toast.makeText(context, state, Toast.LENGTH_SHORT).show();
//                context.unregisterReceiver(this)
                    }
                }, IntentFilter("SMS_SENT_ACTION"));

                registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        Log.d("test SMS delivery", " " + resultCode)
                        when (resultCode) {
                            Activity.RESULT_OK ->
                                state = "SMS delivered"
                            Activity.RESULT_CANCELED ->
                                state = "SMS not delivered"
                        }
//                        val log = "SĐT : $num \nNội dung : $message\nTrạng thái: $state"
//                        adapter!!.add(log)
//                        adapter!!.notifyDataSetChanged()
//                        lv_sendList.adapter = adapter
                        Toast.makeText(context, state, Toast.LENGTH_SHORT).show();
//                context.unregisterReceiver(this)
                    }

                }, IntentFilter("SMS_DELIVERED_ACTION"))
                val simCardList: ArrayList<Int> = ArrayList()
                val subscriptionManager =
                    getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_PHONE_STATE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                val subscriptionInfoList = subscriptionManager
                    .activeSubscriptionInfoList
                for (subscriptionInfo in subscriptionInfoList) {
                    val subscriptionId = subscriptionInfo.subscriptionId
                    simCardList.add(subscriptionId)
                }
                val smsToSendFrom =
                    simCardList[0] //assign your desired sim to send sms, or user selected choice
                val mSmsManager: SmsManager = SmsManager.getDefault()
//                     Split message when message.length over a SMS length.

                Log.d("state", state.toString())

                if (message.length > 140) {
                    val messageList: ArrayList<String> = mSmsManager.divideMessage(message)
                    SmsManager.getSmsManagerForSubscriptionId(smsToSendFrom)
                        .sendMultipartTextMessage(
                            num,
                            null,
                            messageList,
                            null,
                            null
                        )
                } else {
                    Log.d("message", message.toString())
                    SmsManager.getSmsManagerForSubscriptionId(smsToSendFrom)
                        .sendTextMessage(
                            num,
                            null,
                            message,
                            sentIntent,
                            deliveredIntent
                        ) //use your phone number, message and pending intents
                }

                Toast.makeText(this, "SMS Sent", Toast.LENGTH_SHORT).show()
            } catch (e: java.lang.Exception) {
                Log.e("sendSMS", "tel $it.tel \nmessage $it.message", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequest) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                myMessage()
            } else {
                Toast.makeText(
                    this, "You don't have required permission to send a message",
                    Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    // Check string is only ascii
//    private fun isPureAscii(v: String): Boolean {
//        return asciiEncoder.canEncode(v)
//    }

}