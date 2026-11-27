package com.example.paymentreceiver.repositories


import com.example.paymentreceiver.data.PaymentMessage
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class RestRepository {
    companion object{

        const val APPLICATION_ID = "9W3lYkeIpf3JWrjroH4bE9mL5HPC9ZFybVdLgOLn"
        const val CLIENT_KEY = "wPNTqR18DRbZr7UK8jfaIQItw7t7q9xiUX96AWMe"

    }

    fun query(params: HashMap<String, Any>, listener: OnQueryListener){
        val client: OkHttpClient = OkHttpClient.Builder().build()
        val url = "https://parseapi.back4app.com/functions/updateTransactionStatus"


        val mediaType = "application/json;charset=utf-8".toMediaType()
        val requestBody = JSONObject(params).toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Parse-Application-Id", APPLICATION_ID)
            .addHeader("X-Parse-REST-API-Key", CLIENT_KEY)
            .build()
        client.newCall(request).enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
//                    e.localizedMessage?.let { listener.onError(it)}
                println("error withing enqueue...${e.localizedMessage}")
                call.cancel()
                listener.onError(e.localizedMessage)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string().toString()
                val json = JSONObject(responseBody)

                if (json.has("error")){
                    println("error....$json")
                    listener.onError(json.getString("error").toString())
                }else{
                    listener.onSuccess(json.getString("result").toString())
                }

            }
        })

    }

    interface OnQueryListener{
        fun onSuccess(result: String)
        fun onError(error: String?)

    }
}