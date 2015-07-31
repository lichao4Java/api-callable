package com.qding.callable.process.print;

import com.qding.callable.struct.Response;
import com.qding.callable.struct.ResponseData;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;

/**
 * 
 * @author lichao
 *
 */
public class XmlProtocolPrint extends AbstractProtocolPrint{

	@Override
	public <IN> IN in(String input, Class<IN> clazz) {
		
		XStream stream = new XStream(){
			
			protected MapperWrapper wrapMapper(MapperWrapper next) {
		        return new MapperWrapper(next) {
		            @Override
		            @SuppressWarnings("rawtypes")
		            public boolean shouldSerializeMember(Class definedIn, String fieldName) {
		            	
		            	if(definedIn == Object.class) {
		            		return false;
		            	}
		               
		               return super.shouldSerializeMember(definedIn, fieldName);
		            }
		        };
		    }
		};
		stream.alias("root", clazz);
		stream.autodetectAnnotations(true);
		return (IN) stream.fromXML(input);
	}

	@Override
	public <OUT> String out(OUT out) {
		XStream stream = new XStream();
		stream.alias("root", out.getClass());
		stream.autodetectAnnotations(true);
		stream.setMode(XStream.NO_REFERENCES);
		return stream.toXML(out);
	}

	@Override
	public String error(int code, String message) {
		Response<ResponseData> response = new Response<ResponseData>();
		ResponseData data = new ResponseData();
		data.setMessage(message);
		response.setCode(code);
		response.setData(data);
		return out(response);
	}

	@Override
	public String getContentType() {
		return "text/xml;charset=utf-8";
	}

}
