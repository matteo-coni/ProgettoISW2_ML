package control;

import model.ClassifierInfo;
import model.FileJava;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CsvController {

    private final String projName;
    public CsvController(String projName){
        this.projName = projName;
    }

    public void makeCsv(List<List<FileJava>> fileJavaList, String projName, int countRelease) throws IOException {

        List<List<String>> listListString = new ArrayList<>();

        for (List<FileJava> listFile : fileJavaList) {
            for (FileJava fileJava : listFile) {
                List<String> temp = new ArrayList<>();
                temp.add(fileJava.getRelease().getName());
                temp.add(fileJava.getFilename());
                temp.add(String.valueOf(fileJava.getSizeLoc()));
                temp.add(String.valueOf(fileJava.getNr()));
                temp.add(String.valueOf(fileJava.getNumberAuthors()));
                temp.add(String.valueOf(fileJava.getTouchedLoc()));
                temp.add(String.valueOf(fileJava.getAddedLoc()));
                temp.add(String.valueOf(fileJava.getMaxLocAdded()));
                temp.add(String.valueOf(fileJava.getAvgLocAdded()));
                temp.add(String.valueOf(fileJava.getChurn()));
                temp.add(String.valueOf(fileJava.getMaxChurn()));
                temp.add(String.valueOf(fileJava.getAvgChurn()));
                temp.add(String.valueOf(fileJava.getBuggy()));
                listListString.add(temp);

            }

        }

        String csvFilePath;

        csvFilePath = projName + "_training_" + countRelease + ".csv";

        String[] header = {"Release", "Filename", "LOC", "NR", "Authors", "Loc Touched", "Loc added", "LOC added",
                            "Avg LOC added", "Churn", "Max Churn", "Avg Churn", "Buggy"};

        FileWriter writer = new FileWriter(csvFilePath);

        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(header));

        for (List<String> row : listListString) {
            printer.printRecord(row);
        }

        // Chiudi il printer e il writer
        printer.close();
        writer.close();
    }

    public void generateArff(String projName, int countRelease, String training) throws Exception {

        // load CSV
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File("/Users/matteo/IdeaProjects/ProgettoISW2_ML/" + projName + "_" + training + "_" + countRelease + ".csv"));
        Instances data = loader.getDataSet();

        //rimuovo l'attributo filename
        String[] options = new String[]{"-R", "2"};
        Remove removeFilter = new Remove();
        removeFilter.setOptions(options);
        removeFilter.setInputFormat(data);
        Instances newData = Filter.useFilter(data, removeFilter);

        // save ARFF
        ArffSaver saver = new ArffSaver();
        //set the dataset we want to convert
        saver.setInstances(newData);
        //and save as ARFF
        saver.setFile(new File("/Users/matteo/IdeaProjects/ProgettoISW2_ML/" + projName + "_" + training + "_" + countRelease + ".arff"));
        saver.writeBatch();
    }


    public void makeCsvTesting(List<FileJava> fileJavaRel, String projName, int i) throws IOException {

        List<List<String>> listListString = new ArrayList<>();

        for (FileJava fileJava2 : fileJavaRel) {
            List<String> temp = new ArrayList<>();
            temp.add(fileJava2.getRelease().getName());
            temp.add(fileJava2.getFilename());
            temp.add(String.valueOf(fileJava2.getSizeLoc()));
            temp.add(String.valueOf(fileJava2.getNr()));
            temp.add(String.valueOf(fileJava2.getNumberAuthors()));
            temp.add(String.valueOf(fileJava2.getTouchedLoc()));
            temp.add(String.valueOf(fileJava2.getAddedLoc()));
            temp.add(String.valueOf(fileJava2.getMaxLocAdded()));
            temp.add(String.valueOf(fileJava2.getAvgLocAdded()));
            temp.add(String.valueOf(fileJava2.getChurn()));
            temp.add(String.valueOf(fileJava2.getMaxChurn()));
            temp.add(String.valueOf(fileJava2.getAvgChurn()));
            temp.add(String.valueOf(fileJava2.getBuggy()));
            listListString.add(temp);
        }

        String csvFilePath;

        csvFilePath = projName + "_testing_" + i + ".csv";

        String[] header2 = {"Release", "Filename", "LOC", "NR", "Authors", "Loc Touched", "Loc added", "Max LOC added",
                "Avg LOC added", "Churn", "Max Churn", "Avg Churn", "Buggy"};

        FileWriter writer = new FileWriter(csvFilePath);

        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(header2));

        for (List<String> row : listListString) {
            printer.printRecord(row);
        }

        // Chiudi il printer e il writer
        printer.close();
        writer.close();
    }

    public void makeCsvForReport(List<List<ClassifierInfo>> classifierList) throws IOException {

        List<List<String>> listListString = new ArrayList<>();

        for (List<ClassifierInfo> listClass : classifierList) {
            for (ClassifierInfo classInfo : listClass){
                List<String> temp = new ArrayList<>();
                temp.add(classInfo.getProjName());
                temp.add(String.valueOf(classInfo.getWalkForwardIterationIndex()));
                temp.add(String.valueOf(classInfo.getTrainingPercent()));
                temp.add(String.valueOf(classInfo.getClassifier()));
                temp.add(String.valueOf(classInfo.isFeatureSelection()));
                temp.add(String.valueOf(classInfo.isSampling()));
                temp.add(String.valueOf(classInfo.isCostSensitive()));
                temp.add(String.valueOf(classInfo.getPrecision()));
                temp.add(String.valueOf(classInfo.getRecall()));
                temp.add(String.valueOf(classInfo.getAuc()));
                temp.add(String.valueOf(classInfo.getKappa()));
                temp.add(String.valueOf(classInfo.getTn()));
                temp.add(String.valueOf(classInfo.getTp()));
                temp.add(String.valueOf(classInfo.getFn()));
                temp.add(String.valueOf(classInfo.getFp()));
                listListString.add(temp);
            }
        }

        String csvFilePathReport;

        csvFilePathReport = projName + "_Report_" + classifierList.get(0).get(0).getClassifier() + ".csv";

        String[] header2 = {"Dataset", "#TrainingRelease", "%Training", "Classifier", "FeatureSel", "Sampling", "CostSensitive", "Precision",
                "Recall", "AUC", "Kappa", "TN", "TP", "FN", "FP"};

        FileWriter writer = new FileWriter(csvFilePathReport);
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(header2));

        for (List<String> row : listListString) {
            printer.printRecord(row);
        }

        // Chiudi il printer e il writer
        printer.close();
        writer.close();
    }
}
