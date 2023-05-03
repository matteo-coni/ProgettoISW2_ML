package control;

import model.FileJava;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvController {

    public void makeCsv(List<List<FileJava>> fileJavaList) throws IOException {

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

        csvWriter.flush();
        csvWriter.close();

    }

}
