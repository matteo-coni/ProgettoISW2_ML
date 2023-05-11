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

    public  List<Release> getReleases(String project) throws IOException {

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

    public List<Issue> getIssues (String projName, Boolean coldStart) throws IOException {

        List<Release> releaseList = getReleases(projName);
        List<Release> halfReleaseList = releaseList; //in modo che
        //flag per il cold start, se coldStart è true, non devo dimezzare le release perchè sto calcolando cold start
        if (!coldStart) {
            //halfReleaseList = halfReleases(releaseList);todo
            halfReleaseList = releaseList;
        }
        List<Issue> listIssues = new ArrayList<>();
        List<Release> avList;
        JSONArray avJSONArray;
        Release ov;
        Release fv;
        Release iv;
        Date resolutionDate;
        Date openingDate;

        int i=0;
        int j=0;
        int total=1;

        do {
            // only max 1000 issue each time
            j = i + 1000;

            /* project = <projName> AND issuetype = Bug AND (status = Closed OR status = Resolved) AND resolution = Fixed */
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created,fixVersions&startAt="
                    + Integer.toString(i) + "&maxResults=" + Integer.toString(j);

            JSONObject objectJson = readJsonFromUrl(url);
            JSONArray issuesArray = objectJson.getJSONArray("issues");
            total = objectJson.getInt("total");

            for (; i < total && i < j; i++) {

                JSONObject currentObjectJson = issuesArray.getJSONObject(i%1000);
                String key = currentObjectJson.get("key").toString();
                String versionJson = currentObjectJson.getJSONObject(FIELD).get("versions").toString();
                String fixVersionsJson = currentObjectJson.getJSONObject(FIELD).get("fixVersions").toString();
                String stringResolutionDate = currentObjectJson.getJSONObject(FIELD).getString("resolutiondate");
                String creationDate = currentObjectJson.getJSONObject(FIELD).get("created").toString();

                stringResolutionDate = stringResolutionDate.substring(0,10);
                resolutionDate = Date.valueOf(stringResolutionDate);

                creationDate = creationDate.substring(0,10);
                openingDate = Date.valueOf(creationDate);

                //creo due oggetti fv e ov da inserire poi nella creazione dell'oggetto issue
                fv = getFixedOpeningVersions(halfReleaseList, resolutionDate);
                ov = getFixedOpeningVersions(halfReleaseList, openingDate);


                //array dell'oggetto i-esimo, prendendo il campo fields e successivamente le affected versions
                avJSONArray = issuesArray.getJSONObject(i%1000).getJSONObject(FIELD).getJSONArray("versions");
                //System.out.println(i+1);
                List<Release> affectedVersions = getAffectedVersions(avJSONArray, halfReleaseList);
                avList = affectedVersions;
                //System.out.println(avList);

                Issue addIssue = createIssue(key, ov, fv, avList, total-i);
                if (!verifyIssue(addIssue)) listIssues.add(addIssue);

            }

        } while (i < total);
        //ordino lista in ordine crescente, dal più vecchio (1) al più recente [1,2,3..]
        listIssues.sort(Comparator.comparing(o -> o.getNum()));
        return listIssues;
    }

    /*in questo metodo verifico delle condizioni sui vari bug prima di aggiungerli alla lista
    * quando troviamo return true, significa che il ticket è da scartare
    * */
    public boolean verifyIssue(Issue issue){
        //fv or ov are null
        if (issue.getFv() == null || issue.getOv()==null){
            return true;
        }
        // ov > fv
        if (issue.getOv().getDate().compareTo(issue.getFv().getDate())>0){
            return true;
        }
        //cosi con bookkeeper ci sono 372 bug validi, per ora
        // iv = ov = fv
        if (issue.getIv()!=null && issue.getIv().getDate().compareTo(issue.getOv().getDate())==0 && issue.getOv().getDate().compareTo(issue.getFv().getDate())==0){
            return true;
        }
        //con questo diventano 368
        // iv!=null e iv > ov
        if (issue.getIv()!=null && (issue.getOv().getDate().compareTo(issue.getIv().getDate())<0)){
            return true;
        }



        return false;
    }
    public Issue createIssue(String key, Release ov, Release fv, List<Release> avList, int i){
        Issue newIssue = null;
        Release iv = null;
        //se la lista delle av non è vuota, allora significa che è presente iv e posso inserirla
        //ordino le av e prendo come iv la prima della lista ordinata
        if(!avList.isEmpty()){
            avList.sort(Comparator.comparing(o -> o.getDate().toString()));
            iv = avList.get(0);
            newIssue = new Issue(key, iv, ov, fv, avList, i);
        } else {
            //se non è presente l'av, non lo è nemmeno l'iv, quindi la imposto per ora uguale a null
            newIssue = new Issue(key, null, ov, fv, avList, i);
        }

        return newIssue;
    }

    /*
      in questo metodo ottengo la prima release successiva alla data passata come parametro
      se passo come date la data di apertura del ticket, ottengo l'opening version
      se passo la data di chiusura, ottengo la fix version
     */

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

    public List<Release> getAffectedVersions(JSONArray affVersArray, List<Release> releaseList) {

        List<Release> avList = new ArrayList<>();
        Release releaseAv;
        boolean released; //true o false flag
        int jsonArrLenght = affVersArray.length();
        int k;

        for (k = 0; k < jsonArrLenght; k++){
            released = affVersArray.getJSONObject(k).getBoolean("released");
            if (released){
                //questo perché in zookkeeper (e altri) alcuni ticket hanno av senza releasedate nel json
                Date releaseDate = getReleaseDateByName(affVersArray.getJSONObject(k).getString("name"), releaseList);
                int releaseId = getReleaseIdByName(affVersArray.getJSONObject(k).getString("name"), releaseList);

                if (releaseDate!=null) { //qui skip le av senza le releasedate
                    releaseAv = new Release(releaseId, affVersArray.getJSONObject(k).getString("name"), releaseDate);//Date.valueOf(affVersArray.getJSONObject(k).getString("releaseDate")));

                    avList.add(releaseAv);
                }
            }
        }

        return avList;
    }

    public static Date getReleaseDateByName(String nameRel, List<Release> releaseList){
        Date dateRel = null;
        for(Release rel : releaseList){
            if (rel.getName().equals(nameRel)){
                dateRel = rel.getDate();
            }
        }
        return dateRel;
    }

    public static int getReleaseIdByName(String nameRel, List<Release> releaseList){
        int id = 0;
        for(Release rel : releaseList){
            if (rel.getName().equals(nameRel)){
                id = rel.getId();
            }
        }
        return id;
    }
    public List<Release> halfReleases (List<Release> allReleaseList){
        //List<Release> listHalfRelease = new ArrayList<>();

        int halfSize = allReleaseList.size() / 2;

        return  allReleaseList.subList(0,halfSize);
    }

    //in cleanOvFv tolgo tutti i bug che hanno ov e fv = 1 dopo aver fatto proportion
    public static List<Issue> cleanOvFv (List<Issue> bugsList){
        List<Issue> listBugFinal = new ArrayList<>();
        for(Issue bug : bugsList){
            if(bug.getOv().getId() == 1 && (bug.getFv().getId() == 1)){
                continue;
            } else {
                listBugFinal.add(bug);
            }
        }
        return listBugFinal;
    }
}
