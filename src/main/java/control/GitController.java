package control;

import model.FileJava;
import model.Release;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;


public class GitController {

    private final Git git;
    private static String localPath;

    private static final String PATH= "/Users/matteo/IdeaProjects/";
    private static Repository repository;
    public GitController (String projName) throws IOException {
        localPath = PATH + projName.toLowerCase();
        git = Git.open(new File(localPath));
        repository = git.getRepository();
    }
    public static List<RevCommit>  retrieveAllCommits() throws IOException{
        /*
          In questo metodo prendo tutti i commit
         */
        List<RevCommit> commitFinal = new ArrayList<>();

        try (Git git = Git.open(new File(localPath))) {

            Iterable<RevCommit> commits = git.log().all().call();
            //dopo aver filtrato, in una lista nuova (commitFinal) aggiungo solo quelli che nello short message contengono BOOKKEEPER-xxx
            for (RevCommit commit : commits) {
                    commitFinal.add(commit);
            }

        } catch (GitAPIException ignored) {
            Logger.getLogger("CIAO");
        }

        return commitFinal;
    }

    public static List<List<RevCommit>> listOfCommitForRelease (List<RevCommit> commitList, List<Release> releaseList){

        List<List<RevCommit>> listOfListCommits = new ArrayList<>();

        for(Release release : releaseList){
            List<RevCommit> listCommitsReleaseX = new ArrayList<>();
            for(RevCommit commit : commitList){
                Date dateCommit = commit.getCommitterIdent().getWhen();
                Date dateRelease = release.getDate();
                if(dateCommit.compareTo(dateRelease)<=0){
                    listCommitsReleaseX.add(commit);
                }

            }
            commitList.removeAll(listCommitsReleaseX);
            listOfListCommits.add(listCommitsReleaseX);
        }

        return listOfListCommits;

    }

    public static void commitListOrderer(List<List<RevCommit>> llrc){
        for(List<RevCommit> lrc : llrc) {
            Collections.sort(lrc, Comparator.comparingLong(RevCommit::getCommitTime));
        }
    }

    public static List<FileJava> getAllFiles(RevCommit commit) throws IOException {


        TreeWalk treeWalk = new TreeWalk(repository);
        ObjectId treeId = commit.getTree().getId();

        treeWalk.reset(treeId);
        treeWalk.setRecursive(false);

        List<FileJava> javaFile = new ArrayList<>();
        while (treeWalk.next()) {
            if (treeWalk.isSubtree()) {
                treeWalk.enterSubtree();
            }
            else {
                if (treeWalk.getPathString().endsWith(".java") && !treeWalk.getPathString().contains("/test/")) {
                    String className = treeWalk.getPathString() ;
                    FileJava jv = new FileJava(className, null);
                    javaFile.add(jv);
                }
            }
        }
        return javaFile;
    }

    public static void setReleaseFile (List<List<FileJava>> fileJavaList, List<Release> releaseList){

        int i = 0;
        for(List<FileJava> listFile : fileJavaList){
            Release rel = releaseList.get(i);
            for(FileJava fileJava : listFile){
                fileJava.setRelease(rel);
            }
            i++;
        }
    }


    public static void setCommitList(List<List<RevCommit>> commitList, List<List<FileJava>> fileJavaList) throws IOException {


        DiffFormatter formatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
        formatter.setRepository(repository);
        formatter.setDiffComparator(RawTextComparator.DEFAULT);
        formatter.setDetectRenames(true);

        for(int i=0; i < commitList.size();i++) {
            for(RevCommit commit : commitList.get(i)){
                if(commit!=commitList.get(0).get(0) && commit.getParentCount()>0){
                    ObjectId commitId = commit.getId();
                    RevCommit parent = commit.getParent(0);
                    ObjectId parentID = parent.getId();
                    List<DiffEntry> diffs = formatter.scan(parentID, commitId);

                    for (DiffEntry diff : diffs) {
                        List<RevCommit> listTemp;
                        for (FileJava file : fileJavaList.get(i)) {
                            listTemp = file.getListCommmit();
                            if (diff.getNewPath().equals(file.getFilename()) && (listTemp != null)) {
                                listTemp.add(commit);
                                file.setListCommit(listTemp);

                            }
                        }
                    }
                }
            }
        /*
            in questo metodo prendo in input la lista di commit e di fileJava,
            li scorro e utilizzando revTree e TreeWalk mi muovo nell'albero per
            trovare i file che vengono toccati da quel commit
            se il nome del file è uguale a quello dell'oggetto fileJava del ciclo
            for, lo aggiungo alla lista
            alla fine del for aggiungo la lista alla lista di RevCommit
         */

        }
    }

    public List<List<FileJava>> loadGitInfo(String projName, List<Release> halfReleaseList) throws IOException {

        new GitController(projName);
        List<RevCommit> commitList = retrieveAllCommits();

        List<List<RevCommit>> commitDividedForRelease = listOfCommitForRelease(commitList, halfReleaseList);
        commitListOrderer(commitDividedForRelease);
        List<List<FileJava>> listAllFiles = new ArrayList<>();

        for(List<RevCommit> listCommit : commitDividedForRelease) {
            if(!listCommit.isEmpty()) {
                RevCommit lastCommit = listCommit.get(listCommit.size() - 1);
                List<FileJava> javaFile = getAllFiles(lastCommit);
                listAllFiles.add(javaFile);

            } else {
                List<FileJava> javaFile = new ArrayList<>();
                listAllFiles.add(javaFile);
            }

        }
        //settiamo le release per ogni file
        setReleaseFile(listAllFiles,halfReleaseList);

        //settiamo la lista di commit per ogni file
        setCommitList(commitDividedForRelease, listAllFiles);

        MetricsController metricsControl = new MetricsController(projName);

        //prendiamo ogni file e ne computiamo le metriche
        for(int i=0; i < listAllFiles.size(); i++) {
            for (int j = 0; j < (listAllFiles.get(i).size()); j++) {

                metricsControl.computeMetrics(listAllFiles.get(i).get(j), listAllFiles);
            }
        }

    return listAllFiles;
    }

}

/*
prendo tutti i commit, poi lista di liste dove ogni lista contiene tutti i commit che appartengono a una certa release (temporalmente)
prendo, per ogni lista, tutti i file toccati da tutti i commit di quella lista, eliminando i duplicati
-> abbiamo tutti i file con release e lista di commit che li toccano
-> possiamo calcolare tutto
 */


// un file è buggy se, tra i commit che lo hanno modificato, troviamo un commit che riporta nello short message un ticket che ha tra le affected version la versione di quel file