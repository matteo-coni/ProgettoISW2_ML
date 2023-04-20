package model;

public class File {

    /* classe model che mi permette di mantenere informazioni e metriche sul file
        -

     */
    private String filename;
    private Release release;
    private int sizeLOC;

    /*
      costruttore con filename e release del file .java
    */
    public File (String filename, Release release){

        this.filename = filename;
        this.release = release;

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

    public int getSizeLOC() {
        return sizeLOC;
    }

    public void setSizeLOC(int sizeLOC) {
        this.sizeLOC = sizeLOC;
    }
}
