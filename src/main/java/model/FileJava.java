package model;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.ArrayList;
import java.util.List;

public class FileJava {

    /* classe model che mi permette di mantenere informazioni e metriche sul file
        - size (LOC)
        - LOC touched (somma di loc eliminate e aggiunte)
        - number of authors
        - nr: number of revision
        - LOC added
        - max LOC added
        - avg LOc added
        - churn
        - max churn
        - avg churn

     */
    private String filename;
    private Release release;
    private List<RevCommit> listCommmit;
    private int sizeLoc; //
    private int touchedLoc; //
    private int numberAuthors; //
    private int nr; //
    private int addedLoc; //
    private int maxLocAdded; //
    private int avgLocAdded; //
    private int churn; //
    private int maxChurn; //
    private int avgChurn; //



    /*
      costruttore con filename e release del file .java
    */
    public FileJava (String filename, Release release){

        this.filename = filename;
        this.release = release;
        this.listCommmit = new ArrayList<>();

    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Release getRelease() {
        return release;
    }

    public void setRelease(Release release) {
        this.release = release;
    }

    public int getSizeLoc() {
        return sizeLoc;
    }

    public void setSizeLoc(int sizeLOC) {
        this.sizeLoc = sizeLOC;
    }

    public List<RevCommit> getListCommmit() {
        return listCommmit;
    }

    public void setListCommit(List<RevCommit> listCommmit) {
        this.listCommmit = listCommmit;
    }

    public int getTouchedLoc() {
        return touchedLoc;
    }

    public void setTouchedLoc(int touchedLoc) {
        this.touchedLoc = touchedLoc;
    }

    public int getNumberAuthors() {
        return numberAuthors;
    }

    public void setNumberAuthors(int numberAuthors) {
        this.numberAuthors = numberAuthors;
    }

    public int getNr() {
        return nr;
    }

    public void setNr(int nr) {
        this.nr = nr;
    }

    public int getAddedLoc() {
        return addedLoc;
    }

    public void setAddedLoc(int addedLoc) {
        this.addedLoc = addedLoc;
    }

    public int getMaxLocAdded() {
        return maxLocAdded;
    }

    public void setMaxLocAdded(int maxLocAdded) {
        this.maxLocAdded = maxLocAdded;
    }

    public int getAvgLocAdded() {
        return avgLocAdded;
    }

    public void setAvgLocAdded(int avgLocAdded) {
        this.avgLocAdded = avgLocAdded;
    }

    public int getChurn() {
        return churn;
    }

    public void setChurn(int churn) {
        this.churn = churn;
    }

    public int getMaxChurn() {
        return maxChurn;
    }

    public void setMaxChurn(int maxChurn) {
        this.maxChurn = maxChurn;
    }

    public int getAvgChurn() {
        return avgChurn;
    }

    public void setAvgChurn(int avgChurn) {
        this.avgChurn = avgChurn;
    }
}
