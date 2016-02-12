package jou.happyhour.android.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by root on 6/02/16.
 */
public class CosasFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    ArrayAdapter<String> mCosasAdapter;
    private final String LOG_TAG = CosasFragment.class.getSimpleName();
    private String fecha = null;
    private SwipeRefreshLayout swipeRefreshLayout;

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public CosasFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.cosasfragment, menu);
    }

    @Override
    public void onRefresh() {
        updateTime();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateTime();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Función para darle la vuelta a la fecha
    public String formatDate(String fecha) {
        try {
            String pattern = "(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2})";
            Pattern MY_PATTERN = Pattern.compile(pattern);
            Matcher m = MY_PATTERN.matcher(fecha);

            if (m.find()) {
                // Parece ser que la hora aparece con un minuto de retraso, así que le sumo un minuto aquí
                Integer min = Integer.parseInt(m.group(5));
                min=(min+1)%60;
                if(min>=0 && min<=9){
                    // Se le añade un cero a los números de una cifra
                    StringBuffer s = new StringBuffer(min);
                    s.append(0).toString();
                    return m.group(3) + "-" + m.group(2) + "-" + m.group(1) + " " + m.group(4) + ":" + s.append(min).toString();
                }
                return m.group(3) + "-" + m.group(2) + "-" + m.group(1) + " " + m.group(4) + ":" + min.toString();
            }
            else return "";
        } catch (java.lang.NullPointerException e) {
            return getResources().getString(R.string.erroor_toast);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        mCosasAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_cosas, // The name of the layout ID.
                        R.id.list_item_cosas_textview, // The ID of the textview to populate.
                        new ArrayList<String>());
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_cosas);
        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_layout);
        listView.setAdapter(mCosasAdapter);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        swipeRefreshLayout.setRefreshing(true);
                                        updateTime();
                                    }
                                }
        );
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String hora = mCosasAdapter.getItem(position);
                Toast.makeText(getActivity(), formatDate(getFecha()), Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, hora);
                startActivity(intent);
            }
        });

        return rootView;
    }

    private void updateTime() {
        swipeRefreshLayout.setRefreshing(true);
        FetchCosasTask timeTask = new FetchCosasTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = prefs.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));
        timeTask.execute(location);
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateTime();
    }

    public class FetchCosasTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchCosasTask.class.getSimpleName();

        private String[] getTimeDataFromJson(String JsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String WWO_DATA = "data";
            final String WWO_TZ = "time_zone";
            final String WWO_LT = "localtime";
            final String WWO_REQUEST = "request";
            final String WWO_LUGAR = "query";
            final String WWO_UTC = "utcOffset";
            JSONObject horaArray = null;
            JSONObject jsonObject = new JSONObject(JsonStr);
            horaArray = jsonObject.getJSONObject(WWO_DATA);
            String[] resultStrs = new String[1];
            for (int i = 0; i < 1; i++) { //Bucle estúpido xD
                try {
                    JSONArray lugar = horaArray.getJSONArray(WWO_REQUEST);
                    JSONObject a = lugar.getJSONObject(0);
                    String nombre = a.getString(WWO_LUGAR);
                    JSONArray timeZone = horaArray.getJSONArray(WWO_TZ);

                    JSONObject c = timeZone.getJSONObject(0);
                    JSONObject d = timeZone.getJSONObject(0);
                    String localTime = c.getString(WWO_LT);
                    setFecha(localTime);
                    Float gmt = Float.parseFloat(d.getString(WWO_UTC));
                    String que_hora_es = getResources().getString(R.string.que_hora_es);
                    String que_hora_es2 = getResources().getString(R.string.que_hora_es2);
                    if (gmt < 0)
                        resultStrs[i] = que_hora_es + " " + nombre + que_hora_es2 + "\n" + formatDate(getFecha()) + " (GMT" + gmt.toString() + ")";
                    else if (gmt == 0)
                        resultStrs[i] = que_hora_es + " " + nombre + que_hora_es2 + "\n" + formatDate(getFecha()) + " (GMT)";
                    else
                        resultStrs[i] = que_hora_es + " " + nombre + que_hora_es2 + "\n" + formatDate(getFecha()) + " (GMT+" + gmt.toString() + ")";
                } catch (org.json.JSONException e) {
                    e.printStackTrace();
                    resultStrs[i] = getResources().getString(R.string.erroor);
                    setFecha(getResources().getString(R.string.erroor_toast));
                }

            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Esta debería ser la hora: " + s);
            }
            return resultStrs;

        }

        @Override
        protected String[] doInBackground(String... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            if (params.length == 0) {
                return null;
            }
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String cosasJsonStr = null;
            String format = "json";
            String lang = "es";
            try {
                // Se construye la URL
                final String HORA_BASE_URL =
                        "http://api.worldweatheronline.com/free/v2/tz.ashx?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "format";
                final String KEY_PARAM = "key";
                final String LANG_PARAM = "lang";
                Uri builtUri = Uri.parse(HORA_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(KEY_PARAM, BuildConfig.HORA_MAP_API_KEY)
                        .appendQueryParameter(LANG_PARAM, lang)
                        .build();

                URL url = new URL(builtUri.toString());


                // Se hace la petición a la API
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                cosasJsonStr = buffer.toString();
                Log.v(LOG_TAG, "URI: " + builtUri.toString());
                Log.v(LOG_TAG, "String JSON de la hora: " + cosasJsonStr);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error " + e.getMessage(), e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }

            try {
                return getTimeDataFromJson(cosasJsonStr, 1);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mCosasAdapter.clear();
                for (String horaStr : result) { // Solo hay uno xD
                    mCosasAdapter.add(horaStr);
                }
                // New data is back from the server. Hooray!
            }
        }

    }

}
