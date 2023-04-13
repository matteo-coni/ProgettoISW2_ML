package model;

import java.util.List;

public class Issue {

    private String key;
    private Release iv;
    private Release ov;
    private Release fv;
    private List<Release> av;

    private int num;

    public Issue(String key, Release iv, Release ov, Release fv, List<Release> av, int num) {
        this.key = key;
        this.iv = iv;
        this.ov = ov;
        this.fv = fv;
        this.av = av;
        this.num = num;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Release getIv() {
        return iv;
    }

    public void setIv(Release iv) {
        this.iv = iv;
    }

    public Release getOv() {
        return ov;
    }

    public void setOv(Release ov) {
        this.ov = ov;
    }

    public Release getFv() {
        return fv;
    }

    public void setFv(Release fv) {
        this.fv = fv;
    }

    public List<Release> getAv() {
        return av;
    }

    public void setAv(List<Release> av) {
        this.av = av;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
