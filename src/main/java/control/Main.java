package control;

import model.FileJava;
import model.Release;
import model.Issue;

import java.util.List;
public class Main {

    private static final  String PROJNAME = "BOOKKEEPER";
    public static void main(String[] args) throws Exception {

        JiraController jiraControl = new JiraController();
        ProportionController proportionController = new ProportionController();
        List<Release> releaseList = jiraControl.getReleases(PROJNAME); //ottengo tutte le release
        List<Issue> bugsList = jiraControl.getIssues(PROJNAME); //ottengo tutti i bug (controllati)

        List<Release> halfReleaseList = jiraControl.halfReleases(releaseList);

        ProportionController.computeProportion(releaseList, bugsList);
        List<Issue> bugsListFinal = JiraController.cleanOvFv(bugsList);

        //ora calcoliamo le av dei bug le quali iv sono state calcolate con proportion
        proportionController.calculatorAvAfterProportion(bugsListFinal, releaseList);

        GitController gitControl = new GitController(PROJNAME);
        List<List<FileJava>> fileJavaList = gitControl.loadGitInfo(PROJNAME, releaseList); //qui ottengo una lista di file java (model.FileJava) con tutte le metriche calcolate

        MetricsController metricsControl = new MetricsController(PROJNAME);
        List<FileJava> tempListFile = metricsControl.computeBuggynessProva(fileJavaList, bugsListFinal);

        for(List<FileJava> fileJavaL : fileJavaList){
            for(FileJava file : fileJavaL){
                file.setBuggy("No");
                if(tempListFile.contains(file)){
                    file.setBuggy("Yes");
                }
            }
        }



        CsvController csvControl = new CsvController(PROJNAME);
        //genero il file csv
        csvControl.makeCsv(fileJavaList, PROJNAME, halfReleaseList.size());

        //iniziamo con walk forward generando i training set
        WekaController wekaControl = new WekaController(PROJNAME);
        wekaControl.doWalkForward();

        for(int i=1; i<=(fileJavaList.size()/2)-1; i++) {
            csvControl.makeCsvTesting(fileJavaList.get(i), PROJNAME, i+1);
            csvControl.generateArff(PROJNAME, i+1, "testing");
        }

        wekaControl.computeClassifier((fileJavaList.size()/2)-1, PROJNAME);

        /*  strategia walk forward qui di seguito

            poi ricorda weka api: evalutian, instaces, datasource ecc

            se random forest mi da errore perché ho 0 classi bug, fai assunzioni, posso aggiungere una riga nuova finta (non molto buono)
            non considero il run?
         */
    }
}