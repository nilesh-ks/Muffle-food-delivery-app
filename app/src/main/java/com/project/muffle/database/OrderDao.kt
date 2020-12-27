package com.project.muffle.database

import androidx.room.*

@Dao
interface OrderDao{

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrder(orderEntity: OrderEntity)

    @Delete
    fun deleteOrder(orderEntity: OrderEntity)

    @Query("SELECT * FROM orders")
    fun getAllOrders(): List<OrderEntity>

    /*@Query("DELETE FROM orders where resId= :resId")
    fun deleteOrders(resId: String)*/
    @Query("DELETE FROM orders")
    fun deleteOrders()
}