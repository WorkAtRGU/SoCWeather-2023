package uk.ac.rgu.socweather;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import uk.ac.rgu.socweather.data.HourForecast;
import uk.ac.rgu.socweather.data.Utils;
import uk.ac.rgu.socweather.data.WeatherForecastRepository;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ForecastFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ForecastFragment extends Fragment implements View.OnClickListener {

    // tag for loggging message
    private static final String TAG = "ForecastFragment";

    // parameter argument names
    public static final String ARG_PARAM_LOCATION = LocationConfirmationFragment.ARG_PARAM_LOCATION;
    public static final String ARG_PARAM_NUMBER_OF_DAYS = LocationConfirmationFragment.ARG_PARAM_NUMBER_OF_DAYS;

    // paramaters
    private String mLocation;
    private int mNumberOfDays;

    // for the data to be shown to the user
    private List<HourForecast> forecastList;

    // for the RecyclerView
    ForecastRecyclerViewAdapter mRecyclerViewAdapter;

    // repo for managing data locally
    private WeatherForecastRepository mWeatherForecastRepo;

    public ForecastFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param location The location to display the forecast for
     * @param numberOfDays The number of days to get the forecast for.
     * @return A new instance of fragment ForecastFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ForecastFragment newInstance(String location, int numberOfDays) {
        ForecastFragment fragment = new ForecastFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_LOCATION, location);
        args.putInt(ARG_PARAM_NUMBER_OF_DAYS, numberOfDays);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.mLocation = getArguments().getString(ARG_PARAM_LOCATION);
            this.mNumberOfDays = getArguments().getInt(ARG_PARAM_NUMBER_OF_DAYS);
        }

        this.mWeatherForecastRepo = new WeatherForecastRepository(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_forecast, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // update the tvForecastLabel text
        TextView tvForecastLabel = getActivity().findViewById(R.id.tvForecastLabel);
        tvForecastLabel.setText(getContext().getString(R.string.tvForecastLabelLoading,mLocation));

        // setup the RecyclerView
        RecyclerView rv = getActivity().findViewById(R.id.rvForecast);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        this.forecastList = new ArrayList<HourForecast>();
        mRecyclerViewAdapter = new ForecastRecyclerViewAdapter(getContext(), forecastList);
        rv.setAdapter(mRecyclerViewAdapter);

        // set the action handlers on the buttons
        Button btnShowMap = view.findViewById(R.id.btnShowLocationMap);
        btnShowMap.setOnClickListener(this);

        Button btnCheckForecastOnline = view.findViewById(R.id.btnCheckForecastOnline);
        btnCheckForecastOnline.setOnClickListener(this);

        Button btnShareForecast = view.findViewById(R.id.btnShareForecast);
        btnShareForecast.setOnClickListener(this);

        // check if we already have a forecast in the database, if so then load that
        for (int i = 0 ; i < mNumberOfDays ; i++) {
            Calendar now = Calendar.getInstance();
            now.add(Calendar.HOUR, (24 * i));
            String date = Utils.getWeatherForecastDateFormat(getContext()).format(now.getTime());

            forecastList.addAll(this.mWeatherForecastRepo.getHourForecasts(this.mLocation, date));
        }
        if (forecastList.size() == (24*mNumberOfDays)){
            // display the cached values
            ProgressBar pg = getActivity().findViewById(R.id.pb_forecastFragment);
            pg.setVisibility(View.GONE);

            displayForecastViews();
        } else {
            // downlaod and store
            downloadForecast();
        }
    }

    private void downloadForecast(){
        String url = String.format("https://api.weatherapi.com/v1/forecast.json?key=a3b9cc3fb35943d5826152257210311&q=%s&days=%d", this.mLocation, this.mNumberOfDays);

        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, response);

                // for storing all the weather forecast
                forecastList = new ArrayList<HourForecast>(24*5);

                // for formatting the date of the forecast
                SimpleDateFormat sdf = Utils.getWeatherForecastDateFormat(getContext());

                try {
                    // convert text response to a JSON object for processing
                    JSONObject rootObj = new JSONObject(response);

                    // get the location object
                    JSONObject locationObj = rootObj.getJSONObject("location");
                    String location = mLocation;
                    // TODO: would like to store the below, but won't work for
                    // retrival as will need to query using this string which
                    // is not what is passed from the preceeding fragements
//                            locationObj.getString("name") +
//                                    ", " + locationObj.getString("region") +
//                                    ", " + locationObj.getString("country");

                    // get the forecast value
                    JSONObject forecastObject = rootObj.getJSONObject("forecast");
                    // get the forecast day value - an array of days
                    JSONArray forecastDayArray = forecastObject.getJSONArray("forecastday");
                    // for every forecast day
                    for (int i = 0, j = forecastDayArray.length(); i<j; i++){
                        // get the day at position i
                        JSONObject dayObject = forecastDayArray.getJSONObject(i);
                        // from the day, get the hour array
                        JSONArray hourArray = dayObject.getJSONArray("hour");
                        for (int ii = 0, jj = hourArray.length(); ii < jj; ii++){
                            // get the forecast hour object
                            JSONObject forecastHourObject = hourArray.getJSONObject(ii);
                            // now extract the forecast info
                            double temperature = forecastHourObject.getDouble("temp_c");
                            int humidity = forecastHourObject.getInt("humidity");
                            // get the condition object to work out the weather description
                            JSONObject conditionObject = forecastHourObject.getJSONObject("condition");
                            // get the weather description
                            String weather = conditionObject.getString("text");
                            // get the weather icon
                            String weatherIcon = conditionObject.getString("icon");

                            // work out the date and time
                            long timeEpoch = forecastHourObject.getLong("time_epoch");
                            Calendar calendar = Calendar.getInstance();
                            // the time in the forecast json is in seconds, so convert to millisecond
                            calendar.setTimeInMillis(timeEpoch*1000);

                            int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
                            // format the date for display
                            String dayMonth = sdf.format(calendar.getTime());

                            // create the HourForecast domain object for this hour
                            HourForecast hourForecast = new HourForecast();
                            hourForecast.setTemperature(temperature);
                            hourForecast.setHumidity(humidity);
                            hourForecast.setWeather(weather);
                            hourForecast.setIconURL(weatherIcon);
                            hourForecast.setHour(hourOfDay);
                            hourForecast.setDate(dayMonth);
                            hourForecast.setLocation(location);

                            // add this hour forecast to the list
                            forecastList.add(hourForecast);
                        }
                    }
                } catch (JSONException e) {
                    // display an error on the Toast and label text view
                    Toast.makeText(getContext(), R.string.error_parsing_forecast, Toast.LENGTH_LONG );
                    ((TextView)getActivity().findViewById(R.id.tvForecastLabel)).setText(R.string.error_parsing_forecast);

                    Log.d(TAG, "Parsing JSON error" + e.getLocalizedMessage());
                    e.printStackTrace();
                } finally {
                    // do something the forecasts that have been downloaded
                    Log.e(TAG, "Downloaded " + forecastList.size() + " forecasts");
                    // remove the spinner
                    ProgressBar pg = getActivity().findViewById(R.id.pb_forecastFragment);
                    pg.setVisibility(View.GONE);

                    // if we have some data, then enable the relevant Views
                    if (forecastList.size() > 0) {

                        // store the forecast on the device
                        mWeatherForecastRepo.storeHourForecasts(forecastList);

                        // display the forecast list
                        displayForecastViews();

                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getContext(), R.string.error_downloading_forecast, Toast.LENGTH_LONG );
                ((TextView)getActivity().findViewById(R.id.tvForecastLabel)).setText(R.string.error_downloading_forecast);
                Log.e(TAG, "Error downloading " + error.getMessage());
            }
        });
        // create a new RequestQueue
        RequestQueue rq = Volley.newRequestQueue(getContext());
        // add the request to make it
        rq.add(request);
    }

    private void displayForecastViews(){
        mRecyclerViewAdapter.setHourForecasts(forecastList);
        mRecyclerViewAdapter.notifyDataSetChanged();

        RecyclerView rv = getActivity().findViewById(R.id.rvForecast);
        rv.setVisibility(View.VISIBLE);

        // update the tvForecastLabel text
        TextView tvForecastLabel = getActivity().findViewById(R.id.tvForecastLabel);
        tvForecastLabel.setText(getContext().getString(R.string.tvForecastLabel,mLocation));

        // enable the buttons for sharing
        getActivity().findViewById(R.id.btnShareForecast).setEnabled(true);
        getActivity().findViewById(R.id.btnShowLocationMap).setEnabled(true);
        getActivity().findViewById(R.id.btnCheckForecastOnline).setEnabled(true);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnShowLocationMap){
            // show the this.mLocation on a mapp app
            // want to build a URI for the form geo:0,0?q=mLocation
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("geo")
                    .authority("0,0")
                    .appendQueryParameter("q", mLocation);
            Uri geoLocation = builder.build();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(geoLocation);
            //if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
            //}
        } else if (view.getId() == R.id.btnCheckForecastOnline){
            // launch the web browser app loading a serach engine
            // with a URL searching for the weather at mLocation
            // such as https://www.bing.com/search?q=aberdeen+weather
            Uri webpage = Utils.buildUri("https://www.bing.com/search?", "q", this.mLocation + " weather");
            Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
            //if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
            //}
        } else if (view.getId() == R.id.btnShareForecast){
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            String message = "Forecast for " + mLocation;
            if (forecastList != null && forecastList.size() > 0){
                HourForecast firstHour = forecastList.get(0);
                message += String.format(": at %s it'll be %sC", firstHour.getHour(), String.valueOf(firstHour.getTemperature()));
            }
            intent.putExtra("sms_body", message);
            //if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
            //}
        }
    }
}