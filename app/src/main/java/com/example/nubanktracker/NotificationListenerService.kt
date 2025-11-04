package com.example.nubanktracker

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale
class NotificationListenerService : NotificationListenerService() {

    companion object {
        const val TAG = "NubankListener"
        const val NUBANK_PACKAGE = "com.nu.production"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn?.packageName == NUBANK_PACKAGE) {
            val notification = sbn.notification
            val extras = notification.extras

            val title = extras.getString(Notification.EXTRA_TITLE, "")
            val text = extras.getCharSequence(Notification.EXTRA_TEXT, "").toString()

            Log.d(TAG, "Título: $title")
            Log.d(TAG, "Texto: $text")

            parseAndSaveTransaction(title, text)
        }
    }

    private fun parseAndSaveTransaction(title: String, text: String) {
        val transaction = parseTransaction(title, text)

        if (transaction != null) {
            val intent = Intent("com.example.nubanktracker.NEW_TRANSACTION")
            intent.putExtra("type", transaction.type)
            intent.putExtra("amount", transaction.amount)
            intent.putExtra("description", transaction.description)
            intent.putExtra("date", transaction.date)
            sendBroadcast(intent)

            // Salva no banco de dados
            TransactionDatabase.getInstance(this).addTransaction(transaction)
        }
    }

    private fun parseTransaction(title: String, text: String): Transaction? {
        val fullText = "$title $text"

        // Regex para capturar valores em reais (formato brasileiro: 1.234,56)
        val amountRegex = """R\$\s*([\d.]+,\d{2})""".toRegex()
        val matchResult = amountRegex.find(fullText)

        if (matchResult != null) {
            // Remove pontos de milhar e substitui vírgula por ponto
            val amountStr = matchResult.groupValues[1]
                .replace(".", "")  // Remove separador de milhar
                .replace(",", ".") // Converte vírgula decimal para ponto
            val amount = amountStr.toDoubleOrNull() ?: return null

            val type = when {
                fullText.contains("compra", ignoreCase = true) -> "Despesa"
                fullText.contains("pagamento", ignoreCase = true) -> "Despesa"
                fullText.contains("fatura", ignoreCase = true) -> "Despesa"
                fullText.contains("débito", ignoreCase = true) -> "Despesa"
                fullText.contains("transferência enviada", ignoreCase = true) -> "Despesa"
                fullText.contains("recebeu", ignoreCase = true) -> "Renda"
                fullText.contains("transferência recebida", ignoreCase = true) -> "Renda"
                fullText.contains("depósito", ignoreCase = true) -> "Renda"
                fullText.contains("rendimento", ignoreCase = true) -> "Renda"
                else -> "Despesa" // Por padrão, considera como despesa
            }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))
            val currentDate = dateFormat.format(Date())

            return Transaction(
                type = type,
                amount = amount,
                description = text.take(100),
                date = currentDate
            )
        }

        return null
    }
}