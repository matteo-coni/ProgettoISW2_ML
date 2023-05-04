package control;

import model.FileJava;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;


public class CsvController {

    public void makeCsv(List<List<FileJava>> fileJavaList, String projName) throws IOException {

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

        String csvFilePath = "";

        if(projName.equals("BOOKKEEPER")) csvFilePath = "BOOKKEEPER_filejava_metrics.csv";
        if(projName.equals("ZOOKEEPER")) csvFilePath = "ZOOKEEPER_filejava_metrics.csv";


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
        /*
        String csvFilePath = "filejava_metrics.csv";
        FileWriter csvWriter = new FileWriter(csvFilePath);
        csvWriter.append("Release, Filename, LOC, NR, Authors, Loc Touched, Loc added, Max LOC added, Avg LOC added, Churn, Max Churn, Avg Churn, Buggy\n");

        for(List<FileJava> listFile : fileJavaList){
            for(FileJava fileJava : listFile){
                csvWriter.append(fileJava.getRelease().getName()).append(",");
                csvWriter.append(fileJava.getFilename()).append(",");
                csvWriter.append(fileJava.getSizeLoc() + ",");
                csvWriter.append(fileJava.getNr() + ",");
                csvWriter.append(fileJava.getNumberAuthors() +",");
                csvWriter.append(fileJava.getTouchedLoc() +",");
                csvWriter.append(fileJava.getAddedLoc() + ",");
                csvWriter.append(fileJava.getMaxLocAdded() + ",");
                csvWriter.append(fileJava.getAvgLocAdded() + ",");
                csvWriter.append(fileJava.getChurn() + ",");
                csvWriter.append(fileJava.getMaxChurn() + ",");
                csvWriter.append(fileJava.getAvgChurn() + ",");
                csvWriter.append(fileJava.getBuggy() + ",");
                csvWriter.append("\n");
            }
        }
        //String [] headers = {"ciao"};

        //CSVPrinter printer = new CSVPrinter(csvWriter, CSVFormat.DEFAULT.withHeader(headers));
        //printer.printRecord(csvWriter);

        csvWriter.flush();
        csvWriter.close();


    }*/

    public void generateArff(String projName) throws Exception {

        // load CSV
        CSVLoader loader = new CSVLoader();
        if(projName.equals("BOOKKEEPER")) loader.setSource(new File("/Users/matteo/IdeaProjects/ProgettoISW2_ML/BOOKKEEPER_filejava_metrics.csv"));
        if(projName.equals("ZOOKEEPER")) loader.setSource(new File("/Users/matteo/IdeaProjects/ProgettoISW2_ML/ZOOKEEPER_filejava_metrics.csv"));
        Instances data = loader.getDataSet();//get instances object

        // save ARFF
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);//set the dataset we want to convert
        //and save as ARFF
        if(projName.equals("BOOKKEEPER")) saver.setFile(new File("/Users/matteo/IdeaProjects/ProgettoISW2_ML/BOOKKEEPER_output_metrics.arff"));
        if(projName.equals("ZOOKEEPER")) saver.setFile(new File("/Users/matteo/IdeaProjects/ProgettoISW2_ML/ZOOKEEPER_output_metrics.arff"));
        saver.writeBatch();
    }

}
