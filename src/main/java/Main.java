import control.JiraController;
import control.ProportionController;
import model.Release;
import model.Issue;

import java.io.IOException;
import java.util.List;
public class Main {

    private static String projName = "BOOKKEEPER";
    public static void main(String[] args) throws IOException {
        //System.out.println("Hello world!");

        JiraController jiraControl = new JiraController();
        List<Release> releaseList = jiraControl.getReleases(projName); //ottengo tutte le release
        List<Issue> bugsList = jiraControl.getIssues(projName); //ottengo tutti i bug (controllati)
        System.out.println(bugsList.size());

        for (Release r : releaseList){
            System.out.println(r.getId() + " " + r.getName() + " " + r.getDate() );
        }

        List<Release> halfReleaseList = JiraController.halfReleases(releaseList);
        System.out.println("inizio metà release");
        for (Release r : halfReleaseList){
            System.out.println(r.getId() + " " + r.getName() + " " + r.getDate() );
        }

        //proportion
        List<Issue> bugsListProportion = ProportionController.computeProportion(releaseList, bugsList);
        List<Issue> bugsListProportionHalf = JiraController.halfIssues(bugsList); //tolgo la seconda metà dei bug per lo snoring
        List<Issue> bugsListFinal = JiraController.cleanOvFv(bugsListProportionHalf);


        for (Issue issues : bugsListFinal){
            System.out.println("num: " + issues.getNum() + " key: " + issues.getKey() + " ov: " + issues.getOv().getName() + " fv: " +issues.getFv().getName() + " indice fv: " + issues.getFv().getId());
            if (issues.getIv()!=null) System.out.println("indice iv: " + issues.getIv().getId());
        }
        System.out.println(bugsListFinal.size());

        //next: retrive git java file and metrics
    }
}