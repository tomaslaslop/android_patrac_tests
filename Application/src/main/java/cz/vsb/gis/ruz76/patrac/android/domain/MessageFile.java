package cz.vsb.gis.ruz76.patrac.android.domain;

/**
 * Class for handling messages.
 */

public class MessageFile {
    private String message;
    private String filename;
    public MessageFile(String message, String filename) {
        this.message = message;
        this.filename = filename;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
