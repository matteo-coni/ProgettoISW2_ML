package control;

import model.FileJava;
import model.Issue;
import model.Release;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import javax.sound.midi.SysexMessage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MetricsController {

    private static Repository repository;
    public MetricsController() throws IOException {
        String localPath = "/Users/matteo/IdeaProjects/bookkeeper";
        Git git = Git.open(new File(localPath));
        repository = git.getRepository();

    }
    public void computeMetrics(FileJava fileJava, List<List<FileJava>> fileJavaList, List<Issue> bugsList) throws IOException {

        System.out.println("\n");
        int loc = setSizeLoc(fileJava);
        fileJava.setSizeLoc(loc); //setta l'attributo
        //se non ci sono commit che toccano un file, esso avrà loc=0, quindi per risolvere prendo le loc del file nella versione precedente
        if(fileJava.getSizeLoc()==0){
            computeLocWhen0(fileJava, fileJavaList);
        }
        //numero di autori
        int numberAuthors = computeNumberAuthors(fileJava);
        fileJava.setNumberAuthors(numberAuthors);

        //calcoliamo e settiamo le metriche sulle LOC (loc touched, max loc added, avg loc added, churn, max churn, avg churn
        int locTouched = computeLocTouched(fileJava);
        fileJava.setTouchedLoc(locTouched);

        //calcoliamo e settiamo number of revision
        int numberRevision = fileJava.getListCommmit().size();
        fileJava.setNr(numberRevision);

        //computeBuggynessProva(fileJava, bugsList);

        System.out.println("release " + fileJava.getRelease().getName() + " file: " + fileJava.getFilename() + " LOC: " + fileJava.getSizeLoc()
                + " nr authors: " + fileJava.getNumberAuthors() + " LOC TOUCHED: " + fileJava.getTouchedLoc() + " NR: " + fileJava.getNr() + " LOC ADDED: " + fileJava.getAddedLoc()
                + " MAX LOC ADD: " + fileJava.getMaxLocAdded() + " AVG ADD: " + fileJava.getAvgLocAdded() + " Churn: " + fileJava.getChurn() + " MAX CHURN: " + fileJava.getMaxChurn()
                + " AVG churn: " + fileJava.getAvgChurn() + " Buggy: " + fileJava.getBuggy());

        for(RevCommit commit : fileJava.getListCommmit()) {
            System.out.println(commit.getShortMessage());
        }
    }

    public static int setSizeLoc(FileJava fileJava) throws IOException {


        if((fileJava.getListCommmit().size()-1)<0){
            return 0;
        }
        RevCommit lastCommit = fileJava.getListCommmit().get(fileJava.getListCommmit().size()-1);
        String filePath = fileJava.getFilename();

        RevTree commitTree = lastCommit.getTree();
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, filePath, commitTree)) {
            ObjectId blobId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(blobId);

            //Qui leggo il contenuto del file e copio il contenuto di loader nell'output stream
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            loader.copyTo(out);

            // Calcola le linee di codice
            String fileContent = new String(out.toByteArray());
            int linesOfCode = 0;
            try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String read; //for sonar
                while ((read = reader.readLine()) != null) {
                    //togli il commento sottostante per aggiungere i filtri nel conteggio LOC
                    //if (!line.trim().startsWith("//") && !line.trim().startsWith("/*") && !line.trim().startsWith("*") && !line.trim().isEmpty()) {
                    linesOfCode++;
                    //}
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return linesOfCode;
        }
    }

    public static void computeLocWhen0(FileJava fileJava, List<List<FileJava>> fileJavaList){

        //qui calcolo le LOC dei file che avevano sizeLOC=0, prendendo le loc della versione precedente
        for(int i = 0; i < fileJavaList.size(); i++){
            for(int j = 0; j < fileJavaList.get(i).size(); j++){

                int locTemp = fileJavaList.get(i).get(j).getSizeLoc();
                if(fileJavaList.get(i).get(j).getFilename().equals(fileJava.getFilename()) && locTemp!=0){
                    fileJava.setSizeLoc(locTemp);

                }
            }
        }
    }

    public static int computeNumberAuthors(FileJava fileJava){

        int numberAuthors = 0;
        List<String> authors = new ArrayList<>();
        for(RevCommit commit : fileJava.getListCommmit()){
            String name = commit.getAuthorIdent().getName();
            if(!authors.contains(name)){
                authors.add(name);
            }
        }
        numberAuthors = authors.size();

        return numberAuthors;
    }

    public static int computeLocTouched(FileJava fileJava) throws IOException {

        //oltre a calcolare le loc touched, setto anche direttamente le loc added
        int locTouched = 0;
        int linesAddTotal = 0;
        int churnTotal = 0;

        List<Integer> listLocAdded = new ArrayList<>();
        List<Integer> listChurn = new ArrayList<>();

        try (DiffFormatter formatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            formatter.setRepository(repository);
            formatter.setDiffComparator(RawTextComparator.DEFAULT);
            formatter.setDetectRenames(true);
            for (RevCommit commit : fileJava.getListCommmit()) {
                if (commit.getTree() == null) continue;
                RevCommit parent = commit.getParent(0);
                if (parent == null) continue;
                List<DiffEntry> diffs = formatter.scan(parent.getTree(), commit.getTree());
                int churn = 0;
                for (DiffEntry diff : diffs) {
                    if (diff.getNewPath().equals(fileJava.getFilename())) {
                        int linesDel = 0;
                        int linesAdd = 0;

                        for (Edit edit : formatter.toFileHeader(diff).toEditList()) { //per ridurre complessita fai due metodi per calcolare linesDel e linesAdd
                            linesDel += edit.getEndA() - edit.getBeginA();
                            linesAdd += edit.getEndB() - edit.getBeginB();
                            churn += Math.abs(linesAdd - linesDel);
                        }

                        linesAddTotal += linesAdd;
                        locTouched += linesAdd + linesDel;
                        listLocAdded.add(linesAdd);

                    }
                }
                listChurn.add(churn);
                churnTotal += churn;
                //System.out.println(churnTotal);
            }

            fileJava.setAddedLoc(linesAddTotal); //setto le lines add total

            if(!listLocAdded.isEmpty()) { //qui setto le lines add MAX
                int maxLocAdd = Collections.max(listLocAdded);
                //System.out.println(listLocAdded);
                fileJava.setMaxLocAdded(maxLocAdd);
            } else {
                fileJava.setMaxLocAdded(0);
            }

            //ora calcolo e setto la average loc added
            int averageLocAdd = computeAverage(listLocAdded);
            fileJava.setAvgLocAdded(averageLocAdd);

            //CHURN
            fileJava.setChurn(churnTotal);

            if(!listChurn.isEmpty()) { //qui setto il MAX churn
                int maxChurn = Collections.max(listChurn);
                fileJava.setMaxChurn(maxChurn);
            } else {
                fileJava.setMaxChurn(0);
            }

            int averageChurn = computeAverage(listChurn);
            fileJava.setAvgChurn(averageChurn);

            return locTouched;
        }
    }

    public static int computeAverage(List<Integer> list){

        int sum = 0;
        int average = 0;
        for (Integer integer : list) {
            sum += integer;
        }
        if (list.size()!= 0){
            average = sum / list.size();
        }

        return average;
    }

    public List<FileJava> computeBuggynessProva(List<List<FileJava>> fileJavaList, List<Issue> bugsList){

        /*
            Prendo tutti i file, li scorro, scorro i commit di ognuno, scorro ancora i bug totali e verifico quando un
            bug è presente nello shortMessage di un commit. In quel caso mi genero una lista temporanea di file buggy
            con il metodo calcTempList e poi l'aggiungo a una lista buggyFiles da ritornare
        */
        List<FileJava> buggyFiles = new ArrayList<>();
        for(List<FileJava> listFile : fileJavaList){
            for(FileJava fileJava : listFile){
                for (RevCommit commit : fileJava.getListCommmit()) {
                    for (Issue bug : bugsList) {
                        if (commit.getShortMessage().contains(bug.getKey())) {
                            List<FileJava> temp = calcTempList(fileJavaList, bug.getAv(), fileJava.getFilename());
                            buggyFiles.addAll(temp);
                        }
                    }
                }
            }
        }
        return buggyFiles;
    }

    public List<FileJava> calcTempList(List<List<FileJava>> fileJavaList, List<Release> av, String fileName ) {

        /*
            qui verifico se il filename dato ha un corrisponde nella lista di file (stesso nome ma versione diversa)
            e quando lo trovo, scorrendo tutte le av, aggiungo alla lista il file la quale release è identificato come
            av per quel bug
         */

        List<FileJava> retList = new ArrayList<>();
        for (List<FileJava> listFile : fileJavaList) {
            for (FileJava file : listFile) {
                if (file.getFilename().equals(fileName)) {
                    for (Release rel : av) {
                        if (rel.getName().equals(file.getRelease().getName())) {
                            retList.add(file);
                        }
                    }
                }
            }

        }
        return retList;
    }

    //public static void computeBuggyness(FileJava fileJava, List<Issue> bugsList){
        /*
          - per la buggyness, prendo la lista dei commit del fileJava e verifico se fileJava.getCommitList() contiene un commit che ha come shortMessage un
            BOOKKEEPER-XXX (o xx xxx xxxx) (o ZOO). se si, scorro la lista di tutto i bug, prendo quello che ha il nome uguale a quello che ho appena trovato,
            prendo la versione, e, se le versioni affette contengono la versione del file, quel file è buggy
         */
        //fileJava.setBuggy("No");
        /*for(RevCommit com : fileJava.getListCommmit()){
            if(com.getShortMessage().contains("BOOKKEEPER-")){
                //System.out.println(com.getShortMessage().substring(0,14));
                for(Issue bug : bugsList){
                    if(com.getShortMessage().substring(0,13).equals(bug.getKey()) || com.getShortMessage().substring(0,13).equals(bug.getKey())){
                        System.out.println(bug.getKey() + "   " + bug.getAv() );
                        for(Release av : bug.getAv()){
                            if(av.getName().equals(fileJava.getRelease().getName())){
                                fileJava.setBuggy("Yes");
                                System.out.println("ENTRATO");
                                return;
                            }
                        }
                    }
                }
            }
        }*/

        /*fileJava.setBuggy("No");
        for (Issue bug : bugsList) {
            String bugKey = bug.getKey();
            if (bugKey.matches("BOOKKEEPER-\\d+")) {
                for (RevCommit commit : fileJava.getListCommmit()) {
                    //if(fileJava.getRelease().getName().equals("4.1.0")) System.out.println(commit.getShortMessage()+ "      bugkey:  " + bugKey +  " av: " + bug.getAv() + " file rel: " + fileJava.getRelease().getName()); //prova stampa
                    /*if (commit.getShortMessage().contains(bugKey)) {
                                fileJava.setBuggy("Yes");
                    }
                    /*if(bug.getAv().contains(fileJava.getRelease())) {
                        System.out.println("SECOND IFFF");
                        fileJava.setBuggy("yes");
                    }
                    if (commit.getShortMessage().contains(bugKey)) {
                        for (Release av : bug.getAv()) {
                            if (av.getName().equals(fileJava.getRelease().getName())) {
                                fileJava.setBuggy("Yes");
                                System.out.println("ENTRATO");
                                return;
                            }
                        }
                    }
                }



                        //break;
            }
        }
        //fileJava.setBuggy("No");

    }*/
}

