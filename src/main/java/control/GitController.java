package control;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class GitController {

    public static void main(String[] args) throws IOException, GitAPIException {

        String jiraPattern = "BOOKEEPER-\\d+";
        String remoteUrl = "https://github.com/apache/bookkeeper.git";
        String localPath = "/Users/matteo/IdeaProjects/Prova_cloneJgit";
        File dir = new File(localPath);




        try (Git git = Git.cloneRepository()
                .setURI(remoteUrl)
                .setDirectory(dir)
                .call()) {

            List<Ref> branches = git.branchList().call();
            System.out.println(branches.size());

            Iterable<RevCommit> commits = git.log().all().call();
            int count = 0;
            for (RevCommit commit : commits) {
                System.out.println("Commit: " + commit.getName() + " " + commit.getShortMessage());
                count++;
            }
            System.out.println(count);

            try (Git gits = Git.open(new File(localPath))) {
                List<Ref> branchess = gits.branchList().call();
                for (Ref branch : branchess) {
                    try (RevWalk revWalk = new RevWalk(git.getRepository())) {
                        RevCommit commit = revWalk.parseCommit(branch.getObjectId());
                        revWalk.markStart(commit);
                        Iterator<RevCommit> commitIterator = revWalk.iterator();
                        while (commitIterator.hasNext()) {
                            RevCommit rev = commitIterator.next();
                            //System.out.println("Commit " + rev.getName());
                            String version = getVersion(gits, rev);
                            TreeWalk treeWalk = new TreeWalk(git.getRepository());
                            treeWalk.addTree(rev.getTree());
                            treeWalk.setRecursive(true);
                            int count2 = 0;
                            while (treeWalk.next()) {
                                if (!treeWalk.isSubtree() && treeWalk.getPathString().endsWith(".java") && !treeWalk.getPathString().contains("/test/")) {
                                    count2++;
                                    System.out.println("File " + treeWalk.getPathString() + " file numero: " + count2 + "  nella versione " + version);
                                }
                            }
                        }
                    }
                    System.out.println(branchess.size());
                }
            }


        } catch (GitAPIException e) {
            System.out.println("Exception occurred while cloning repository: " + e.getMessage());
        } finally {
            deleteDirectory(dir);
        }

    }

    private static String getVersion(Git git, RevCommit commit) throws GitAPIException, IOException {
        try {
            List<Ref> tags = git.tagList().call();

            for (Ref tag : tags) {
                ObjectId tagObjectId = tag.getObjectId();
                RevWalk revWalk = new RevWalk(git.getRepository());
                RevTag tagObject = revWalk.parseTag(tagObjectId);
                revWalk.dispose();
                if (tagObject.getObject().equals(commit)) {
                    return tag.getName();
                }
            }
        } catch (Exception e) {
                System.out.println("eccezione ok");

        }
        return "unknown";
    }
   //In questo esempio, il metodo getVersion restituisce la versione corrispondente al commit specificato.




    public static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }


}
