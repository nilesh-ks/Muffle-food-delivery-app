package com.project.muffle.activity

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.project.muffle.R
import com.project.muffle.adapter.CartItemAdapter
import com.project.muffle.adapter.RestaurantMenuAdapter
import com.project.muffle.database.OrderEntity
import com.project.muffle.database.RestaurantDatabase
import com.project.muffle.fragment.RestaurantFragment
import com.project.muffle.model.FoodItem
import com.project.muffle.util.PLACE_ORDER
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONArray
import org.json.JSONObject

class CartActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerCart: RecyclerView
    private lateinit var cartItemAdapter: CartItemAdapter
    private var orderList = ArrayList<FoodItem>()
    private lateinit var txtResName: TextView
    private lateinit var rlLoading: RelativeLayout
    private lateinit var rlCart: RelativeLayout
    private lateinit var btnPlaceOrder: Button


    private var resId: Int = 0
    private  var amount: Int=0
    private var resName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)



        init()
        setupToolbar()
        setUpCartList()
        placeOrder()
    }


    private fun init() {
        rlLoading = findViewById(R.id.rlLoading)
        rlCart = findViewById(R.id.rlCart)
        txtResName = findViewById(R.id.txtCartResName)
        txtResName.text = RestaurantFragment.resName
        val bundle = intent.getBundleExtra("data")
        resId = bundle?.getInt("resId", 0) as Int
        resName = bundle.getString("resName", "") as String
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "My Cart"
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setTitleTextAppearance(this, R.style.GadugiTextAppearance)
    }

    private fun setUpCartList() {
        recyclerCart = findViewById(R.id.recyclerCartItems)

        if (orderList.isNotEmpty()) {
            orderList.clear()
            RestaurantMenuAdapter.isCartEmpty=true
            onBackPressed()
        }
        val dbList = GetItemsFromDBAsync(applicationContext).execute().get()

        /*Extracting the data saved in database and then using Gson to convert the String of food items into a list
        * of food items*/
        for (element in dbList) {
            orderList.addAll(
                Gson().fromJson(element.foodItems, Array<FoodItem>::class.java).asList()
            )
        }

        /*If the order list extracted from DB is empty we do not display the cart*/
        if (orderList.isEmpty()) {
            rlCart.visibility = View.GONE
            rlLoading.visibility = View.VISIBLE
        } else {
            rlCart.visibility = View.VISIBLE
            rlLoading.visibility = View.GONE
        }

        /*Else we display the cart using the cart item adapter*/
        cartItemAdapter = CartItemAdapter(orderList, this@CartActivity)
        val mLayoutManager = LinearLayoutManager(this@CartActivity)
        recyclerCart.layoutManager = mLayoutManager
        recyclerCart.itemAnimator = DefaultItemAnimator()
        recyclerCart.adapter = cartItemAdapter
    }


    private fun placeOrder() {
        btnPlaceOrder = findViewById(R.id.btnConfirmOrder)

        /*Before placing the order, the user is displayed the price or the items on the button for placing the orders*/
        var sum = 0
        for (i in 0 until orderList.size) {
            sum += orderList[i].cost as Int
        }
        val total = "Place Order (Total: Rs. $sum)"
        btnPlaceOrder.text = total
        amount=sum
        btnPlaceOrder.setOnClickListener {
            rlLoading.visibility = View.VISIBLE
            rlCart.visibility = View.INVISIBLE
            sendServerRequest()
//startPayment()
        }
    }

    private fun sendServerRequest() {

        val queue = Volley.newRequestQueue(this)

        /*Creating the json object required for placing the order*/
        val jsonParams = JSONObject()
        jsonParams.put(
            "user_id",
            this@CartActivity.getSharedPreferences("FoodApp", Context.MODE_PRIVATE).getString(
                "user_id",
                null
            ) as String
        )
        jsonParams.put("restaurant_id", RestaurantFragment.resId?.toString() as String)
        var sum = 0
        for (i in 0 until orderList.size) {
            sum += orderList[i].cost as Int
        }
        amount=sum*100
        jsonParams.put("total_cost", sum.toString())
        val foodArray = JSONArray()
        for (i in 0 until orderList.size) {
            val foodId = JSONObject()
            foodId.put("food_item_id", orderList[i].id)
            foodArray.put(i, foodId)
        }
        jsonParams.put("food", foodArray)

        val jsonObjectRequest =
            object : JsonObjectRequest(Method.POST, PLACE_ORDER, jsonParams, Response.Listener {

                try {
                    val data = it.getJSONObject("data")
                    val success = data.getBoolean("success")
                    /*If order is placed, clear the DB for the recently added items
                    * Once the DB is cleared, notify the user that the order has been placed*/
                    if (success) {

                                        startPayment()



                     ClearDBAsync(applicationContext, resId.toString()).execute().get()
                       RestaurantMenuAdapter.isCartEmpty = true

                     /*val dialog = Dialog(
                            this@CartActivity,
                            android.R.style.Theme_Black_NoTitleBar_Fullscreen
                        )
                        dialog.setContentView(R.layout.order_placed_dialog)
                        dialog.show()
                        dialog.setCancelable(false)
                        val btnOk = dialog.findViewById<Button>(R.id.btnOk)
                        btnOk.setOnClickListener {
                            dialog.dismiss()
                            startActivity(Intent(this@CartActivity, DashboardActivity::class.java))
                            ActivityCompat.finishAffinity(this@CartActivity)
                        }*/
                    } else {
                        rlCart.visibility = View.VISIBLE
                        Toast.makeText(this@CartActivity, "Some Error occurred", Toast.LENGTH_SHORT)
                            .show()
                    }

                } catch (e: Exception) {
                    rlCart.visibility = View.VISIBLE
                    e.printStackTrace()
                }

            }, Response.ErrorListener {
                rlCart.visibility = View.VISIBLE
                Toast.makeText(this@CartActivity, it.message, Toast.LENGTH_SHORT).show()
            }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Content-type"] = "application/json"

                    //The below used token will not work, kindly use the token provided to you in the training
                    headers["token"] = "82443d9edf0a08"
                    return headers
                }
            }

        queue.add(jsonObjectRequest)

    }


    /*Asynctask class for extracting the items from the database*/
    class GetItemsFromDBAsync(context: Context) : AsyncTask<Void, Void, List<OrderEntity>>() {
        private val db = Room.databaseBuilder(context, RestaurantDatabase::class.java, "res-db").build()
        override fun doInBackground(vararg params: Void?): List<OrderEntity> {
            return db.orderDao().getAllOrders()
        }

    }

    /*Asynctask class for clearing the recently added items from the database*/
    /*class ClearDBAsync(context: Context, private val resId: String) : AsyncTask<Void, Void, Boolean>() {
        val db = Room.databaseBuilder(context, RestaurantDatabase::class.java, "res-db").build()
        override fun doInBackground(vararg params: Void?): Boolean {
            db.orderDao().deleteOrders(resId)
            db.close()
            return true
        }*/
        class ClearDBAsync(context: Context, private val resId: String) : AsyncTask<Void, Void, Boolean>() {
            val db = Room.databaseBuilder(context, RestaurantDatabase::class.java, "res-db").build()
            override fun doInBackground(vararg params: Void?): Boolean {
                db.orderDao().deleteOrders()
                db.close()
                return true
            }

    }

    /*When the user presses back, we clear the cart so that when the returns to the cart, there is no
    * redundancy in the entries*/
    override fun onSupportNavigateUp(): Boolean {
        if (ClearDBAsync(applicationContext, resId.toString()).execute().get()) {
            RestaurantMenuAdapter.isCartEmpty = true
            onBackPressed()
            return true
        }
        return false
    }

    override fun onBackPressed() {
        ClearDBAsync(applicationContext, resId.toString()).execute().get()
        RestaurantMenuAdapter.isCartEmpty = true
        super.onBackPressed()
    }
    private fun startPayment() {
        /*
        *  You need to pass current activity in order to let Razorpay create CheckoutActivity
        * */
        val activity: Activity = this
        val co = Checkout()

        try {
            val options = JSONObject()
            options.put("name", "Muffle")
            options.put("description", "Your total charges")
            //You can omit the image option to fetch the image from dashboard
            //options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
            options.put("theme.color", "#e7415c");
            options.put("currency", "INR");
            //options.put("order_id", "order_DBJOWzybf0sJbb");

            options.put("amount", amount.toString())//pass amount in currency subunits

            /*val prefill = JSONObject()
            prefill.put("email", "gaurav.kumar@example.com")
            prefill.put("contact", "9876543210")

            options.put("prefill", prefill)*/
            co.open(activity, options)
        } catch (e: Exception) {
            Toast.makeText(activity, "Error in payment: " + e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }


            override fun onPaymentSuccess(p0: String?) {
                Toast.makeText(this, "Payment successful", Toast.LENGTH_SHORT).show()
                  /*  ClearDBAsync(applicationContext, resId.toString()).execute().get()
                RestaurantMenuAdapter.isCartEmpty = true*/


                /*Here we have done something new. We used the Dialog class to display the order placed message
                * It is just a neat trick to avoid creating a whole new activity for a very small purpose
                * Guess, you learned something new here*/
                val dialog = Dialog(
                    this@CartActivity,
                    android.R.style.Theme_Black_NoTitleBar_Fullscreen
                )
                dialog.setContentView(R.layout.order_placed_dialog)
                dialog.show()
                dialog.setCancelable(false)
                val btnOk = dialog.findViewById<Button>(R.id.btnOk)
                btnOk.setOnClickListener {
                    dialog.dismiss()
                    startActivity(Intent(this@CartActivity, DashboardActivity::class.java))
                    ActivityCompat.finishAffinity(this@CartActivity)
                }
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
    }
}
