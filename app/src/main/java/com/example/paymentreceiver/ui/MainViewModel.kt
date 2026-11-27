package com.example.paymentreceiver.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.paymentreceiver.data.Messages
import com.example.paymentreceiver.data.PaymentMessage
import com.example.paymentreceiver.data.SmsRepository
import com.example.paymentreceiver.repositories.RestRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class ProviderFilter(val displayName: String) {
    MOBILE_MONEY("MobileMoney"),
    ORANGE_MONEY("OrangeMoney")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var restRepo =  RestRepository()
    private val repository = SmsRepository(application)
    private var _messages = ArrayList<PaymentMessage>()

    private val _selectedProvider = MutableStateFlow(ProviderFilter.MOBILE_MONEY)
    val selectedProvider: StateFlow<ProviderFilter> = _selectedProvider.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val filteredMessages: StateFlow<List<PaymentMessage>> = combine(
        repository.allMessages,
        _selectedProvider
    ) { allMessages, provider ->
        allMessages.filter { message ->
            message.sender.equals(provider.displayName, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectProvider(provider: ProviderFilter) {
        _selectedProvider.value = provider
    }

    fun syncInbox() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.syncMessagesFromInbox()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteMessages(messages: List<PaymentMessage>) {
        viewModelScope.launch {
            repository.deleteMessages(messages)
        }
    }

    fun updateMessages(temp: List<PaymentMessage>){
        _messages.clear()
        _messages.addAll(temp)
        _messages.add(PaymentMessage(sender="Elice", body = "", timestamp = 0, senderNumber = "658667466", senderName = "Elice", amount = "100", transactionId = "12345678911", reason = "3tk6qub"))
        sendToServer()
    }

    fun sendToServer(){

        val messages = JSONObject(Gson().toJson(Messages(_messages)))
        val params = hashMapOf<String, Any>("messages" to messages)

        restRepo.query(params, object : RestRepository.OnQueryListener{
            override fun onSuccess(result: String) {
                Log.d("Result", result)
            }

            override fun onError(error: String?) {
                Log.e("Result", "Error: $error")
            }
        }
        )
    }
}
