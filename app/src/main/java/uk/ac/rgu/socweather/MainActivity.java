package uk.ac.rgu.socweather;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import uk.ac.rgu.socweather.data.HourForecast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create some sample data
        HourForecast hf = new HourForecast();
        hf.setTemperature(32);
    }
}