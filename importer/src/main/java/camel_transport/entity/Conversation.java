package camel_transport.entity;

import java.util.Date;

public class Conversation {
    private String actFile;
    private Date fromDate;
    private Date toDate;
    private Date metaObjToDate;
    private Long metaObjId;
    public String getActFile() {
        return actFile;
    }
    public void setActFile(String actFile) {
        this.actFile = actFile;
    }
    public Date getFromDate() {
        return fromDate;
    }
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }
    public Date getToDate() {
        return toDate;
    }
    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }
    public Date getMetaObjToDate() {
        return metaObjToDate;
    }
    public void setMetaObjToDate(Date metaObjToDate) {
        this.metaObjToDate = metaObjToDate;
    }
    public Long getMetaObjId() {
        return metaObjId;
    }
    public void setMetaObjId(Long metaObjId) {
        this.metaObjId = metaObjId;
    }
    
}
