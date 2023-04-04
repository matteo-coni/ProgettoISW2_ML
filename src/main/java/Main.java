import control.JiraController;
import model.Release;

import java.io.IOException;
import java.util.List;
public class Main {

    private static String projName = "BOOKKEEPER";
    public static void main(String[] args) throws IOException {
        System.out.println("Hello world!");

        JiraController jiraControl = new JiraController();
        List<Release> releaseList = jiraControl.getReleases(projName);

        for (Release r : releaseList){
            System.out.println(r.getId() + " " + r.getName() + " " + r.getDate() );
        }


    }
}