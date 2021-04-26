package pt.ulisboa.tecnico.cmov.shopist;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.PantryList;
import pt.ulisboa.tecnico.cmov.shopist.persistence.domain.StoreList;

public class DownloadUrl extends AsyncTask<Object, Void, String> {

    PantryList pantryList = null;
    StoreList storeList = null;

    @Override
    protected String doInBackground(Object... objects) {

        if(objects[0] instanceof PantryList) {
            pantryList = (PantryList) objects[0];
        }
        else if(objects[0] instanceof StoreList) {
            storeList = (StoreList) objects[0];
        }

        String url = (String) objects[1];
        String data = "";
        InputStream inputStream = null;
        HttpURLConnection urlConnection = null;

        try {
            URL _url = new URL(url);
            urlConnection = (HttpURLConnection) _url.openConnection();
            urlConnection.connect();

            inputStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuffer sb = new StringBuffer();

            String line = "";
            while((line = br.readLine()) != null) {
                sb.append(line);
            }

            data  = sb.toString();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(urlConnection != null)
                urlConnection.disconnect();
        }
        return data;
    }

    @Override
    protected void onPostExecute(String s) {
        JSONObject jsonObject = null;
        String driveTime = null;

        try {
            jsonObject = new JSONObject(s);
            driveTime = jsonObject.getJSONArray("rows").getJSONObject(0).getJSONArray("elements").getJSONObject(0).getJSONObject("duration").getString("text");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(pantryList == null) storeList.driveTime = driveTime;
        else if(storeList == null) pantryList.driveTime = driveTime;
        if(driveTime != null)
            Log.d("SHIT", driveTime);
        else
            Log.d("SHIT", "driveTime is NULL");

    }


}
