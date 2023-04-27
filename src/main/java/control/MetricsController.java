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



public class MetricsController {


    public static void computeMetrics(FileJava fileJava) throws IOException {

        System.out.println("\n");
        System.out.println("release " + fileJava.getRelease().getName() + " file: " + fileJava.getFilename() + " LOC: " + setSizeLoc(fileJava));
        //todo: problema loc = 0 quando nessun commit tocca quel file -> in questo caso prendi le loc del file precedente
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

}

