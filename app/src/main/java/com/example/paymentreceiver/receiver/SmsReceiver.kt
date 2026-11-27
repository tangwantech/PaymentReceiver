package com.example.paymentreceiver.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.paymentreceiver.data.SmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val repository = SmsRepository(context)

            // Group multi-part messages by originating address
            val messagesBySender = messages.groupBy { it.displayOriginatingAddress ?: it.originatingAddress }

            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    for ((sender, partList) in messagesBySender) {
                        if (sender != null && repository.isValidSender(sender)) {
                            val fullBody = partList.joinToString("") { it.messageBody ?: "" }
                            val timestamp = partList.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
                            repository.insertMessage(sender, fullBody, timestamp)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
