package com.example.byahemoto

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.byahemoto.models.GetProfileResponse
import com.example.byahemoto.models.TransactionResponse
import com.example.byahemoto.network.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MenuFragment : Fragment() {

    private lateinit var driverMoney: TextView
    private lateinit var listView: ListView
    private lateinit var transactionAdapter: ArrayAdapter<String>
    private val transactionList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isAdded) return
        val context = context ?: return
        val userPrefs = context.getSharedPreferences("user_prefs", 0)
        val token = userPrefs.getString("access_token", null) ?: return Toast.makeText(context, "Invalid Token...", Toast.LENGTH_SHORT).show()
        val authService = RetrofitInstance.getAuthService(context)

        driverMoney = view.findViewById(R.id.driverMoneyText)

        authService.getUserProfile("Bearer $token").enqueue(object :
            Callback<GetProfileResponse> {
            override fun onResponse(call: Call<GetProfileResponse>, response: Response<GetProfileResponse>) {
                if (response.isSuccessful) {
                    // UPDATE THE DRIVER'S MONEY
                    val balance = response.body()?.data?.wallet?.balance
                    driverMoney.text = balance.toString()
                }
            }
            override fun onFailure(call: Call<GetProfileResponse>, t: Throwable) {
                Log.e("MenuFragment", "Failed to get user profile", t)
            }
        })
        loadTransactionHistory()
    }
    private fun loadTransactionHistory() {
        val context = context ?: return
        val view = view ?: return
        val token = getTokenFromSharedPreferences()
        listView = view.findViewById(R.id.driver_transaction_history)
        transactionAdapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, transactionList)
        listView.adapter = transactionAdapter
        if (token != null) {
            RetrofitInstance.getAuthService(context).getTransactionHistory("Bearer $token").enqueue(object : Callback<TransactionResponse> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<TransactionResponse>, response: Response<TransactionResponse>) {
                    if (response.isSuccessful) {
                        val transactionResponse = response.body()
                        val transactions = transactionResponse?.data
                        if (transactions.isNullOrEmpty()) {
                            transactionList.add("No transactions available")
                        } else {
                            transactions.forEach { transaction ->
                                val transactionEntry = when (transaction.type) {
                                    "credit" -> "You successfully dropped off the passenger at the destination. ₱${transaction.amount} have been credited to your account."
                                    else -> transaction.type
                                }
                                transactionList.add(transactionEntry)
                            }
                        }
                        transactionAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(context, "Failed to load transaction history", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<TransactionResponse>, t: Throwable) {
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error fetching transaction history", t)
                }
            })
        } else {
            Toast.makeText(context, "Token not available. Please login again.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun getTokenFromSharedPreferences(): String? {
        val sharedPref = activity?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref?.getString("access_token", null)
    }
}