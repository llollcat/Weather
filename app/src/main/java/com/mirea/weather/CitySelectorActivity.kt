package com.mirea.weather


import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.View.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import java.util.concurrent.Executors


class CitySelectorActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_city_selector)


        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        var city = "0"

        val input = findViewById<EditText>(R.id.city_name_input)

        val weather = findViewById<ListView>(R.id.weather_list)
        weather.onItemClickListener = OnItemClickListener { _, _, childPosition, _ ->
            // добавление в избранное
            if (childPosition == 8) {
                if (prefs.contains("cites")) {
                    val cites = prefs.getStringSet("cites", setOf())?.toHashSet()
                    if (cites != null) {
                        if (cites.size > 6)
                            Toast.makeText(this, "Избранных городов не может быть больше 7", Toast.LENGTH_LONG).show()
                        else {
                            cites.add(city)

                            Toast.makeText(this, "Добавленно", Toast.LENGTH_LONG).show()
                            prefs.edit().putStringSet("cites", cites.toSet()).apply()
                        }
                    }
                } else
                    prefs.edit().putStringSet("cites", setOf(city)).apply()
            }
        }
        val arr = arrayListOf<String>()
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arr)
        weather.adapter = adapter


        input.setOnKeyListener(object : OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                // нажание на enter для поиска
                if (event.action == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_ENTER
                ) {


                    if (!hasInternet(this@CitySelectorActivity)) {
                        Toast.makeText(this@CitySelectorActivity, "Отсутствует подключение", Toast.LENGTH_LONG).show()
                        return true
                    }


                    city = input.text.toString()
                    city = city[0].toUpperCase().toString() + city.slice(1 until city.length)
                    val executor = Executors.newSingleThreadExecutor()
                    val handler = Handler(Looper.getMainLooper())


                    executor.execute {
                        val output = getWeatherDaily(input.text.toString(), this@CitySelectorActivity)
                        arr.clear()
                        if (output.size != 1) {

                            for (i in (0..7)) {
                                arr.add(days[i] + output[i] + "°")

                            }

                            arr.add("Добавить в избранное")


                        } else {
                            arr.add("Город не найден")
                        }
                        handler.post {
                            adapter.notifyDataSetChanged()

                        }
                    }
                    return true
                }
                return false
            }
        })


    }


}