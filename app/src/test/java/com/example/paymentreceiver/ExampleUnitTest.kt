package com.example.paymentreceiver

import com.example.paymentreceiver.data.PaymentMessageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testParseSampleMessageFromUser() {
        val sms = "You have received 1100 XAF from ANITA NEC (237658456328) on your mobile money account at 2026-11-15 11:57:18. Message from sender: 1. Your new balance;1115 XAF. Financial Transaction Id: 19750287609."
        assertTrue(PaymentMessageParser.isValidMessage("MobileMoney", sms))

        val parsed = PaymentMessageParser.parse("MobileMoney", sms, 1000L)
        assertEquals("ANITA NEC", parsed.senderName)
        assertEquals("658456328", parsed.senderNumber)
        assertEquals("1100 XAF", parsed.amount)
        assertEquals("19750287609", parsed.transactionId)
        assertEquals("1", parsed.reason)
    }

    @Test
    fun testTwentyCharTransactionId() {
        val sms = "You have received 5000 XAF from 237670000000. Financial Transaction Id: 12345678901234567890."
        val parsed = PaymentMessageParser.parse("MobileMoney", sms, 1000L)
        assertEquals("670000000", parsed.senderNumber)
        assertEquals("12345678901234567890", parsed.transactionId)
    }

    @Test
    fun testTransactionIdDoesNotConfuseSenderNumber() {
        val sms = "You have received 5000 XAF from 237670000000. ID: 237670000000. Financial Transaction Id: 19750287609."
        val parsed = PaymentMessageParser.parse("MobileMoney", sms, 1000L)
        assertEquals("670000000", parsed.senderNumber)
        assertEquals("19750287609", parsed.transactionId)
    }

    @Test
    fun testParseSampleMessageWithDotReason() {
        val sms = "You have received 1100 XAF from ANITA NEC (237658456328) on your mobile money account at 2026-11-15 11:57:18. Message from sender: . Your new balance;1115 XAF. Financial Transaction Id: 19750287609."
        assertTrue(PaymentMessageParser.isValidMessage("MobileMoney", sms))

        val parsed = PaymentMessageParser.parse("MobileMoney", sms, 1000L)
        assertEquals("ANITA NEC", parsed.senderName)
        assertEquals("658456328", parsed.senderNumber)
        assertEquals("1100 XAF", parsed.amount)
        assertEquals("19750287609", parsed.transactionId)
        assertEquals("", parsed.reason)
    }

    @Test
    fun testParseSampleMessageWithTextReason() {
        val sms = "You have received 1100 XAF from ANITA NEC (237658456328) on your mobile money account at 2026-11-15 11:57:18. Message from sender: Payment for goods. Your new balance;1115 XAF. Financial Transaction Id: 19750287609."
        assertTrue(PaymentMessageParser.isValidMessage("MobileMoney", sms))

        val parsed = PaymentMessageParser.parse("MobileMoney", sms, 1000L)
        assertEquals("ANITA NEC", parsed.senderName)
        assertEquals("658456328", parsed.senderNumber)
        assertEquals("1100 XAF", parsed.amount)
        assertEquals("19750287609", parsed.transactionId)
        assertEquals("Payment for goods", parsed.reason)
    }

    @Test
    fun testParseMessageWithValue() {
        val sms = "You have received 5000 XAF from 237670000000. Transaction ID: TXN12345. Message from sender:value."
        assertTrue(PaymentMessageParser.isValidMessage("MobileMoney", sms))

        val parsed = PaymentMessageParser.parse("MobileMoney", sms, 1000L)
        assertEquals("670000000", parsed.senderNumber)
        assertEquals("5000 XAF", parsed.amount)
        assertEquals("TXN12345", parsed.transactionId)
        assertEquals("value", parsed.reason)
    }

    @Test
    fun testParseMessageWithNoValue() {
        val sms = "You have received 5000 XAF from 237670000000. Transaction ID: TXN12345. Message from sender: ."
        assertTrue(PaymentMessageParser.isValidMessage("MobileMoney", sms))

        val parsed = PaymentMessageParser.parse("MobileMoney", sms, 1000L)
        assertEquals("670000000", parsed.senderNumber)
        assertEquals("5000 XAF", parsed.amount)
        assertEquals("TXN12345", parsed.transactionId)
        assertEquals("", parsed.reason)
    }

    @Test
    fun testParseMessageWithSpaces() {
        val sms = "You have received 10000 FCFA from 237699112233. Transaction ID: 987654. Message from sender: Fee payment."
        val parsed = PaymentMessageParser.parse("OrangeMoney", sms, 1000L)
        assertEquals("699112233", parsed.senderNumber)
        assertEquals("10000 FCFA", parsed.amount)
        assertEquals("987654", parsed.transactionId)
        assertEquals("Fee payment", parsed.reason)
    }
}
