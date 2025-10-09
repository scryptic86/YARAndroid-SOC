package com.netsec.clamav;

public class ScanRecord {
    public String timestamp;
    public String filename;
    public String result;
    public String reportPath;

    public ScanRecord() {}

    public ScanRecord(String timestamp, String filename, String result, String reportPath) {
        this.timestamp = timestamp;
        this.filename = filename;
        this.result = result;
        this.reportPath = reportPath;
    }
}
