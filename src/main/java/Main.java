import control.JiraController;
import control.ProportionController;
import model.Release;
import model.Issue;

import java.io.IOException;
import java.util.List;
public class Main {

    private static String projName = "ZOOKEEPER";
    public static void main(String[] args) throws IOException {
        //System.out.println("Hello world!");

        JiraController jiraControl = new JiraController();
        List<Release> releaseList = jiraControl.getReleases(projName); //ottengo tutte le release
        List<Issue> bugsList = jiraControl.getIssues(projName); //ottengo tutti i bug (controllati)

        for (Release r : releaseList){
            System.out.println(r.getId() + " " + r.getName() + " " + r.getDate() );
        }

       for (Issue issues : bugsList){
            System.out.println(issues.getKey() + " ov: " + issues.getOv().getName() + " fv: " +issues.getFv().getName() + " affect: " + issues.getAv().size());

       }
        System.out.println(bugsList.size());

        //now retrive iv in the bug where is null
        List<Release> halfReleaseList = JiraController.halfReleases(releaseList);

        //to do: proportion
        List<Issue> bugsListProportion = ProportionController.computeProportion(releaseList, bugsList);

        //next: retrive git java file and metrics

    }
}