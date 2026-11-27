package com.example.paymentreceiver.data

import java.util.regex.Pattern

object PaymentMessageParser {

    fun isValidSender(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val s = sender.trim()
        return s.equals("MobileMoney", ignoreCase = true) || s.equals("OrangeMoney", ignoreCase = true)
    }

    fun isValidMessage(sender: String?, body: String?): Boolean {
        if (!isValidSender(sender) || body.isNullOrBlank()) return false
        return body.contains("you have received", ignoreCase = true)
    }

    fun extractSenderName(body: String): String {
        // Sender's name begins just after "from" and ends before phone number
        val pattern = Pattern.compile("(?i)\\bfrom\\b\\s+(.*?)(?=\\s*\\(?\\+?(?:237)?(?:6\\d{8}|\\d{8,12})\\)?)")
        val matcher = pattern.matcher(body)
        if (matcher.find()) {
            var name = matcher.group(1)?.trim() ?: ""
            name = name.removeSuffix("(").trim()
            if (name.isNotBlank() &&
                !name.equals("mobile", ignoreCase = true) &&
                !name.equals("orange", ignoreCase = true) &&
                !name.equals("your", ignoreCase = true)
            ) {
                return name
            }
        }
        return ""
    }

    fun extractSenderNumber(body: String): String {
        val pattern = Pattern.compile("(?i)\\bfrom\\b.*?(?:\\+?237)?\\s*\\(?(\\d{8,12})\\)?")
        val matcher = pattern.matcher(body)
        if (matcher.find()) {
            var digits = matcher.group(1) ?: ""
            digits = digits.replace("+", "").replace("-", "").replace(" ", "")
            if (digits.startsWith("237") && digits.length == 12) {
                digits = digits.substring(3)
            }
            if (digits.length == 9) {
                return digits
            }
            if (digits.length == 12 && digits.startsWith("237")) {
                return digits.substring(3)
            }
        }

        val fallbackPattern = Pattern.compile("\\b(?:237)?(6\\d{8})\\b|\\b237(\\d{9})\\b")
        val fallbackMatcher = fallbackPattern.matcher(body)
        if (fallbackMatcher.find()) {
            return fallbackMatcher.group(1) ?: fallbackMatcher.group(2) ?: ""
        }

        return ""
    }

    fun extractAmount(body: String): String {
        val pattern = Pattern.compile("(?i)you\\s+have\\s+received\\s+([\\d,.]+\\s*(?:XAF|FCFA|CFA|F|FRS)?|\\$[\\d,.]+)")
        val matcher = pattern.matcher(body)
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: ""
        }
        return ""
    }

    fun extractTransactionId(body: String): String {
        val priorityPattern = Pattern.compile("(?i)(?:financial\\s+)?transaction\\s*id\\s*[:=]?\\s*([a-zA-Z0-9]+)")
        val priorityMatcher = priorityPattern.matcher(body)
        var fallbackCandidate = ""
        while (priorityMatcher.find()) {
            val candidate = priorityMatcher.group(1)?.trim() ?: ""
            if (isValidTransactionId(candidate)) {
                if (candidate.length == 11 || candidate.length == 20) {
                    return candidate
                }
                if (fallbackCandidate.isEmpty()) {
                    fallbackCandidate = candidate
                }
            }
        }
        if (fallbackCandidate.isNotEmpty()) return fallbackCandidate

        val pattern = Pattern.compile("(?i)(?:txn|trans|tx)\\s*(?:id|ref|reference)?\\s*[:=]?\\s*([a-zA-Z0-9]+)")
        val matcher = pattern.matcher(body)
        while (matcher.find()) {
            val candidate = matcher.group(1)?.trim() ?: ""
            if (isValidTransactionId(candidate)) {
                if (candidate.length == 11 || candidate.length == 20) {
                    return candidate
                }
                if (fallbackCandidate.isEmpty()) {
                    fallbackCandidate = candidate
                }
            }
        }
        if (fallbackCandidate.isNotEmpty()) return fallbackCandidate

        val refPattern = Pattern.compile("(?i)ref(?:erence)?\\s*[:=]?\\s*([a-zA-Z0-9]+)")
        val refMatcher = refPattern.matcher(body)
        while (refMatcher.find()) {
            val candidate = refMatcher.group(1)?.trim() ?: ""
            if (isValidTransactionId(candidate)) {
                if (candidate.length == 11 || candidate.length == 20) {
                    return candidate
                }
                if (fallbackCandidate.isEmpty()) {
                    fallbackCandidate = candidate
                }
            }
        }

        return fallbackCandidate
    }

    private fun isValidTransactionId(candidate: String): Boolean {
        if (candidate.isBlank()) return false
        val cleanCandidate = candidate.replace("+", "").replace("-", "").trim()

        if (cleanCandidate.length == 9 || (cleanCandidate.length == 12 && cleanCandidate.startsWith("237"))) {
            return false
        }

        return true
    }

    private fun cleanReasonText(text: String): String {
        var result = text
        result = result.replace("(?i)^(?:message\\s*[:=]?)?\\s*(?:from\\s+(?:the\\s+)?sender|the\\s+sender|sender)[:\\s]*".toRegex(), "").trim()
        result = result.replace("(?i)^(?:from\\s+(?:the\\s+)?sender|the\\s+sender|sender)[:\\s]*".toRegex(), "").trim()
        result = result.replace("(?i)(?:message\\s*[:=]?)?\\s*(?:from\\s+(?:the\\s+)?sender|the\\s+sender)[:\\s]*".toRegex(), "").trim()

        if (result.all { it == '.' || it == ':' || it == '-' || it == ';' || it.isWhitespace() }) {
            return ""
        }
        return result.trimEnd('.').trim()
    }

    fun extractReason(body: String): String {
        val priorityPattern = Pattern.compile("(?i)Message\\s*[:=]?\\s*(?:from\\s+)?(?:the\\s+)?sender\\s*[:=]?\\s*([^.\n;]*)")
        val priorityMatcher = priorityPattern.matcher(body)
        if (priorityMatcher.find()) {
            val captured = priorityMatcher.group(1)?.trim() ?: ""
            return cleanReasonText(captured)
        }

        val pattern = Pattern.compile("(?i)(?:reason|motif|note)\\s*[:=]?\\s*([^.\n;]*)")
        val matcher = pattern.matcher(body)
        if (matcher.find()) {
            val captured = matcher.group(1)?.trim() ?: ""
            return cleanReasonText(captured)
        }

        return ""
    }

    fun parse(sender: String, body: String, timestamp: Long): PaymentMessage {
        val senderName = extractSenderName(body)
        val senderNumber = extractSenderNumber(body)
        val amount = extractAmount(body)
        val transactionId = extractTransactionId(body)
        val reason = extractReason(body)

        return PaymentMessage(
            sender = sender.trim(),
            body = body,
            timestamp = timestamp,
            senderNumber = senderNumber,
            senderName = senderName,
            amount = amount,
            transactionId = transactionId,
            reason = reason
        )
    }
}
