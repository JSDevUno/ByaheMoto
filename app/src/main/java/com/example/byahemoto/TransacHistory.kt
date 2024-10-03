package com.example.byahemoto

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.byahemoto.models.TransactionResponse
import com.example.byahemoto.network.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TransacHistory : AppCompatActivity() {


    private lateinit var listView: ListView
    private lateinit var transactionAdapter: ArrayAdapter<String>
    private val transactionList = mutableListOf<String>()
    private val TAG = "TransacHistory"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        listView = findViewById(R.id.commuter_transaction_history)
        transactionAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, transactionList)
        listView.adapter = transactionAdapter

        loadTransactionHistory()
    }

    private fun loadTransactionHistory() {
        val token = getTokenFromSharedPreferences()

        if (token != null) {
            RetrofitInstance.getAuthService(this).getTransactionHistory("Bearer $token").enqueue(object : Callback<TransactionResponse> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<TransactionResponse>, response: Response<TransactionResponse>) {
                    if (response.isSuccessful) {
                        val transactionResponse = response.body()
                        Log.d("TRANSACTION RESPONSE", "Transaction response: $transactionResponse")
                        val transactions = transactionResponse?.data

                        if (transactions.isNullOrEmpty()) {
                            transactionList.add("No transactions available")
                        } else {
                            transactions.forEach { transaction ->
                                val transactionEntry = when (transaction.type) {
                                    "payment" -> "₱${transaction.amount} credited for trip completed on " +
                                            "${formatDate(transaction.createdAt)}, at ${formatTime(transaction.createdAt)}."
                                    "credit" -> "You successfully topped up ₱${transaction.amount} to your account on " +
                                            "${formatDate(transaction.createdAt)}, at ${formatTime(transaction.createdAt)}."
                                    else -> transaction.type
                                }
                                transactionList.add(transactionEntry)
                            }
                        }

                        transactionAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@TransacHistory, "Failed to load transaction history", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<TransactionResponse>, t: Throwable) {
                    Toast.makeText(this@TransacHistory, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error fetching transaction history", t)
                }
            })
        } else {
            Toast.makeText(this, "Token not available. Please login again.", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatDate(dateString: String): String {
        val zonedDateTime = ZonedDateTime.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return zonedDateTime.format(formatter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatTime(dateString: String): String {
        val zonedDateTime = ZonedDateTime.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        return zonedDateTime.format(formatter)
    }

    private fun getTokenFromSharedPreferences(): String? {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("access_token", null)
    }
}
