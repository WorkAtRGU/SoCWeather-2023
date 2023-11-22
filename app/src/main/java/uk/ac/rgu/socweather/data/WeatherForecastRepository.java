package uk.ac.rgu.socweather.data;

import android.content.Context;

import java.util.List;

public class WeatherForecastRepository {

    private HourForecastDAO mHourForecastDAO;

    public WeatherForecastRepository(Context context){
        super();
        mHourForecastDAO = WeatherDatabase.getInstance(context).hourForecastDAO();
    }

    /**
     * Stores hourForecasts locally on the device in a RoomSQL database.
     * @param hourForecasts
     */
    public void storeHourForecasts(List<HourForecast> hourForecasts){
        this.mHourForecastDAO.insert(hourForecasts);
    }

    /**
     * Deletes hourForecasts locally on the device in a RoomSQL database.
     * @param hourForecasts
     */
    public void deleteHourForecasts(List<HourForecast> hourForecasts){
        this.mHourForecastDAO.delete(hourForecasts);
    }


    /**
     * Returns any HourForecasts stored for the location and date
     * @param location
     * @param date
     */
    public List<HourForecast> getHourForecasts(String location, String date){
        return this.mHourForecastDAO.findByLocationDate(location, date);
    }
}
