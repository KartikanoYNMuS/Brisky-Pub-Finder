package com.example.myplaces;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_FINE_LOCATION = 101;
    private static final int PLACE_PICKER_REQUEST = 1;
    TextView placeNameText;
    TextView placeAddressText;
    WebView attributionText;
    Button getPlaceButton;
    String ResName[];
    String ResVis[];
    int ResSize;
    private GoogleMap mGoogleMap;
 //   private final static LatLngBounds bounds = new LatLngBounds(new LatLng(51.5152192,-0.1321900), new LatLng(51.5166013,-0.1299262));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

      requestPermissions();

      placeNameText = (TextView) findViewById(R.id.tvPlaceName);
      placeAddressText = (TextView) findViewById(R.id.tvPlaceAddress);
      attributionText  =(WebView) findViewById(R.id.wvAttribution);
      getPlaceButton =(Button) findViewById(R.id.btGetPlace);
      getPlaceButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              PlacePicker.IntentBuilder builder= new PlacePicker.IntentBuilder();

           //   builder.setLatLngBounds(bounds);
              try {
                  Intent intent=builder.build(MainActivity.this);
                  startActivityForResult(intent, PLACE_PICKER_REQUEST);
              } catch (GooglePlayServicesRepairableException e) {
                  e.printStackTrace();
              } catch (GooglePlayServicesNotAvailableException e) {
                  e.printStackTrace();
              }
          }
      });
    }
    private void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_FINE_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case MY_PERMISSION_FINE_LOCATION:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(getApplicationContext(),"This app reguires location permissions to be granted",Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
      if(requestCode == PLACE_PICKER_REQUEST){
          if(resultCode == RESULT_OK){
              Place place = PlacePicker.getPlace(MainActivity.this,data);
              double latitude=place.getLatLng().latitude;
              double longitude=place.getLatLng().longitude;
              StringBuilder sbValue = new StringBuilder(sbMethod(latitude,longitude));
              PlacesTask placesTask = new PlacesTask();
              placesTask.execute(sbValue.toString());

              placeNameText.setText(place.getName());

              placeAddressText.setText(place.getAddress());


              if(place.getAttributions() ==null){
                  attributionText.loadData("no attribution", "text/html; charset=utf-8","UFT-8");
              }
              else{
                  attributionText.loadData(place.getAttributions().toString(), "text/html; charset=utf-8","UFT-8");
              }
          }
      }
    }
    public StringBuilder sbMethod(double mLatitude,double mLongitude) {
        StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        sb.append("location=" + mLatitude + "," + mLongitude);
        sb.append("&radius=2000");
        //sb.append("&type=" + "restaurant");
        sb.append("&type=" + "night_club");
        sb.append("&sensor=true");
        sb.append("&key=AIzaSyBJBPDSswjOAMQ6OWphzVjYi5qcZD_mlY4");
        Log.d("Map", "api: " + sb.toString());
        return sb;
    }
    private class PlacesTask extends AsyncTask<String, Integer, String> {

        String data = null;

        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url) {
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result) {
            ParserTask parserTask = new ParserTask();

            // Start parsing the Google places in JSON format
            // Invokes the "doInBackground()" method of the class ParserTask
            parserTask.execute(result);
        }
    }
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception ", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String, String>>> {

        JSONObject jObject;

        // Invoked by execute() method of this object
        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;
            Place_JSON placeJson = new Place_JSON();

            try {
                jObject = new JSONObject(jsonData[0]);

                places = placeJson.parse(jObject);

            } catch (Exception e) {
                Log.d("Exception", e.toString());
            }
            return places;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(List<HashMap<String, String>> list) {

            ResSize=list.size();
            // Clears all the existing markers;
          //  mGoogleMap.clear();
            ResName=new String[ResSize];
            ResVis=new String[ResSize];
            for (int i = 0; i < list.size(); i++) {
                // Getting a place from the places list
                HashMap<String, String> hmPlace = list.get(i);


                // Getting latitude of the place
           //     double lat = Double.parseDouble(hmPlace.get("lat"));

                // Getting longitude of the place
           //     double lng = Double.parseDouble(hmPlace.get("lng"));

                // Getting name
                 ResName[i] = hmPlace.get("place_name");

                // Getting vicinity
                  ResVis[i]= hmPlace.get("vicinity");

              //  LatLng latLng = new LatLng(lat, lng);
            }
            ArrayAdapter adapter = new ArrayAdapter<String>(MainActivity.this,
                    R.layout.activity_listview, ResName);

            ListView listView = (ListView) findViewById(R.id.res_list);
            listView.setAdapter(adapter);
        }
    }
    public class Place_JSON {

        /**
         * Receives a JSONObject and returns a list
         */
        public List<HashMap<String, String>> parse(JSONObject jObject) {

            JSONArray jPlaces = null;
            try {
                /** Retrieves all the elements in the 'places' array */
                jPlaces = jObject.getJSONArray("results");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            /** Invoking getPlaces with the array of json object
             * where each json object represent a place
             */
            return getPlaces(jPlaces);
        }

        private List<HashMap<String, String>> getPlaces(JSONArray jPlaces) {
            int placesCount = jPlaces.length();
            List<HashMap<String, String>> placesList = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> place = null;

            /** Taking each place, parses and adds to list object */
            for (int i = 0; i < placesCount; i++) {
                try {
                    /** Call getPlace with place JSON object to parse the place */
                    place = getPlace((JSONObject) jPlaces.get(i));
                    placesList.add(place);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return placesList;
        }

        /**
         * Parsing the Place JSON object
         */
        private HashMap<String, String> getPlace(JSONObject jPlace) {

            HashMap<String, String> place = new HashMap<String, String>();
            String placeName = "-NA-";
            String vicinity = "-NA-";
            String latitude = "";
            String longitude = "";
            String reference = "";

            try {
                // Extracting Place name, if available
                if (!jPlace.isNull("name")) {
                    placeName = jPlace.getString("name");
                }

                // Extracting Place Vicinity, if available
                if (!jPlace.isNull("vicinity")) {
                    vicinity = jPlace.getString("vicinity");
                }

                latitude = jPlace.getJSONObject("geometry").getJSONObject("location").getString("lat");
                longitude = jPlace.getJSONObject("geometry").getJSONObject("location").getString("lng");
                reference = jPlace.getString("reference");

                place.put("place_name", placeName);
                place.put("vicinity", vicinity);
                place.put("lat", latitude);
                place.put("lng", longitude);
                place.put("reference", reference);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return place;
        }
    }
}
