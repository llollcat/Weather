package com.mirea.weather


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.util.concurrent.Executors
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity() {
    private val groupName = "cites"
    private val groupDataList = arrayListOf<HashMap<String, String>>()

    private val childName = "weather"
    private val childDataList = arrayListOf<ArrayList<HashMap<String, String>>>()

    private var latitude = arrayListOf<String>()
    private var longitude = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)


        // Загрузка данных
        for (i in 0 until 7) {
            val pref = getSharedPreferences(i.toString(), Context.MODE_PRIVATE)
            if (pref.contains("0")) {
                groupDataList.add(hashMapOf(groupName to pref.getString("0", "").toString()))


                val list = arrayListOf<HashMap<String, String>>()
                for (i2 in 1..9) {

                    list.add(hashMapOf(childName to pref.getString(i2.toString(), "").toString()))

                }
                childDataList.add(list)
                latitude.add(pref.getString("10", "").toString())
                longitude.add(pref.getString("11", "").toString())

            }
        }
        for (i in groupDataList.size until 7) {
            val pref = getSharedPreferences(i.toString(), Context.MODE_PRIVATE)
            pref.edit().clear().apply()
        }


        val adapter = SimpleExpandableListAdapter(
                this, groupDataList,
                android.R.layout.simple_expandable_list_item_1, arrayOf(groupName), intArrayOf(android.R.id.text1),
                childDataList,
                android.R.layout.simple_list_item_1, arrayOf(childName), intArrayOf(android.R.id.text1))
        val expandableListView = findViewById<ExpandableListView>(R.id.cites)
        val emptyText = findViewById<TextView>(R.id.empty_text_view)
        expandableListView.emptyView = emptyText
        expandableListView.setAdapter(adapter)


        // листенер на взаимодействие с кнопками "Удалить из избранного" и "Открыть на карте"
        expandableListView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            // открытие карт
            if (childPosition == 7) {
                                val uri = "geo:" + latitude[groupPosition] + "," + longitude[groupPosition]

                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                startActivity(Intent.createChooser(mapIntent, "Открыть с помощью:"))

            }


            // удаление из избранного
            if (childPosition == 8) {
                val cites = prefs.getStringSet("cites", setOf())?.toHashSet()
                if (cites != null) {
                    var cityToDelete = String()
                    // получение названия города
                    for (i in groupDataList[groupPosition][groupName].toString()) {
                        if (i == ':')
                            break
                        cityToDelete += i.toString()
                    }

                    cites.remove(cityToDelete)
                    groupDataList.removeAt(groupPosition)
                    childDataList.removeAt(groupPosition)
                    emptyText.text = "Нет избранных городов"
                    adapter.notifyDataSetChanged()

                    prefs.edit().putStringSet("cites", cites.toSet()).apply()

                }
            }
            true
        }


        // обновление информации о погоде
        fun refresh() {

            if (hasInternet(this)) {
                if (prefs.contains("cites")) {
                    val cites: MutableSet<String>? = prefs.getStringSet("cites", hashSetOf())




                    if ((cites != null)) {
                        if (cites.size != 0) {
                            // для создания треда
                            val executor = Executors.newSingleThreadExecutor()
                            val handler = Handler(Looper.getMainLooper())
                            groupDataList.clear()
                            childDataList.clear()
                            emptyText.text = ""
                            adapter.notifyDataSetChanged()
                            latitude.clear()
                            longitude.clear()

                            for (city in cites)
                                executor.execute {
                                    val output = getWeatherDaily(city, this)

                                    handler.post {
                                        if (output.size != 1) {
                                            groupDataList.add(hashMapOf(groupName to "$city:\n" + days[0] + output[0] + "°"))
                                            val list = arrayListOf<HashMap<String, String>>()
                                            for (i in 1..7) {
                                                list.add(hashMapOf(childName to "      " + days[i] + output[i] + "°"))
                                            }
                                            list.add(hashMapOf(childName to "      " + "Открыть на карте"))
                                            list.add(hashMapOf(childName to "      " + "Удалить из избранного"))
                                            longitude.add(output[8])
                                            latitude.add(output[9])

                                            childDataList.add(list)
                                            adapter.notifyDataSetChanged()


                                        } else {
                                            Toast.makeText(this, "Отсутствует подключение", Toast.LENGTH_LONG).show()
                                            emptyText.text = "Нет избранных городов"
                                        }
                                    }
                                }

                        } else
                            Toast.makeText(this, "Нет избранных городов", Toast.LENGTH_LONG).show()
                    }
                } else
                    Toast.makeText(this, "Нет избранных городов", Toast.LENGTH_LONG).show()
            } else
                Toast.makeText(this, "Отсутствует подключение", Toast.LENGTH_LONG).show()


        }


        // листенер на жест обновления
        val swipe = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipe.setColorSchemeResources(android.R.color.holo_red_light)
        swipe.setOnRefreshListener {
            refresh()
            swipe.isRefreshing = false
        }


    }


    // кнопка +
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_plus, menu)
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_plus) {

            val intent = Intent(this, CitySelectorActivity::class.java)
            startActivity(intent)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()


        // Сохранение данных
        for (i in 0 until groupDataList.size) {
            val pref = getSharedPreferences(i.toString(), Context.MODE_PRIVATE)

            pref.edit().putString("0", groupDataList[i][groupName]).apply()
            for (i2 in 1..9) {

                pref.edit().putString(i2.toString(), childDataList[i][i2 - 1][childName]).apply()

            }

            pref.edit().putString("10", latitude[i]).apply()
            pref.edit().putString("11", longitude[i]).apply()


        }
        for (i in groupDataList.size until 7) {
            val pref = getSharedPreferences(i.toString(), Context.MODE_PRIVATE)
            pref.edit().clear().apply()
        }


    }
}