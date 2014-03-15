package camel_transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

class StateProcessor implements Processor {
    public static final String A_RECORDS_CURRENT_ID = "aRecordsCurrentId";
    private String stateFile;
    private String method;
    
    public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

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

	@Override
	public void process(Exchange exchange) throws Exception {
		if(method.equals("load")) {
			processLoad(exchange);
		}
		else if (method.equals("save")) {
			processSave(exchange);
		}
		else if (method.equals("getMaxIdAndSave")) {
			processGetMaxIdAndSave(exchange);
		}
	}

	private void processGetMaxIdAndSave(Exchange exchange) throws IOException {
        Properties p = getProperties();

        Message inMessage = exchange.getIn();

        List<Map<String,Object>> messageList = (List<Map<String, Object>>) inMessage.getBody();
      
        Integer maxId = inMessage.getHeader(A_RECORDS_CURRENT_ID, Integer.class);
        for (Map<String,Object> m : messageList) {
        	maxId = Math.max(maxId,(Integer) m.get("ID"));
		}
        
        p.setProperty(A_RECORDS_CURRENT_ID,maxId.toString());
        String a = new File(getStateFile()).getAbsolutePath(); 
        p.storeToXML(new FileOutputStream(new File(getStateFile())),"","utf-8");
	}

	private void processSave(Exchange exchange) throws IOException {
        Properties p = getProperties();

        Message inMessage = exchange.getIn();

        p.setProperty(A_RECORDS_CURRENT_ID,((Integer)inMessage.getHeader(A_RECORDS_CURRENT_ID)).toString());
        p.storeToXML(new FileOutputStream(new File(getStateFile())),"","utf-8");
	}

	private void processLoad(Exchange exchange) throws IOException {
        Properties state = getProperties();
        Message outMessage = exchange.getOut();
        outMessage.setHeader(A_RECORDS_CURRENT_ID,Integer.parseInt(state.getProperty(A_RECORDS_CURRENT_ID,"0")));
	}
}
