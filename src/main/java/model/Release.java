package model;

import java.sql.Date;
import java.time.LocalDateTime;

public class Release {

    private int id;
    private Date date;
    private String name;

    public Release(int id, String name, Date date) {
        this.id = id;
        this.date = date;
        this.name = name;
    }

    public Release(String name, Date date ) {
        this.name = name;
        this.date = date;

    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
