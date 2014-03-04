package camel_transport;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.commons.csv.writer.CSVConfig;
import org.apache.commons.csv.writer.CSVField;

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
			.split(constant(true));
	}

}
