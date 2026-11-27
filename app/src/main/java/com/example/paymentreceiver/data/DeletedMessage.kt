package com.example.paymentreceiver.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "deleted_messages",
    indices = [Index(value = ["sender", "body", "timestamp"], unique = true)]
)
data class DeletedMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val body: String,
    val timestamp: Long
)
