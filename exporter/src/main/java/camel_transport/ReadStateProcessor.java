package camel_transport;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: balmaster
 * Date: 21.01.14
 * Time: 1:31
 * To change this template use File | Settings | File Templates.
 */
public class ReadStateProcessor extends StateProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        Properties state = getProperties();

        Message outMessage = exchange.getOut();
        outMessage.setHeader(A_RECORDS_CURRENT_ID,Integer.parseInt(state.getProperty(A_RECORDS_CURRENT_ID,"0")));
    }

}
