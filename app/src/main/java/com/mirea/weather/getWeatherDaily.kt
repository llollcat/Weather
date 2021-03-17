package com.mirea.weather

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import android.util.Log
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private const val APIKEY = "36ae5d263ec1cda6de5d556fac7beb1b"


fun getWeatherDaily(city: String, context: Context): ArrayList<String> {

    if (!hasInternet(context))
        return arrayListOf("-1")
    val output = arrayListOf<String>()
    val lon: String
    val lat: String
    var api = "https://api.openweathermap.org/data/2.5/weather?q=$city&units=metric&appid=$APIKEY"
    var url = URL(api)
    var connectionApi = url.openConnection() as HttpsURLConnection
    connectionApi.disconnect()



    if (connectionApi.responseCode == 200) {
        val responseBodyReader = InputStreamReader(connectionApi.inputStream, "UTF-8")

        val jsonReader = JsonReader(responseBodyReader)
        jsonReader.beginObject()
        jsonReader.nextName()
        jsonReader.beginObject()
        jsonReader.nextName()
        lon = jsonReader.nextString()
        jsonReader.nextName()
        lat = jsonReader.nextString()
        jsonReader.close()


    } else {

        output.add(connectionApi.responseCode.toString())
        return output
    }




    api = "https://api.openweathermap.org/data/2.5/onecall?lat=$lat&lon=$lon&units=metric&exclude=current,minutely,hourly,alerts&appid=$APIKEY"
    url = URL(api)
    connectionApi = url.openConnection() as HttpsURLConnection
    connectionApi.disconnect()
    if (connectionApi.responseCode == 200) {
        val responseBodyReader = InputStreamReader(connectionApi.inputStream, "UTF-8")


        val jsonReader = JsonReader(responseBodyReader)
        jsonReader.beginObject() //
        var counter = 0


        while (jsonReader.peek() != JsonToken.END_DOCUMENT) {
            val key: String

            when (jsonReader.peek()) {
                JsonToken.NAME -> {
                    key = jsonReader.nextName()

                    if (key.equals("temp")) {
                        jsonReader.beginObject()
                        jsonReader.skipValue()


                        output.add(jsonReader.nextString())

                        ++counter

                    }
                }
                JsonToken.END_OBJECT -> jsonReader.endObject()
                JsonToken.BEGIN_OBJECT -> jsonReader.beginObject()
                JsonToken.BEGIN_ARRAY -> jsonReader.beginArray()
                JsonToken.END_ARRAY -> jsonReader.endArray()
                else -> jsonReader.skipValue()

            }


        }


    } else {

        output.add(connectionApi.responseCode.toString())
        return output
    }


    output += lon
    output += lat
    return output
}