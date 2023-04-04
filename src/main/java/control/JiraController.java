package control;

import model.Release;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;


public class JiraController {

    private static final String MACRODATE = "releaseDate";
    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    public static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONArray json = new JSONArray(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public  static List<Release> getReleases(String project) throws IOException {

        String url = "https://issues.apache.org/jira/rest/api/2/project/" + project + "/versions";
        List<Release> releaseList = new ArrayList<>();
        boolean released;
        JSONArray json = readJsonArrayFromUrl(url);
        int len = json.length();
        for(int i = 0; i<len; i++){
            JSONObject jsonObject = json.getJSONObject(i);
            released = jsonObject.getBoolean("released");
            if(released){
                try {
                    //String dateRelease = json.getJSONObject(i).get("releaseDate").toString();
                    Release release = new Release(jsonObject.getString("name"), Date.valueOf(jsonObject.getString(MACRODATE)));
                    releaseList.add(release);

                }catch (Exception e){
                    //no date in release
                }
            }
        }

        //sorting con interfaccia comparator
        releaseList.sort(Comparator.comparing(o -> o.getDate().toString()));

        //Set index of release
        int count = 1;
        for(Release release: releaseList){
            release.setId(count);
            count ++;
        }

        return releaseList;
    }
}
