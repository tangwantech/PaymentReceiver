package com.example.paymentreceiver.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_messages",
    indices = [Index(value = ["sender", "body", "timestamp"], unique = true)]
)
data class PaymentMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val senderNumber: String = "",
    val senderName: String = "",
    val amount: String = "",
    val transactionId: String = "",
    val reason: String = ""
)
