package com.example.data.remote

import android.util.Log
import com.example.domain.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

class SupabaseRealtimeClient(
    private val client: OkHttpClient,
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var reconnectDelay = 2000L

    val realtimeEventFlow = MutableSharedFlow<String>(extraBufferCapacity = 100)

    fun connect(supabaseUrl: String, apiKey: String, restaurantCode: String) {
        val trimmedUrl = supabaseUrl.trim()
        val trimmedKey = apiKey.trim()
        if (trimmedUrl.isEmpty() || trimmedKey.isEmpty() || 
            (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) || 
            trimmedUrl.contains("SUPABASE_URL") || 
            trimmedUrl.contains("your-supabase-project") || 
            trimmedKey.contains("SUPABASE_KEY")) {
            Log.w("SupabaseRealtime", "Supabase credentials empty or invalid placeholders. Realtime not connected.")
            return
        }

        disconnect()

        val wsUrl = trimmedUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/realtime/v1/websocket?apikey=$trimmedKey&vsn=1.0.0"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                reconnectDelay = 2000L
                Log.i("SupabaseRealtime", "WebSocket opened successfully")
                
                // Subscribe to postgres changes
                joinChannel(webSocket, "realtime:public:tables", "tables", restaurantCode)
                joinChannel(webSocket, "realtime:public:orders", "orders", restaurantCode)
                joinChannel(webSocket, "realtime:public:kots", "kots", restaurantCode)
                joinChannel(webSocket, "realtime:public:bills", "bills", restaurantCode)
                joinChannel(webSocket, "realtime:public:settlements", "settlements", restaurantCode)

                startHeartbeat(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    realtimeEventFlow.emit(text)
                }
                Log.d("SupabaseRealtime", "Received message: $text")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.w("SupabaseRealtime", "WebSocket closing: $reason (Code: $code)")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e("SupabaseRealtime", "WebSocket failure: ${t.message}", t)
                retryConnection(supabaseUrl, apiKey, restaurantCode)
            }
        })
    }

    private fun joinChannel(webSocket: WebSocket, topic: String, tableName: String, restaurantCode: String) {
        // Construct the phoenix join channel request
        val joinPayload = """
            {
              "topic": "$topic",
              "event": "phx_join",
              "payload": {
                "config": {
                  "postgres_changes": [
                    {
                      "event": "*",
                      "schema": "public",
                      "table": "$tableName",
                      "filter": "restaurant_code=eq.$restaurantCode"
                    }
                  ]
                }
              },
              "ref": "$tableName"
            }
        """.trimIndent()
        webSocket.send(joinPayload)
    }

    private fun startHeartbeat(webSocket: WebSocket) {
        scope.launch {
            var refIndex = 1
            while (isConnected && isActive) {
                delay(30000)
                val heartbeat = """
                    {"topic":"phoenix","event":"heartbeat","payload":{},"ref":"h-$refIndex"}
                """.trimIndent()
                try {
                    webSocket.send(heartbeat)
                    refIndex++
                } catch (e: Exception) {
                    Log.e("SupabaseRealtime", "Heartbeat failed", e)
                }
            }
        }
    }

    private fun retryConnection(supabaseUrl: String, apiKey: String, restaurantCode: String) {
        scope.launch {
            delay(reconnectDelay)
            reconnectDelay = (reconnectDelay * 1.5).toLong().coerceAtMost(60000L)
            Log.i("SupabaseRealtime", "Reconnecting WebSocket... delay is ${reconnectDelay}ms")
            connect(supabaseUrl, apiKey, restaurantCode)
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        isConnected = false
    }
}
