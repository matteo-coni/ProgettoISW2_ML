package control;

import model.FileJava;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class MetricsController {


    public static void computeMetrics(FileJava fileJava, List<List<FileJava>> fileJavaList) throws IOException {

        System.out.println("\n");
        int loc = setSizeLoc(fileJava);
        fileJava.setSizeLoc(loc); //setta l'attributo
        //se non ci sono commit che toccano un file, esso avrà loc=0, quindi per risolvere prendo le loc del file nella versione precedente

        if(fileJava.getSizeLoc()==0){
            System.out.println(loc);
            computeLocWhen0(fileJava, fileJavaList);
        }

        System.out.println("release " + fileJava.getRelease().getName() + " file: " + fileJava.getFilename() + " LOC: " + fileJava.getSizeLoc());

        int numberAuthors = computeNumberAuthors(fileJava);
        fileJava.setNumberAuthors(numberAuthors);
        System.out.println("release " + fileJava.getRelease().getName() + " file: " + fileJava.getFilename() + " LOC: " + fileJava.getSizeLoc() + " nr authors: " + fileJava.getNumberAuthors());
    }

    public static int setSizeLoc(FileJava fileJava) throws IOException {


        String localPath = "/Users/matteo/IdeaProjects/bookkeeper";
        Git git = Git.open(new File(localPath));
        Repository repository = git.getRepository();

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

                while ((reader.readLine()) != null) {
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
                    //System.out.println("oooooooooooooo " + "   loc " + fileJava.getSizeLoc());
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
}

