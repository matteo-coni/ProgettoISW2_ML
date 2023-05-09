package control;

import model.FileJava;
import model.Issue;
import model.Release;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class WekaController {

    private Git git;
    private String localPath = "";
    private Repository repository;

    private String projName;
    public WekaController(String projName) throws IOException {
        this.localPath = "/Users/matteo/IdeaProjects/" + projName.toLowerCase();
        this.git = Git.open(new File(localPath));
        this.repository = git.getRepository();
        this.projName = projName;
    }

    public void doWalkForward() throws Exception {

        JiraController jiraControl = new JiraController();

        List<Release> releaseList = jiraControl.getReleases(projName);
        List<Issue> bugsList = jiraControl.getIssues(projName,false);
        List<Release> halfReleaseList = jiraControl.halfReleases(releaseList);

        //qui applico walk forward richiamando

        for(int countRelease = 1; countRelease < halfReleaseList.size(); countRelease++){

            ProportionController proportionControl = new ProportionController();
            List<Issue> bugsListProportion = ProportionController.computeProportion(releaseList.subList(0, countRelease), bugsList);
            List<Issue> bugsListFinal = JiraController.cleanOvFv(bugsList);
            proportionControl.calculatorAvAfterProportion(bugsListFinal, releaseList.subList(0, countRelease));
            GitController gitControl = new GitController(projName);
            List<List<FileJava>> fileJavaList = gitControl.loadGitInfo(bugsListFinal, projName, halfReleaseList.subList(0,countRelease));
            MetricsController metricsControl = new MetricsController(projName);
            List<FileJava> tempListFile = metricsControl.computeBuggynessProva(fileJavaList, bugsListFinal);

            for(List<FileJava> fileJavaL : fileJavaList){
                for(FileJava file : fileJavaL){
                    file.setBuggy("No");
                    if(tempListFile.contains(file)){
                        file.setBuggy("Yes");
                    }
                }
            }

            CsvController csvControl = new CsvController();
            //genero il file csv
            csvControl.makeCsv(fileJavaList, projName, countRelease);
            //converto il csv in arff e genero il file arff
            csvControl.generateArff(projName, countRelease, "training");



        }



    }
}
