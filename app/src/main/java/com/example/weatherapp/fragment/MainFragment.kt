package com.example.weatherapp.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weatherapp.DialogManager
import com.example.weatherapp.fragment.HoursFragment
import com.example.weatherapp.MainViewModel
import com.example.weatherapp.adapters.VpAdapter
import com.example.weatherapp.adapters.WeatherModel
import com.example.weatherapp.databinding.FragmentMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import org.json.JSONObject

const val API_KEY = "e8a079eacf27470bbcb60232242806"

class MainFragment : Fragment() {
    private lateinit var fLocationClient: FusedLocationProviderClient
    private val fList = listOf(
        HoursFragment.newInstance(),
        DaysFragment.newInstance()
    )
    private val tList = listOf(
        "Hours",
        "Days"
    )
    private val model: MainViewModel by activityViewModels()
    private lateinit var pLauncher: ActivityResultLauncher<String>
    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        checkPermission()
        updateCurrentCard()
        init()

    }

    private fun updateCurrentCard() = with(binding) {
        model.liveDataCurrent.observe(viewLifecycleOwner) {
            val maxMinTemp = "${it.maxTemp}°/${it.minTemp}°"
            tvData.text = it.time
            tvCity.text = it.city
            tvCurrentTemp.text = it.currentTemp.ifEmpty { maxMinTemp }
            tvCondition.text = it.condition
            tvMaxMin.text = if (it.currentTemp.isEmpty()) "" else maxMinTemp
            Picasso.get().load("https:" + it.imageUrl).into(imageView2)
            tvSunrise.text = it.sunrise
            tvSunset.text = it.sunset
            tvCountry.text = it.country
            tvMoonset.text= it.moonset
            tvMoonrise.text= it.moonrise
        }
    }

    private fun requestWeatherData(city: String) {
        val url = "http://api.weatherapi.com/v1/forecast.json?key=" +
                "$API_KEY" +
                "&q=" +
                "$city" +
                "&days=10" +
                "&aqi=no&alerts=no"
        val queue = Volley.newRequestQueue(context)
        val request = StringRequest(
            Request.Method.GET,
            url,
            { result -> parseWeatherData(result) },
            { error -> Log.e("1234", "Error: $error")
            Toast.makeText(context, "Напишите город правильно: $error", Toast.LENGTH_SHORT).show()}
        )
        queue.add(request)
    }

    private fun parseWeatherData(result: String) {
        val mainObject = JSONObject(result)
        val list = parseDays(mainObject)
        parseCurrentData(mainObject, list[0])
    }

    private fun parseDays(mainObject: JSONObject): List<WeatherModel> {
        val list = ArrayList<WeatherModel>()
        val daysArray = mainObject.getJSONObject("forecast").getJSONArray("forecastday")
        val name = mainObject.getJSONObject("location").getString("name")
        val country = mainObject.getJSONObject("location").getString("country")
        for (i in 0 until daysArray.length()) {
            val day = daysArray[i] as JSONObject
            val item = WeatherModel(
                name,
                day.getString("date"),
                day.getJSONObject("day").getJSONObject("condition").getString("text"),
                currentTemp = "",
                day.getJSONObject("day").getString("maxtemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getString("mintemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getJSONObject("condition").getString("icon"),
                day.getJSONArray("hour").toString(),
                day.getJSONObject("astro").getString("sunrise"),
                day.getJSONObject("astro").getString("sunset"),
                country,
                day.getJSONObject("astro").getString("moonrise"),
                day.getJSONObject("astro").getString("moonset")
            )
            list.add(item)
        }
        model.liveDataList.value = list
        return list
    }

    private fun parseCurrentData(mainObject: JSONObject, weatherItem: WeatherModel) {
        val locationObject = mainObject.getJSONObject("location")
        val currentObject = mainObject.getJSONObject("current")
        val astroObject = mainObject.getJSONObject("forecast").getJSONArray("forecastday")
            .getJSONObject(0).getJSONObject("astro")

        val item = WeatherModel(
            locationObject.getString("name"),
            currentObject.getString("last_updated"),
            currentObject.getJSONObject("condition").getString("text"),
            currentObject.getString("temp_c").toFloat().toInt().toString(),
            weatherItem.maxTemp,
            weatherItem.minTemp,
            currentObject.getJSONObject("condition").getString("icon"),
            weatherItem.hours,
            astroObject.getString("sunrise"),
            astroObject.getString("sunset"),
            locationObject.getString("country"),
            astroObject.getString("moonrise"),
            astroObject.getString("moonset")
        )
        model.liveDataCurrent.value = item
    }
    override fun onResume() {
        super.onResume()
        checkLocation()
    }


    private fun getLocation() {
        val ct = CancellationTokenSource()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!isLocationEnabled()) {
            Toast.makeText(context, "Please enable location services", Toast.LENGTH_LONG).show()
            return
        }
        if (isGooglePlayServicesAvailable()) {
            fLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, ct.token)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val location = it.result
                        if (location != null) {
                            requestWeatherData("${location.latitude},${location.longitude}")
                        } else {
                            Log.e("1234", "Location is null")
                            Toast.makeText(context, "Unable to determine location", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e("1234", "Failed to get location: ${it.exception}")
                        Toast.makeText(context, "Failed to get location", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(context, "Google Play Services not available", Toast.LENGTH_LONG).show()
        }
    }

    private fun init() = with(binding) {
        if (isGooglePlayServicesAvailable()) {
            fLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            val adapter = VpAdapter(activity as FragmentActivity, fList)
            vp.adapter = adapter
            TabLayoutMediator(tabLayout, vp) { tab, pos ->
                tab.text = tList[pos]
            }.attach()
            ibSync.setOnClickListener {
                tabLayout.selectTab(tabLayout.getTabAt(0))
                checkLocation()
            }
        } else {
            Toast.makeText(context, "Google Play Services not available", Toast.LENGTH_LONG).show()
        }
        ibSearch.setOnClickListener{
            DialogManager.searchByNameDialog(requireContext(), object : DialogManager.Listener{
                override fun onClick(name: String?) {
                    name?.let { it1 -> requestWeatherData(it1) }
                }

            })
        }

    }

    private fun checkLocation(){
        if(isLocationEnabled()){
            getLocation()
        } else {
            DialogManager.locationSettingsDialog(requireContext(), object : DialogManager.Listener{
                override fun onClick(name: String?) {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            })
        }
    }

    private fun permissionListener() {
        pLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            Toast.makeText(activity, "Permission is $it", Toast.LENGTH_LONG).show()
            Log.e("1234", "Permission: $it")
        }
    }

    private fun checkPermission() {
        if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val lm = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(requireContext())
        return resultCode == ConnectionResult.SUCCESS
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}
