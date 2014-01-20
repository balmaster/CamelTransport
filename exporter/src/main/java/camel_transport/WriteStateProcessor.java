package camel_transport;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: balmaster
 * Date: 21.01.14
 * Time: 2:01
 * To change this template use File | Settings | File Templates.
 */
public class WriteStateProcessor extends StateProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        Properties p = getProperties();

        Message inMessage = exchange.getIn();

        p.setProperty(A_RECORDS_CURRENT_ID,((Integer)inMessage.getHeader(A_RECORDS_CURRENT_ID)).toString());
        p.storeToXML(new FileOutputStream(new File(getStateFile())),"","utf-8");
    }
}
