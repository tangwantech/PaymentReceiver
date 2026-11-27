package com.example.paymentreceiver.data

import android.content.Context
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SmsRepository(private val context: Context) {

    private val dao: PaymentMessageDao by lazy {
        AppDatabase.getDatabase(context).paymentMessageDao()
    }

    val allMessages: Flow<List<PaymentMessage>> = dao.getAllMessagesSorted()

    fun isValidSender(sender: String?): Boolean {
        return PaymentMessageParser.isValidSender(sender)
    }

    private fun createSignature(sender: String, body: String, timestamp: Long): String {
        return "${sender.lowercase().trim()}___${body.trim()}___$timestamp"
    }

    suspend fun syncMessagesFromInbox() = withContext(Dispatchers.IO) {
        val currentRoomMessages = dao.getAllMessagesDirect()
        val deletedList = dao.getAllDeletedMessages()
        val deletedSignatures = deletedList.map { createSignature(it.sender, it.body, it.timestamp) }.toSet()

        val inboxSignatures = mutableSetOf<String>()
        val messagesToInsert = mutableListOf<PaymentMessage>()

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use { c ->
                val addressIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)

                while (c.moveToNext()) {
                    val address = if (addressIdx >= 0) c.getString(addressIdx) else null
                    val body = if (bodyIdx >= 0) c.getString(bodyIdx) else ""
                    val date = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()

                    if (PaymentMessageParser.isValidMessage(address, body)) {
                        val sig = createSignature(address!!, body, date)
                        inboxSignatures.add(sig)

                        if (!deletedSignatures.contains(sig)) {
                            val parsedMsg = PaymentMessageParser.parse(address, body, date)
                            messagesToInsert.add(parsedMsg)
                        }
                    }
                }
            }

            // Delete from Room any message that was removed from the phone's inbox
            val messagesToRemoveFromRoom = currentRoomMessages.filter { roomMsg ->
                val roomSig = createSignature(roomMsg.sender, roomMsg.body, roomMsg.timestamp)
                !inboxSignatures.contains(roomSig)
            }

            if (messagesToRemoveFromRoom.isNotEmpty()) {
                dao.deleteMessages(messagesToRemoveFromRoom)
            }

            if (messagesToInsert.isNotEmpty()) {
                dao.insertAll(messagesToInsert)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertMessage(sender: String, body: String, timestamp: Long) = withContext(Dispatchers.IO) {
        if (PaymentMessageParser.isValidMessage(sender, body)) {
            val deletedList = dao.getAllDeletedMessages()
            val deletedSignatures = deletedList.map { createSignature(it.sender, it.body, it.timestamp) }.toSet()
            val sig = createSignature(sender, body, timestamp)
            if (!deletedSignatures.contains(sig)) {
                val parsedMsg = PaymentMessageParser.parse(sender, body, timestamp)
                dao.insert(parsedMsg)
            }
        }
    }

    suspend fun deleteMessages(messages: List<PaymentMessage>) = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) return@withContext
        val deletedMessages = messages.map {
            DeletedMessage(
                sender = it.sender.trim(),
                body = it.body,
                timestamp = it.timestamp
            )
        }
        dao.insertDeletedSignatures(deletedMessages)
        dao.deleteMessages(messages)
    }
}
