package control;

import model.ClassifierInfo;
import model.FileJava;
import model.Issue;
import model.Release;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WekaController {

    private static final String RANDOM_FOREST = "Random Forest";
    private static final String NAIVE_BAYES = "Naive Bayes";
    private static final String IBK = "IBk";
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


    public void computeClassifier(int numberIterator, String projName) throws Exception {

        //liste di classificatori semplici, senza ulteriori parametri
        List<ClassifierInfo> randomForestList = new ArrayList<>();
        List<ClassifierInfo> naiveBayesList = new ArrayList<>();
        List<ClassifierInfo> ibkList = new ArrayList<>();

        //liste con feature selection
        List<ClassifierInfo> featureRandomForestList = new ArrayList<>();
        List<ClassifierInfo> featureNaiveBayesList = new ArrayList<>();
        List<ClassifierInfo> featureIbkList = new ArrayList<>();

        //list con sampling (bilanciamento)
        List<ClassifierInfo> samplingRandomForestList = new ArrayList<>();
        List<ClassifierInfo> samplingNaiveBayesList = new ArrayList<>();
        List<ClassifierInfo> samplingIbkList = new ArrayList<>();

        //list con cost sensitive
        List<ClassifierInfo> costSensRandomForestList = new ArrayList<>();
        List<ClassifierInfo> costSensNaiveBayesList = new ArrayList<>();
        List<ClassifierInfo> costSensSamplingIbkList = new ArrayList<>();

        int countTest = 3;
        for( int count = 2; count < numberIterator; count++){

            /*
                in questo ciclo for devo iterare tante volte quante sono le release(metà) per il walk forward
                e mettere i risultati ogni volta nella lista appropriata a quel classificatore
             */

            DataSource source1 = new DataSource(projName + "_training_" + count + ".arff");
            System.out.println(projName + "_training_" + count + ".arff");
            DataSource source2 = new DataSource(projName + "_testing_" + countTest + ".arff");
            System.out.println(projName + "_testing_" + countTest + ".arff");
            countTest++;

            //creo gli oggetti istanze per lavorare su training e testing separatamente
            Instances training = source1.getDataSet();
            Instances testing = source2.getDataSet();

            RandomForest randomForestClassifier = new RandomForest();
            NaiveBayes naiveBayesClassifier = new NaiveBayes();
            IBk ibkClassifier = new IBk();

            int numAttr = training.numAttributes();
            training.setClassIndex(numAttr - 1);
            testing.setClassIndex(numAttr - 1);

            Evaluation eval = new Evaluation(testing);

            //blocco try-catch per problema quando tutti i buggy sono NO nell'ultimo testing
            try {
                randomForestClassifier.buildClassifier(training);
                eval.evaluateModel(randomForestClassifier, testing);
                ClassifierInfo simpleRandomForest = new ClassifierInfo(this.projName, count, RANDOM_FOREST, false, false, false);
                simpleRandomForest.setTrainingPercent(100.0 * training.numInstances() / (training.numInstances() + testing.numInstances()));
                simpleRandomForest.setPrecision(eval.precision(0));
                simpleRandomForest.setRecall(eval.recall(0));
                simpleRandomForest.setAuc(eval.areaUnderROC(0));
                simpleRandomForest.setKappa(eval.kappa());
                simpleRandomForest.setTp(eval.numTruePositives(0));
                simpleRandomForest.setFp(eval.numFalsePositives(0));
                simpleRandomForest.setTn(eval.numTrueNegatives(0));
                simpleRandomForest.setFn(eval.numFalseNegatives(0));
                randomForestList.add(simpleRandomForest);
            } catch (ArrayIndexOutOfBoundsException e){
                System.out.println("Exception: " + e.getMessage());
            }

            naiveBayesClassifier.buildClassifier(training);
            eval.evaluateModel(naiveBayesClassifier, testing);
            ClassifierInfo simpleNaiveBayes = new ClassifierInfo(this.projName, count, NAIVE_BAYES, false, false, false);
            simpleNaiveBayes.setTrainingPercent(100.0 * training.numInstances() / (training.numInstances() + testing.numInstances()));
            simpleNaiveBayes.setPrecision(eval.precision(0));
            simpleNaiveBayes.setRecall(eval.recall(0));
            simpleNaiveBayes.setAuc(eval.areaUnderROC(0));
            simpleNaiveBayes.setKappa(eval.kappa());
            simpleNaiveBayes.setTp(eval.numTruePositives(0));
            simpleNaiveBayes.setFp(eval.numFalsePositives(0));
            simpleNaiveBayes.setTn(eval.numTrueNegatives(0));
            simpleNaiveBayes.setFn(eval.numFalseNegatives(0));
            naiveBayesList.add(simpleNaiveBayes);

            ibkClassifier.buildClassifier(training);
            eval.evaluateModel(ibkClassifier, testing);
            ClassifierInfo simpleIBk = new ClassifierInfo(this.projName, count, IBK, false, false, false);
            simpleIBk.setTrainingPercent(100.0 * training.numInstances() / (training.numInstances() + testing.numInstances()));
            simpleIBk.setPrecision(eval.precision(0));
            simpleIBk.setRecall(eval.recall(0));
            simpleIBk.setAuc(eval.areaUnderROC(0));
            simpleIBk.setKappa(eval.kappa());
            simpleIBk.setTp(eval.numTruePositives(0));
            simpleIBk.setFp(eval.numFalsePositives(0));
            simpleIBk.setTn(eval.numTrueNegatives(0));
            simpleIBk.setFn(eval.numFalseNegatives(0));
            ibkList.add(simpleIBk);

        }

        System.out.println("Random forest: ");
        for(int i=0;i<randomForestList.size();i++){
            System.out.println("WFindex: " + randomForestList.get(i).getWalkForwardIterationIndex() + " Precision: " + randomForestList.get(i).getPrecision() +
                    " Recall: " + randomForestList.get(i).getRecall() + " AUC: " + randomForestList.get(i).getAuc() + " Kappa: " + randomForestList.get(i).getKappa());
        }
        System.out.println("Naive Bayes: ");
        for(int i=0;i<naiveBayesList.size();i++){
            System.out.println("WFindex: " + naiveBayesList.get(i).getWalkForwardIterationIndex() + " Precision: " + naiveBayesList.get(i).getPrecision() +
                    " Recall: " + naiveBayesList.get(i).getRecall() + " AUC: " + naiveBayesList.get(i).getAuc() + " Kappa: " + naiveBayesList.get(i).getKappa());
        }

        System.out.println("IBk: ");
        for(int i=0;i<ibkList.size();i++){
            System.out.println("WFindex: " + ibkList.get(i).getWalkForwardIterationIndex() + " Precision: " + ibkList.get(i).getPrecision() +
                    " Recall: " + ibkList.get(i).getRecall() + " AUC: " + ibkList.get(i).getAuc() + " Kappa: " + ibkList.get(i).getKappa());
        }
    }
}
