package com.example.paymentreceiver.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMessageDao {

    @Query("SELECT * FROM payment_messages ORDER BY timestamp DESC")
    fun getAllMessagesSorted(): Flow<List<PaymentMessage>>

    @Query("SELECT * FROM payment_messages")
    suspend fun getAllMessagesDirect(): List<PaymentMessage>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: PaymentMessage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<PaymentMessage>)

    @Delete
    suspend fun deleteMessages(messages: List<PaymentMessage>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeletedSignatures(deletedMessages: List<DeletedMessage>)

    @Query("SELECT * FROM deleted_messages")
    suspend fun getAllDeletedMessages(): List<DeletedMessage>
}
