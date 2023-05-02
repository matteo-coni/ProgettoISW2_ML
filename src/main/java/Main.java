import control.CsvController;
import control.GitController;
import control.JiraController;
import control.ProportionController;
import model.FileJava;
import model.Release;
import model.Issue;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;
public class Main {

    private static String projName = "BOOKKEEPER";
    public static void main(String[] args) throws IOException, GitAPIException {

        JiraController jiraControl = new JiraController();
        ProportionController proportionController = new ProportionController();
        List<Release> releaseList = jiraControl.getReleases(projName); //ottengo tutte le release
        List<Issue> bugsList = jiraControl.getIssues(projName,false); //ottengo tutti i bug (controllati)
        //System.out.println(bugsList.size());

        /*for (Release r : releaseList){
            System.out.println(r.getId() + " " + r.getName() + " " + r.getDate() );
        }*/

        List<Release> halfReleaseList = jiraControl.halfReleases(releaseList);
        /*System.out.println("inizio metà release");
        for (Release r : halfReleaseList){
            System.out.println(r.getId() + " " + r.getName() + " " + r.getDate() );
        }*/


        List<Issue> bugsListProportion = ProportionController.computeProportion(releaseList, bugsList);
        List<Issue> bugsListFinal = JiraController.cleanOvFv(bugsList);

        //System.out.println(bugsListFinal.size());

        //ora calcoliamo le av dei bug le quali iv sono state calcolate con proportion
        proportionController.calculatorAvAfterProportion(bugsListFinal, releaseList);

        //stampa con av
        /*for (Issue issues : bugsListFinal){
            System.out.println("num: " + issues.getNum() + " key: " + issues.getKey() + " ov: " + issues.getOv().getName() + " fv: " +issues.getFv().getName() + " indice fv: " + issues.getFv().getId());
            if (issues.getIv()!=null) System.out.println("indice iv: " + issues.getIv().getId());
            for(Release rel: issues.getAv()) System.out.println("av: " + rel.getId());
        }*/

        //next: retrive git java file and metrics
        GitController gitControl = new GitController();
        List<List<FileJava>> fileJavaList = gitControl.loadGitInfo(); //qui ottengo una lista di file java (model.FileJava) con tutte le metriche calcolate
        //gitControl.loadGitInfo();

        CsvController csvControl = new CsvController();
        csvControl.makeCsv(fileJavaList);


    }
}