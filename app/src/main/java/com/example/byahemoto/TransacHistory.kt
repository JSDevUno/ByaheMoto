package com.example.byahemoto

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.byahemoto.models.TransactionResponse
import com.example.byahemoto.network.RetrofitInstance
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Context
import android.util.Log

class TransacHistory : AppCompatActivity() {


    private lateinit var listView: ListView
    private lateinit var transactionAdapter: ArrayAdapter<String>
    private val transactionList = mutableListOf<String>()
    private val TAG = "TransacHistory"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        listView = findViewById(R.id.list_view)
        transactionAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, transactionList)
        listView.adapter = transactionAdapter

        loadTransactionHistory()
    }

    private fun loadTransactionHistory() {
        val token = getTokenFromSharedPreferences()

        if (token != null) {
            RetrofitInstance.getAuthService(this).getTransactionHistory("Bearer $token").enqueue(object : Callback<TransactionResponse> {
                override fun onResponse(call: Call<TransactionResponse>, response: Response<TransactionResponse>) {
                    if (response.isSuccessful) {
                        val transactionResponse = response.body()
                        val transactions = transactionResponse?.transactions

                        if (transactions.isNullOrEmpty()) {
                            transactionList.add("No transactions available")
                        } else {
                            transactions.forEach { transaction ->
                                val transactionEntry = when (transaction.type) {
                                    "send_money" -> "Send Money: ${transaction.amount}"
                                    "topup" -> "Top-up: ${transaction.amount}"
                                    else -> transaction.description ?: "Transaction"
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


    private fun getTokenFromSharedPreferences(): String? {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("access_token", null)
    }
}
