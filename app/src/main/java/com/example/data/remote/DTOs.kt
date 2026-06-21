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
    val currentOrderId: Any? = null
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

data class MenuItemDto(
    val id: String? = null,
    @com.squareup.moshi.Json(name = "_id") val underscoreId: String? = null,
    val name: String? = null,
    val category: String? = null,
    val price: Double? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val image: String? = null,
    val isAvailable: Any? = null
) {
    fun toDomain(): com.example.domain.model.MenuItem {
        val safeId = id ?: underscoreId ?: "unknown"
        val isAvail = when (isAvailable) {
            is Boolean -> isAvailable
            is String -> isAvailable.toBoolean()
            is Number -> isAvailable.toInt() != 0
            else -> true
        }
        return com.example.domain.model.MenuItem(
            id = safeId,
            name = name ?: "Unnamed Item",
            category = category ?: "Uncategorized",
            price = price ?: 0.0,
            description = description ?: "",
            imageUrl = imageUrl ?: image ?: "",
            isAvailable = isAvail
        )
    }

    companion object {
        fun fromDomain(item: com.example.domain.model.MenuItem) = MenuItemDto(
            id = item.id,
            name = item.name,
            category = item.category,
            price = item.price,
            description = item.description,
            imageUrl = item.imageUrl,
            isAvailable = item.isAvailable
        )
    }
}

data class OrderItemDto(
    val id: String? = null,
    @com.squareup.moshi.Json(name = "_id") val underscoreId: String? = null,
    val menuItemId: String? = null,
    val name: String? = null,
    val price: Double? = null,
    val quantity: Int? = null
) {
    fun toDomain(): com.example.domain.model.OrderItem {
        return com.example.domain.model.OrderItem(
            id = id ?: underscoreId ?: "unknown_item",
            menuItemId = menuItemId ?: "unknown_menu_item",
            name = name ?: "Unnamed Item",
            price = price ?: 0.0,
            quantity = quantity ?: 0
        )
    }
}

data class OrderDto(
    val id: String? = null,
    @com.squareup.moshi.Json(name = "_id") val underscoreId: String? = null,
    val tableId: String? = null,
    val items: List<OrderItemDto>? = null,
    val status: String? = null,
    val captainId: String? = null,
    val totalAmount: Double? = null,
    val createdAt: Any? = null
) {
    fun toDomain(): com.example.domain.model.Order {
        val parsedCreated = when (createdAt) {
            is Number -> createdAt.toLong()
            is String -> {
                try {
                    createdAt.toLongOrNull() ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            }
            else -> System.currentTimeMillis()
        }
        return com.example.domain.model.Order(
            id = id ?: underscoreId ?: "unknown_order",
            tableId = tableId ?: "",
            items = items?.map { it.toDomain() } ?: emptyList(),
            status = status ?: "ACTIVE",
            captainId = captainId ?: "unknown_captain",
            totalAmount = totalAmount ?: 0.0,
            createdAt = parsedCreated
        )
    }
}

data class OrderResponse(
    val success: Boolean? = null,
    val order: OrderDto? = null,
    val data: OrderDto? = null
)

fun parseOrderResponse(rawJson: String): com.example.domain.model.Order {
    val moshi = com.squareup.moshi.Moshi.Builder()
        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()
    
    // 1. Try to parse directly as OrderDto
    try {
        val directAdapter = moshi.adapter(OrderDto::class.java)
        val orderDto = directAdapter.fromJson(rawJson)
        if (orderDto != null && (orderDto.id != null || orderDto.underscoreId != null)) {
            return orderDto.toDomain()
        }
    } catch (e: Exception) {
        // Fallback
    }

    // 2. Try to parse as OrderResponse (wrapped in success/order/data)
    try {
        val responseAdapter = moshi.adapter(OrderResponse::class.java)
        val wrapped = responseAdapter.fromJson(rawJson)
        val orderDto = wrapped?.order ?: wrapped?.data
        if (orderDto != null) {
            return orderDto.toDomain()
        }
    } catch (e: Exception) {
        // Fallback
    }

    // 3. Fallback to generic map/nested parsing
    val mapAdapter = moshi.adapter<Map<String, Any>>(
        com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )
    val map = mapAdapter.fromJson(rawJson)
    val orderRaw = map?.get("order") ?: map?.get("data") ?: map
    val orderJson = moshi.adapter(Any::class.java).toJson(orderRaw)
    val orderDto = moshi.adapter(OrderDto::class.java).fromJson(orderJson)
    return orderDto?.toDomain() ?: throw com.squareup.moshi.JsonDataException("Could not parse Order from JSON: $rawJson")
}
