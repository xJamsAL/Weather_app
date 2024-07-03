package com.example.weatherapp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherapp.MainViewModel
import com.example.weatherapp.adapters.WeatherAdapter
import com.example.weatherapp.adapters.WeatherModel
import com.example.weatherapp.databinding.FragmentHoursBinding
import org.json.JSONArray
import org.json.JSONObject

class HoursFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentHoursBinding
    private lateinit var adapter: WeatherAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHoursBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRcView()
        observeViewModel()
    }

    private fun initRcView() {
        binding.rcView.layoutManager = LinearLayoutManager(activity)
        adapter = WeatherAdapter(null)
        binding.rcView.adapter = adapter
    }

    private fun observeViewModel() {
        model.liveDataCurrent.observe(viewLifecycleOwner) { weatherItem ->
            if (weatherItem != null) {
                showProgressBar(false)
                adapter.submitList(getHoursList(weatherItem))
            } else {
                showProgressBar(true)
            }
        }
    }

    private fun getHoursList(wItem: WeatherModel): List<WeatherModel> {
        val hoursArray = JSONArray(wItem.hours)
        val list = ArrayList<WeatherModel>()
        for (i in 0 until hoursArray.length()) {

            val item = WeatherModel(
                wItem.city,
                (hoursArray[i] as JSONObject).getString("time"),
                (hoursArray[i] as JSONObject).getJSONObject("condition").getString("text"),
                (hoursArray[i] as JSONObject).getString("temp_c").toFloat().toInt().toString() + "°",
                "",
                "",
                (hoursArray[i] as JSONObject).getJSONObject("condition").getString("icon"),
                "",
                "",
                "",
                "",
                "",
                ""
            )
            list.add(item)
        }
        return list
    }

    private fun showProgressBar(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    companion object {
        @JvmStatic
        fun newInstance() = HoursFragment()
    }
}
