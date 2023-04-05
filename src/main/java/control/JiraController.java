package control;

import model.Release;
import model.Issue;

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
    private static final String FIELD = "fields";
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

    public List<Issue> getIssues (String projName) throws IOException {

        List<Release> releaseList = JiraController.getReleases(projName);
        List<Issue> listIssues = new ArrayList<>();
        List<Release> avList;
        JSONArray avJSONArray;
        Release ov;
        Release fv;
        Release iv;
        Date resolutionDate;
        Date openingDate;

        Integer i=0;
        Integer j=0;
        Integer total=1;

        do {
            // only max 1000 issue each time
            j = i + 1000;

            /* project = <projName> AND issuetype = Bug AND (status = Closed OR status = Resolved) AND resolution = Fixed */
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created&startAt="
                    + i.toString() + "&maxResults=" + j.toString();

            JSONObject objectJson = readJsonFromUrl(url);
            JSONArray issuesArray = objectJson.getJSONArray("issues");
            total = objectJson.getInt("total");

            for (; i < total && i < j; i++) {

                /*Issue issue = createTicketInstance(i, issues, releasesList);
                if(ticket != null) {
                    ticketsList.add(ticket);
                }*/
                JSONObject currentObjectJson = issuesArray.getJSONObject(i%1000);
                String key = currentObjectJson.get("key").toString();
                String versionJson = currentObjectJson.getJSONObject(FIELD).get("versions").toString();
                String fixVersionsJson = currentObjectJson.getJSONObject(FIELD).get("fixVersions").toString();
                String stringResolutionDate = currentObjectJson.getJSONObject(FIELD).get("resolutiondate").toString();
                String creationDate = currentObjectJson.getJSONObject(FIELD).get("created").toString();

                //resolutionDate = resolutionDate.substring(0,10);
                resolutionDate = Date.valueOf(stringResolutionDate);
                openingDate = Date.valueOf(creationDate);

                fv = getFixedOpeningVersions(releaseList, resolutionDate);
                ov = getFixedOpeningVersions(releaseList, openingDate);

                avJSONArray = issuesArray.getJSONObject(i%1000).getJSONObject(FIELD).getJSONArray("versions");
                avList = getAffectedVersions(avJSONArray);

            }

        } while (i < total);

        return listIssues;
    }

    public Release getFixedOpeningVersions(List<Release> releaseList, Date date) {

        Release rel = null;
        for (Release release : releaseList){
            if(release.getDate().compareTo(date)>0){
                rel = release;
                break;
            }
        }

        return rel;
    }

    public List<Release> getAffectedVersions(JSONArray affVersArray){

        List<Release> avList = new ArrayList<>();
        //to do: fai il parsing del jsonArray per creare le versioni ed aggiungerle alla lista avList


        return avList;
    }
}
