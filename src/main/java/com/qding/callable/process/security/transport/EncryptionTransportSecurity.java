package com.qding.callable.process.security.transport;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.qding.callable.exception.CallableException;
import com.qding.framework.common.log.APILogger;
import com.qdingnet.auth.service.SecureTransportService;

/**
 * token传输加密
 * @author lichao
 *
 */
public class EncryptionTransportSecurity extends TransportSecurity{

	
	/**
	 * 判断传输参数是不是经过加密的
	 * @param request
	 * @return
	 */
	@Override
	public boolean isSecueTransport(HttpServletRequest request){
		if( request.getHeader(TOKEN)!=null) return true;
		return false;
	}

	
	/**
	 * 凡是经过安全传输的参数都是从Attribute中获取， 否则就是从Parameter中获取
	 * @param request
	 * @param key
	 * @return
	 */
	@Override
	public Object getParameter(HttpServletRequest request, String key) {
		
		if(isSecueTransport(request)) {
			
			return request.getAttribute(key);
		}
		
		return request.getParameter(key);
		
	}
	
	/**
	 * 解析url中params放入map中
	 * @param queryStr
	 * @return
	 */
	private void parseQueryStr(String queryStr,HttpServletRequest request){
		StringBuffer toString = new StringBuffer();
		String[] queryArray =queryStr.split("&");
		for(int i=0;i<queryArray.length;i++){
			//TODO bug key应该可以是重复的
			String key= queryArray[i].split("=")[0];
			String value= queryArray[i].split("=")[1];
			request.setAttribute(key, value);
			
			toString.append(key).append("=").append(value).append(",");
			
			APILogger.getCurrRecord().putParameters(key, oneLine(new String[]{value}));

		}
		
      	logger.info("request [" + request.getRequestURI() + " param : " + toString.toString() + "]") ;

	}
	
	private String[] oneLine(String[] value) {
		
		if(value == null) return value;

		String[] newValue = new String[value.length];
		for(int i = 0; i < newValue.length; i ++) {
			
			String v = value[i];
			
			if(v == null) {
				
				newValue[i] = null;
			}
			else {
				newValue[i] = v.replaceAll("\r|\n|\t", "");
			}
			
		}
		return newValue;
	}
	
	private static final Logger logger = Logger.getLogger("TransportSecurity");
	
	@Override
	public String response(HttpServletRequest request, 
			HttpServletResponse response, 
			String source) throws CallableException  {
		
      	logger.info("response [" + request.getRequestURI() + " param : " + source + "]") ;

		if(!isSecueTransport(request)) {
			
			return source;
		}	
		
		try {
			String newSecretKey = SecureTransportService.getSecretkey();
			String target = SecureTransportService.encodeContent(source,newSecretKey);
			String newToken= SecureTransportService.encodeSecretKey(newSecretKey);
			response.setHeader(TOKEN, newToken);

	      	logger.debug("response [" + request.getRequestURI() + " secret param : " + target + "]") ;

			return target;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CallableException("生成token失败");
		}
		
	}
	
	
	private static final String TOKEN = "transport-security-token";
	
	@Override
	public void request(HttpServletRequest request) throws CallableException {
		
		if(!isSecueTransport(request)) {
			
			Map<String, String[]> parameterMap = request.getParameterMap();
			Set<String> keySet = parameterMap.keySet();
			StringBuffer toString = new StringBuffer();
			for(String key : keySet) {
				toString.append(key).append("=").append(Arrays.toString(parameterMap.get(key))).append(",");
				APILogger.getCurrRecord().putParameters(key, oneLine(parameterMap.get(key)));
			}
			
	      	logger.info("request [" + request.getRequestURI() + " param : " + toString.toString() + "]") ;

			return;
		}
		
		//对request请求进行解密
		String secretkey = null;
		String token = request.getHeader(TOKEN);
		logger.info("token:" + token);
		secretkey = SecureTransportService.decodeSecretkey(token);
		logger.info("secretkey:"+secretkey);
		try {
			if(request.getMethod().equalsIgnoreCase("GET")) {
				
				String queryString = request.getQueryString();
				
		      	logger.debug("request [" + request.getRequestURI() + " secret param : " + queryString + "]") ;

				String decodeQueryString = SecureTransportService.decodeContent(queryString, secretkey);
				if (decodeQueryString != null) {
					parseQueryStr(decodeQueryString, request);
				}
			}
			//如果是post 请求对body进行解密
			if(request.getMethod().equalsIgnoreCase("POST")){
				String body = request.getParameter(requestKey);
				String userToken = request.getParameter("userToken");
				String decodebody;
				decodebody = SecureTransportService.decodeContent(body,secretkey);
				request.setAttribute(requestKey,decodebody);
				request.setAttribute("userToken",userToken);
				APILogger.getCurrRecord().putParameters(requestKey, oneLine(new String[]{decodebody}));
				APILogger.getCurrRecord().putParameters("userToken", oneLine(new String[]{userToken}));
				
		      	logger.debug("request [" + request.getRequestURI() + " secret param : body=" + body + ",userToken= " + userToken + "]") ;
		      	logger.info("request [" + request.getRequestURI() + " param : body=" + decodebody + ",userToken= " + userToken + "]") ;
			
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new CallableException("解析token失败");
		}
		
	}
}
