import control.*;
import model.FileJava;
import model.Release;
import model.Issue;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.util.List;
public class Main {

    private static String projName = "BOOKKEEPER";
    public static void main(String[] args) throws Exception {

        JiraController jiraControl = new JiraController();
        ProportionController proportionController = new ProportionController();
        List<Release> releaseList = jiraControl.getReleases(projName); //ottengo tutte le release
        List<Issue> bugsList = jiraControl.getIssues(projName,false); //ottengo tutti i bug (controllati)
        //System.out.println(bugsList.size());

        /*for (Release r : releaseList){
            System.out.println(r.getId() + " " + r.getName() + " " + r.getDate() );
        }*/

        List<Release> halfReleaseList = jiraControl.halfReleases(releaseList);
        System.out.println("inizio metà release");
        for (Release r : halfReleaseList){
            System.out.println(r.getId() + " " + r.getName() + " " + r.getDate() );
        }

        List<Issue> bugsListProportion = ProportionController.computeProportion(releaseList, bugsList);
        List<Issue> bugsListFinal = JiraController.cleanOvFv(bugsList);

        //ora calcoliamo le av dei bug le quali iv sono state calcolate con proportion
        proportionController.calculatorAvAfterProportion(bugsListFinal, releaseList);

        //stampa con av
        for (Issue issues : bugsListFinal){
            System.out.println("num: " + issues.getNum() + " key: " + issues.getKey() + " ov: " + issues.getOv().getName() + " fv: " +issues.getFv().getName() + " indice fv: " + issues.getFv().getId());
            if (issues.getIv()!=null) System.out.println("indice iv: " + issues.getIv().getId());
            for(Release rel: issues.getAv()) System.out.println("av: " + rel.getId());
        }

        //next: retrive git java file and metrics
        GitController gitControl = new GitController(projName);
        List<List<FileJava>> fileJavaList = gitControl.loadGitInfo(bugsListFinal, projName, halfReleaseList); //qui ottengo una lista di file java (model.FileJava) con tutte le metriche calcolate

        MetricsController metricsControl = new MetricsController(projName);
        List<FileJava> tempListFile = metricsControl.computeBuggynessProva(fileJavaList, bugsListFinal);

        /*for(FileJava fileJava : fileJavaList.get(4)){
            System.out.println("Nome: " + fileJava.getFilename() + " commit list: " + fileJava.getListCommmit());
        }*/

        int countBuggy = 0;
        int countT = 0;
        for(List<FileJava> fileJavaL : fileJavaList){
            for(FileJava file : fileJavaL){
                countT++;
                file.setBuggy("No");
                if(tempListFile.contains(file)){
                    file.setBuggy("Yes");
                    countBuggy++;
                }
            }
        }
        System.out.println("Numero file bug yes = " + countBuggy);
        System.out.println("Tot file = " + countT);

        CsvController csvControl = new CsvController();
        //genero il file csv
        csvControl.makeCsv(fileJavaList, projName, halfReleaseList.size());
        //converto il csv in arff e genero il file arff
        //csvControl.generateArff(projName, 0); //testing totale

        //iniziamo con walk foward generando i training set
        WekaController wekaControl = new WekaController(projName);
        wekaControl.doWalkForward();

        for(int i=1; i<fileJavaList.size(); i++) {
            csvControl.makeCsvTesting(fileJavaList.get(i), projName, i+1);
            csvControl.generateArff(projName, i+1, "testing");
        }

        wekaControl.computeClassifier(fileJavaList.size(), projName);

        /*  strategia walk forward qui di seguito
            -
            -
            -
            poi ricorda weka api: evalutian, instaces, datasource ecc

            se random forest mi da errore perché ho 0 classi bug, fai assunzioni, posso aggiungere una riga nuova finta (non molto buono)
            non considero il run?
         */
    }
}