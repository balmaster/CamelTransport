package camel_transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: balmaster
 * Date: 21.01.14
 * Time: 1:28
 * To change this template use File | Settings | File Templates.
 */
class StateProcessor {
    public static final String A_RECORDS_CURRENT_ID = "aRecordsCurrentId";
    private String stateFile;

    public String getStateFile() {
        return stateFile;
    }

    public void setStateFile(String stateFile) {
        this.stateFile = stateFile;
    }

    protected Properties getProperties() throws IOException {
        Properties state = new Properties();
        File f = new File(getStateFile());
        if(f.exists()) {
            state.loadFromXML(new FileInputStream(f));
        }
        return state;
    }
}
