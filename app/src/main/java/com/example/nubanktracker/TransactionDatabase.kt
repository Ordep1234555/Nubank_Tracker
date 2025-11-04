package com.example.nubanktracker

import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.Locale
import java.nio.charset.Charset

class TransactionDatabase private constructor() {

    companion object {
        @Volatile
        private var instance: TransactionDatabase? = null

        fun getInstance(): TransactionDatabase {
            return instance ?: synchronized(this) {
                instance ?: TransactionDatabase().also { instance = it }
            }
        }
    }

    private val transactions = mutableListOf<Transaction>()

    init {
        loadFromCSV() // Carrega dados ao iniciar
    }

    private fun loadFromCSV() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "nubank_transacoes.csv")

            if (file.exists()) {
                file.bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.readLine() // Pula o cabeçalho
                    reader.forEachLine { line ->
                        val parts = line.split(";")
                        if (parts.size >= 4) {
                            val date = parts[0]
                            val type = parts[1]
                            val amount = parts[2].replace(",", ".").toDoubleOrNull() ?: 0.0
                            val description = parts[3].trim('"')
                            transactions.add(Transaction(type, amount, description, date))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addTransaction(transaction: Transaction) {
        transactions.add(transaction)
        saveToCSV()
    }

    fun getAllTransactions(): List<Transaction> = transactions.toList()

    fun clearTransactions() {
        transactions.clear()
        saveToCSV()
    }

    private fun saveToCSV() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "nubank_transacoes.csv")

            // Use ISO-8859-1 (Latin-1) — sem BOM
            OutputStreamWriter(FileOutputStream(file), Charset.forName("ISO-8859-1")).use { writer ->

                // Cabeçalho
                writer.append("Data;Tipo;Valor;Descrição\n")

                // Dados
                transactions.forEach { transaction ->
                    writer.append("${transaction.date};")
                    writer.append("${transaction.type};")
                    writer.append("${String.format(Locale.forLanguageTag("pt-BR"), "%.2f", transaction.amount)};")
                    writer.append("\"${transaction.description}\"\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportToCSV(): String {
        saveToCSV()
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, "nubank_transacoes.csv")
        return file.absolutePath
    }
}