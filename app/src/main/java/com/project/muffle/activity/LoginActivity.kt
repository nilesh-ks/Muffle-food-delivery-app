package com.project.muffle.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.project.muffle.R
import com.project.muffle.util.ConnectionManager
import com.project.muffle.util.LOGIN
import com.project.muffle.util.SessionManager
import com.project.muffle.util.Validations
import org.json.JSONException
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {


    lateinit var etMobileNumber: EditText
    lateinit var etPassword: EditText
    lateinit var btnLogin: Button
    lateinit var txtForgotPassword: TextView
    lateinit var txtRegisterYourself: TextView

    lateinit var sessionManager: SessionManager
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        /*Initialising the views*/
        etMobileNumber = findViewById(R.id.etMobileNumber)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        txtForgotPassword = findViewById(R.id.txtForgotPassword)
        txtRegisterYourself = findViewById(R.id.txtRegisterYourself)

        /*Initialising the session variables*/
        sessionManager = SessionManager(this)
        sharedPreferences =
            this.getSharedPreferences(sessionManager.PREF_NAME, sessionManager.PRIVATE_MODE)

        /*Clicking on this text takes you to the forgot password activity*/
        txtForgotPassword.setOnClickListener {
            startActivity(Intent(this@LoginActivity, ForgotPasswordActivity::class.java))
        }


        txtRegisterYourself.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }


        btnLogin.setOnClickListener {

            btnLogin.visibility = View.INVISIBLE


            if (Validations.validateMobile(etMobileNumber.text.toString()) && Validations.validatePasswordLength(etPassword.text.toString())) {
                if (ConnectionManager().isNetworkAvailable(this@LoginActivity)) {


                    val queue = Volley.newRequestQueue(this@LoginActivity)

                    val jsonParams = JSONObject()
                    jsonParams.put("mobile_number", etMobileNumber.text.toString())
                    jsonParams.put("password", etPassword.text.toString())


                    val jsonObjectRequest = object : JsonObjectRequest(Method.POST, LOGIN, jsonParams,
                        Response.Listener {

                            try {
                                val data = it.getJSONObject("data")
                                val success = data.getBoolean("success")
                                if (success) {
                                    val response = data.getJSONObject("data")
                                    sharedPreferences.edit()
                                        .putString("user_id", response.getString("user_id")).apply()
                                    sharedPreferences.edit()
                                        .putString("user_name", response.getString("name")).apply()
                                    sharedPreferences.edit()
                                        .putString(
                                            "user_mobile_number",
                                            response.getString("mobile_number")
                                        )
                                        .apply()
                                    sharedPreferences.edit()
                                        .putString("user_address", response.getString("address"))
                                        .apply()
                                    sharedPreferences.edit()
                                        .putString("user_email", response.getString("email")).apply()
                                    sessionManager.setLogin(true)
                                    startActivity(
                                        Intent(
                                            this@LoginActivity,
                                            DashboardActivity::class.java
                                        )
                                    )
                                    finish()
                                } else {
                                    btnLogin.visibility = View.VISIBLE
                                    txtForgotPassword.visibility = View.VISIBLE
                                    btnLogin.visibility = View.VISIBLE
                                    val errorMessage = data.getString("errorMessage")
                                    Toast.makeText(
                                        this@LoginActivity,
                                        errorMessage,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: JSONException) {
                                btnLogin.visibility = View.VISIBLE
                                txtForgotPassword.visibility = View.VISIBLE
                                txtRegisterYourself.visibility = View.VISIBLE
                                e.printStackTrace()
                            }
                        },
                        Response.ErrorListener {
                            btnLogin.visibility = View.VISIBLE
                            txtForgotPassword.visibility = View.VISIBLE
                            txtRegisterYourself.visibility = View.VISIBLE
                            Log.e("Error::::", "/post request fail! Error: ${it.message}")
                        }) {
                        override fun getHeaders(): MutableMap<String, String> {
                            val headers = HashMap<String, String>()
                            headers["Content-type"] = "application/json"

                            /*The below used token will not work, kindly use the token provided to you in the training*/
                            headers["token"] = "82443d9edf0a08"
                            return headers
                        }
                    }
                    queue.add(jsonObjectRequest)

                } else {
                    btnLogin.visibility = View.VISIBLE
                    txtForgotPassword.visibility = View.VISIBLE
                    txtRegisterYourself.visibility = View.VISIBLE
                    Toast.makeText(this@LoginActivity, "No internet Connection", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                btnLogin.visibility = View.VISIBLE
                txtForgotPassword.visibility = View.VISIBLE
                txtRegisterYourself.visibility = View.VISIBLE
                Toast.makeText(this@LoginActivity, "Invalid Phone or Password", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    }
}
