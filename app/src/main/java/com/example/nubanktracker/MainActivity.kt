package com.example.nubanktracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var transactionsText: TextView
    private lateinit var enableButton: Button
    private lateinit var exportButton: Button
    private lateinit var clearButton: Button

    private val transactionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateTransactionsList()
            Toast.makeText(this@MainActivity, "Nova transação detectada!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        transactionsText = findViewById(R.id.transactionsText)
        enableButton = findViewById(R.id.enableButton)
        exportButton = findViewById(R.id.exportButton)
        clearButton = findViewById(R.id.clearButton)

        enableButton.setOnClickListener {
            openNotificationSettings()
        }

        exportButton.setOnClickListener {
            exportTransactions()
        }

        clearButton.setOnClickListener {
            clearTransactions()
        }

        updateStatus()
        updateTransactionsList()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        updateStatus()
        registerReceiver(transactionReceiver, IntentFilter("com.example.nubanktracker.NEW_TRANSACTION"), RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(transactionReceiver)
    }

    private fun updateStatus() {
        val enabled = isNotificationServiceEnabled()
        statusText.text = if (enabled) {
            "Status: Ativo ✓"
        } else {
            "Status: Inativo - Permissão necessária"
        }
    }

    private fun updateTransactionsList() {
        val transactions = TransactionDatabase.getInstance().getAllTransactions()

        if (transactions.isEmpty()) {
            transactionsText.text = getString(R.string.nenhuma_transa_o_ainda)
        } else {
            val sb = StringBuilder()
            sb.append("Total de transações: ${transactions.size}\n\n")

            transactions.takeLast(10).reversed().forEach { transaction ->
                sb.append("${transaction.date}\n")
                sb.append("${transaction.type}: R$ ${String.format(Locale.forLanguageTag("pt-BR"), "%.2f", transaction.amount)}\n")
                sb.append("${transaction.description}\n")
                sb.append("---\n")
            }

            transactionsText.text = sb.toString()
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(this)
        return enabledListeners.contains(packageName)
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Ative o 'Nubank Tracker' na lista", Toast.LENGTH_LONG).show()
    }

    private fun exportTransactions() {
        val path = TransactionDatabase.getInstance().exportToCSV()
        Toast.makeText(this, "Planilha salva em: $path", Toast.LENGTH_LONG).show()
    }

    private fun clearTransactions() {
        TransactionDatabase.getInstance().clearTransactions()
        updateTransactionsList()
        Toast.makeText(this, "Transações apagadas", Toast.LENGTH_SHORT).show()
    }
}