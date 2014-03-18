package camel_transport;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.commons.csv.writer.CSVConfig;
import org.apache.commons.csv.writer.CSVField;

import camel_transport.entity.Conversation;

public class ImporterRouteBuilder extends RouteBuilder {

	//@Resource 
	private Properties configProperties;

	public Properties getConfigProperties() {
		return configProperties;
	}

	public void setConfigProperties(Properties configProperties) {
		this.configProperties = configProperties;
	}

	@Override
	public void configure() throws Exception {
		
		CsvDataFormat csv = new CsvDataFormat();
		CSVConfig config = new CSVConfig();
		List<CSVField> fieldList = new ArrayList<CSVField>();

		fieldList.add(new CSVField("actFile"));
		fieldList.add(new CSVField("fromDate"));
		fieldList.add(new CSVField("toDate"));
		fieldList.add(new CSVField("metaObjToDate"));
		fieldList.add(new CSVField("metaObjId"));
		config.setFields(fieldList);
		csv.setConfig(config );
		
		from("file:{{import.csv_dir}}?autoCreate=true")
			.unmarshal(csv)
            .split(body())
			    .log(LoggingLevel.DEBUG, "${body}")
			    .setHeader("actFile", simple("${body[0]}"))
			    .setHeader("fromDate", simple("${body[1]}"))
			    .setHeader("toDate", simple("${body[2]}"))
			    .setHeader("metaObjToDate", simple("${body[3]}"))
			    .setHeader("metaObjId", simple("${body[4]}"))
			    .process(new Processor() {
                    
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message inMessage = exchange.getIn();
                        Map<String,Object> params = new HashMap<String,Object>();
                        params.put("actFile", inMessage.getHeader("actFile",String.class));
                        inMessage.setBody(params);
                    }
                })
			    .to("mybatis:selectConversation?statementType=SelectOne&executorType=reuse")
			    .choice()
			        .when(simple("${header[CamelMyBatisResult]} == null"))
			            .process(new Processor() {
                            
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Message inMessage = exchange.getIn();
                                List data = inMessage.getBody(List.class);

                                SimpleDateFormat dateFormat = new SimpleDateFormat("y-M-d H:m:s");
                                
                                Conversation conversation = new Conversation();
                                conversation.setActFile(inMessage.getHeader("actFile",String.class));
                                conversation.setFromDate(dateFormat.parse(inMessage.getHeader("fromDate",String.class)));
                                conversation.setToDate(dateFormat.parse(inMessage.getHeader("toDate",String.class)));
                                conversation.setMetaObjToDate(dateFormat.parse(inMessage.getHeader("metaObjToDate",String.class)));
                                conversation.setMetaObjId(inMessage.getHeader("metaObjId",Long.class));
                                inMessage.setBody(conversation);
                            }
                        })
                        .to("mybatis:insertConversation?statementType=Insert&executorType=reuse")
                        .log(LoggingLevel.INFO,"Conversation ${header[actFile]} added")
                    .otherwise()
                        .log(LoggingLevel.INFO,"Conversation ${header[actFile]} already exists");
		
	}

}
