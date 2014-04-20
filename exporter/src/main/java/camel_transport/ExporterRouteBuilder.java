package camel_transport;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;

import oracle.jdbc.proxy.annotation.GetProxy;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder; 
import org.apache.camel.component.exec.ExecBinding;
import org.apache.camel.component.file.remote.RemoteFileConfiguration.PathSeparator;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.component.properties.PropertiesParser;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.commons.csv.CSVStrategy;
import org.apache.commons.csv.writer.CSVConfig;
import org.apache.commons.csv.writer.CSVField;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.config.PropertiesFactoryBean;

public class ExporterRouteBuilder extends RouteBuilder {

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
		
		
		final int toDateDeltaMin =  Integer.parseInt(getConfigProperties().getProperty("meta.to_date_delta_min"));
		
		from("timer://timer1?period={{interval}}")
		    .to("direct:start");
		
		from("direct:start")
		    .setHeader("processEndpoint", constant("direct:process"))
		    .dynamicRouter(header("processEndpoint"))
		    .log(LoggingLevel.DEBUG,"no new records found");
		
		from("direct:process")
			.processRef("loadState")
			.log(LoggingLevel.DEBUG, "max record id: ${header[aRecordsCurrentId]}")
			.setBody()
				.constant(new StringBuilder()
					.append(" SELECT ar.ID, ") 
                    .append(" doubletodatetime(ar.REC_DATETIME) as FROM_DATE,") 
                    .append(" doubletodatetime(ar.REC_DATETIME + ar.REC_LENGTH) as TO_DATE,") 
					.append(" au.NAME as OPERATOR_NAME,")
					.append(" ar.FILE_A, ")
					.append(" ar.FILE_B,")
                    .append(" ar.NUMBER_B, ar.NOTE_A")
                    .append(" FROM A_RECORDS ar left join A_USERS au on ar.abonent_b_id = au.id") 
                    .append(" where ar.abonent_b_id is not null")
                    .append(" and ar.id > :?aRecordsCurrentId ")
                    .append(" order by ar.id").toString())
            .to("jdbc:infDataSource?readSize={{inf.read_size}}&useHeadersAsParameters=true")
            .log(LoggingLevel.DEBUG, "query records: ${header[CamelJdbcRowCount]}")
            .choice()
                .when(simple("${header[CamelJdbcRowCount]} > 0"))
                    .setHeader("processEndpoint",constant("direct:process"))
                .otherwise()    
                    .setHeader("processEndpoint",constant(null))
            .end()
            .log(LoggingLevel.DEBUG,"process")
            .split(body()).parallelProcessing().executorServiceRef("metaQueryPool")
            	.setHeader("aRecordsCurrentId").simple("${body['ID']}")
				.setHeader("operatorName").simple("${body['OPERATOR_NAME']}")
				.setHeader("fromDate").simple("${body['FROM_DATE']}")
				.setHeader("toDate").simple("${body['TO_DATE']}")
				.setHeader("fileA").simple("${body['FILE_A']}")
				.setHeader("fileB").simple("${body['FILE_B']}")
				.setHeader("actFile").simple("${body['ID']}.mp3")
				.processRef("mapOperator")
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						Message inMessage = exchange.getIn();
						Timestamp toDate = (Timestamp) inMessage.getHeader("toDate");
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(toDate.getTime());
						cal.add(Calendar.MINUTE,  toDateDeltaMin);
						inMessage.setHeader("toDate",new Timestamp(cal.getTimeInMillis()));
					}
				})
				.choice()
					.when(header(MapOperatorProcessor.HEADER_OPERATOR_NAME).isNotNull())
						.setBody(constant(new StringBuilder()
							.append(" select mos.to_date,mos.meta_obj_id,mos.act_id, pu.name")
							.append(" from meta_obj_stats mos join ps_user pu on mos.ps_user_id=pu.id")
							.append(" join meta_obj mo on mos.meta_obj_id=mo.id ")
							.append(" where")
							.append(" (mos.to_date between cast(:?fromDate as date) and")
							.append(" cast(:?toDate as date))")
							.append(" and pu.name = :?operatorName ")
							.append(" and mo.is_pay=1 ")
							.toString()))
						.to("jdbc:metaDataSource?readSize=1&useHeadersAsParameters=true")
						.split(body())
							.setHeader("metaObjId").simple("${body['META_OBJ_ID']}")
							.setHeader("actId").simple("${body['ACT_ID']}")
							.setHeader("metaObjToDate").simple("${body['TO_DATE']}")
							.inOnly("seda:convert")
						.end()	
					.end()
				.end()
			.processRef("saveState");
		
		from("seda:convert")
			.setHeader("metaObjToDate").simple("${body['TO_DATE']}")
			.process(new Processor() {

                @Override
                public void process(Exchange exchange) throws Exception {
                    Message inMessage = exchange.getIn();
                    inMessage.setHeader("fileA", StringUtils.replaceChars(inMessage.getHeader("fileA",String.class), '\\', File.separatorChar));
                    inMessage.setHeader("fileB", StringUtils.replaceChars(inMessage.getHeader("fileB",String.class), '\\', File.separatorChar));
                }
			    
			})
			//.setHeader(ExecBinding.EXEC_COMMAND_ARGS).simple("-i ${properties:inf.wav_dir}2013_12_12_00_31_24_352_1001.wav -i ${properties:inf.wav_dir}2013_12_12_00_40_52_289_1001.wav -filter_complex amerge -c:a libmp3lame -q:a 4 ${properties:export.mp3_dir}${header.actFile}")
			.setHeader(ExecBinding.EXEC_COMMAND_ARGS).simple("-i ${properties:inf.wav_dir}${header.fileA} -i ${properties:inf.wav_dir}${header.fileB} -filter_complex amerge -c:a libmp3lame -q:a 4 ${properties:export.mp3_dir}${header.actFile}")
			.to("exec://{{ffmpeg.file}}?useStderrOnEmptyStdout=true")
			//.to("log:convertLog?showHeaders=true")
			.choice()
			    .when(new Predicate() {
                    
                    @Override
                    public boolean matches(Exchange exchange) {
                        Message inMessage = exchange.getIn();
                        return new File(configProperties.getProperty("export.mp3_dir"),inMessage.getHeader("actFile",String.class)).exists();
                    }
                })
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message inMessage = exchange.getIn();
                        Map<String,Object> body = new HashMap<String,Object>();
                        body.put("actFile",inMessage.getHeader("actFile"));
                        body.put("fromDate",inMessage.getHeader("fromDate"));
                        body.put("toDate",inMessage.getHeader("toDate"));
                        body.put("metaObjToDate",inMessage.getHeader("metaObjToDate"));
                        body.put("metaObjId",inMessage.getHeader("metaObjId"));
                        inMessage.setBody(body);
                    }
                })
                .marshal(csv)
                .aggregate(new AggregationStrategy() {
                    @Override
                    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
                        if (oldExchange == null) {
                            return newExchange;
                        }

                        if (newExchange == null) {
                            return oldExchange;
                        }

                        Message m1 = oldExchange.getIn();
                        Message m2 = newExchange.getIn();

                        String s1 = m1.getBody(String.class);
                        String s2 = m2.getBody(String.class);

                        oldExchange.getIn().setBody(s1 + s2);
                        return oldExchange;
                    }
                }).constant(true).completionSize(simple("{{export.batch.size}}")).completionTimeout(simple("{{export.batch.timeout}}"))
				    //.to("log:convertLog");
				    .to("file:{{export.csv_dir}}");
		
        from("file:{{export.csv_dir}}?autoCreate=true")
            .to("sftp:{{export.sftp.user}}@{{export.sftp.address}}/{{export.sftp.csv_dir}}?privateKeyFile={{export.sftp.private_key_file}}"); 

		from("file:{{export.mp3_dir}}?autoCreate=true")
			.to("sftp:{{export.sftp.user}}@{{export.sftp.address}}/{{export.sftp.mp3_dir}}?privateKeyFile={{export.sftp.private_key_file}}");	
	}

}
