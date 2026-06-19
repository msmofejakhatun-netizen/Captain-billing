package com.example.data.remote

data class LoginRequest(
    val restaurantCode: String,
    val username: String,
    val password: String
)

data class UserDto(
    val id: String,
    val username: String,
    val restaurantCode: String? = null,
    val role: String
)

data class LoginResponse(
    val user: UserDto,
    val token: String,
    val restaurantCode: String? = null,
    val success: Boolean? = null,
    val restaurantId: String? = null,
    val username: String? = null,
    val role: String? = null
)

data class TableOpenRequest(
    val tableNumber: String
)

data class AddItemRequest(
    val tableId: String,
    val menuItemId: String,
    val quantity: Int
)

data class UpdateItemRequest(
    val tableId: String,
    val itemId: String,
    val quantity: Int
)

data class RemoveItemRequest(
    val tableId: String,
    val itemId: String
)

data class SendKOTRequest(
    val tableId: String
)

data class GenerateBillRequest(
    val tableId: String
)

data class SettleBillRequest(
    val billId: String,
    val cashAmount: Double,
    val cardAmount: Double,
    val upiAmount: Double,
    val paymentType: String // "CASH", "CARD", "UPI", "MIXED"
)

data class TableDto(
    val id: String? = null,
    @com.squareup.moshi.Json(name = "_id") val underscoreId: String? = null,
    val restaurantId: String? = null,
    val tableNumber: String? = null,
    val seatingCapacity: Int? = null,
    val status: String? = null,
    val assignedCaptainId: String? = null,
    val currentOrderId: String? = null
)

data class TablesResponse(
    val success: Boolean? = null,
    val tables: List<TableDto>? = null,
    val data: List<TableDto>? = null
)

class TablesResponseAdapter {
    @com.squareup.moshi.FromJson
    fun fromJson(reader: com.squareup.moshi.JsonReader): TablesResponse {
        val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
        val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, TableDto::class.java)
        val listAdapter = moshi.adapter<List<TableDto>>(listType)
        
        return if (reader.peek() == com.squareup.moshi.JsonReader.Token.BEGIN_ARRAY) {
            val list = listAdapter.fromJson(reader)
            TablesResponse(success = true, tables = list)
        } else {
            val mapAdapter = moshi.adapter<Map<String, Any>>(com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))
            val map = mapAdapter.fromJson(reader)
            val success = (map?.get("success") as? Boolean) ?: true
            val rawList = (map?.get("tables") ?: map?.get("data")) as? List<*>
            val tables = if (rawList != null) {
                val listString = moshi.adapter(Any::class.java).toJson(rawList)
                listAdapter.fromJson(listString)
            } else {
                null
            }
            TablesResponse(success = success, tables = tables)
        }
    }

    @com.squareup.moshi.ToJson
    fun toJson(writer: com.squareup.moshi.JsonWriter, value: TablesResponse?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("success").value(value.success)
        writer.name("tables")
        if (value.tables != null) {
            val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, TableDto::class.java)
            moshi.adapter<List<TableDto>>(listType).toJson(writer, value.tables)
        } else {
            writer.nullValue()
        }
        writer.endObject()
    }
}
