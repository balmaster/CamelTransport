package camel_transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

public class MapOperatorProcessor implements Processor {

	public static final String HEADER_OPERATOR_NAME = "operatorName";
	private Map<String,String> nameMap = new HashMap<String,String>();
	private String nameMapFile;
	
	
	public String getNameMapFile() {
		return nameMapFile;
	}

	public void setNameMapFile(String nameMapFile) throws IOException {
		this.nameMapFile = nameMapFile;
		
		for ( Entry<Object, Object> e : getProperties().entrySet()) {
			nameMap.put((String)e.getKey(),(String)e.getValue());
		}
	}

	public Map<String, String> getNameMap() {
		return nameMap;
	}

	public void setNameMap(Map<String, String> nameMap) {
		this.nameMap = nameMap;
	}

    protected Properties getProperties() throws IOException {
        Properties state = new Properties();
        File f = new File(getNameMapFile());
        if(f.exists()) {
            state.loadFromXML(new FileInputStream(f));
        }
        return state;
    }
	
	@Override
	public void process(Exchange exchange) throws Exception {
		Message inMessage = exchange.getIn();
		String inOperatorName = (String) inMessage.getHeader(HEADER_OPERATOR_NAME);
		String outOperatorName = nameMap.get(inOperatorName);
		inMessage.setHeader(HEADER_OPERATOR_NAME, outOperatorName);
	}

}
