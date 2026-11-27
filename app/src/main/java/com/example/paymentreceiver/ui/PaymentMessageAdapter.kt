package com.example.paymentreceiver.ui

import android.graphics.Color
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.paymentreceiver.R
import com.example.paymentreceiver.data.PaymentMessage
import com.google.android.material.card.MaterialCardView
import java.util.Date

class PaymentMessageAdapter : ListAdapter<PaymentMessage, PaymentMessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    private val selectedItems = mutableSetOf<PaymentMessage>()
    var isSelectionMode: Boolean = false
        private set

    var onSelectionChangedListener: ((selectedCount: Int) -> Unit)? = null

    fun getSelectedMessages(): List<PaymentMessage> {
        return selectedItems.toList()
    }

    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        onSelectionChangedListener?.invoke(0)
    }

    fun toggleSelection(message: PaymentMessage) {
        if (selectedItems.contains(message)) {
            selectedItems.remove(message)
        } else {
            selectedItems.add(message)
        }
        isSelectionMode = selectedItems.isNotEmpty()
        notifyDataSetChanged()
        onSelectionChangedListener?.invoke(selectedItems.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_payment_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        val isSelected = selectedItems.contains(message)
        holder.bind(message, isSelectionMode, isSelected)

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(message)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                toggleSelection(message)
            } else {
                toggleSelection(message)
            }
            true
        }
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)
        private val tvSender: TextView = itemView.findViewById(R.id.tvSender)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvSenderName: TextView = itemView.findViewById(R.id.tvSenderName)
        private val tvSenderNumber: TextView = itemView.findViewById(R.id.tvSenderNumber)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val tvTransactionId: TextView = itemView.findViewById(R.id.tvTransactionId)
        private val tvReason: TextView = itemView.findViewById(R.id.tvReason)

        fun bind(message: PaymentMessage, isSelectionMode: Boolean, isSelected: Boolean) {
            tvSender.text = message.sender
            val formattedDate = DateFormat.format("MMM dd, yyyy • HH:mm:ss", Date(message.timestamp))
            tvTimestamp.text = formattedDate

            if (message.amount.isNotBlank()) {
                tvAmount.text = "Amount: ${message.amount}"
                tvAmount.visibility = View.VISIBLE
            } else {
                tvAmount.visibility = View.GONE
            }
            tvSenderName.text = "Sender Name: ${message.senderName}"
            tvSenderNumber.text = "Sender Number: ${message.senderNumber}"

//            if (message.senderName.isNotBlank() && message.senderNumber.isNotBlank()) {
//                tvSenderNumber.text = "From: ${message.senderName} (${message.senderNumber})"
//                tvSenderNumber.visibility = View.VISIBLE
//            } else if (message.senderNumber.isNotBlank()) {
//                tvSenderNumber.text = "From: ${message.senderNumber}"
//                tvSenderNumber.visibility = View.VISIBLE
//            } else if (message.senderName.isNotBlank()) {
//                tvSenderNumber.text = "From: ${message.senderName}"
//                tvSenderNumber.visibility = View.VISIBLE
//            } else {
//                tvSenderNumber.visibility = View.GONE
//            }

            if (message.transactionId.isNotBlank()) {
                tvTransactionId.text = "TxID: ${message.transactionId}"
                tvTransactionId.visibility = View.VISIBLE
            } else {
                tvTransactionId.visibility = View.GONE
            }

            if (message.reason.isNotBlank()) {
                tvReason.text = "Reason: ${message.reason}"
            } else {
                tvReason.text = "Reason: "
            }
            tvReason.visibility = View.VISIBLE

            if (isSelectionMode) {
                cbSelect.visibility = View.VISIBLE
                cbSelect.isChecked = isSelected
            } else {
                cbSelect.visibility = View.GONE
            }

            if (isSelected) {
                cardView.setStrokeColor(Color.parseColor("#1976D2"))
                cardView.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
            } else {
                cardView.setStrokeColor(Color.parseColor("#E0E0E0"))
                cardView.setCardBackgroundColor(Color.WHITE)
            }
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<PaymentMessage>() {
        override fun areItemsTheSame(oldItem: PaymentMessage, newItem: PaymentMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PaymentMessage, newItem: PaymentMessage): Boolean {
            return oldItem == newItem
        }
    }
}
