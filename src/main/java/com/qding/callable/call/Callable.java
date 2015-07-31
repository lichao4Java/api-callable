package com.qding.callable.call;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

import com.qding.callable.annotation.HTTP;
import com.qding.callable.exception.CallableException;
import com.qding.callable.process.GlobalInstance;
import com.qding.callable.process.HttpMethod;
import com.qding.callable.process.InvokeCallable;
import com.qding.callable.process.print.AbstractProtocolPrint;
import com.qding.callable.process.security.CallableSecurity;
import com.qding.callable.process.security.SecurityObject;
import com.qding.callable.struct.Response;
import com.qding.callable.struct.ResponseData;
import com.qding.framework.common.api.struct.ReturnInfo;
import com.qding.framework.common.api.struct.request.BaseRequest;
import com.qding.framework.common.api.struct.response.BaseResponse;
import com.qding.framework.common.constants.HttpStatus;
import com.qding.framework.common.exception.ServiceException;
import com.smart.validate.SmartValidate;


/**
 * 
 * @author lichao
 *
 */
public class Callable extends InvokeCallable{
	
	@Autowired
	private Mapper mapper;

	/**
	 * 接口认证
	 * @return
	 * @throws CallableException
	 */
	protected CallableSecurity getCallableSecurity() throws CallableException {
		
		return GlobalInstance.getUserTokenCallableSecurity();
		
	}
	
	public String call(AbstractProtocolPrint print,
			Method targetMethod, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		if(isNotSupportHttpMethod(request.getMethod(), targetMethod)) {
	        	
			throw new CallableException("http method " + request.getMethod() + " not support ");
		}
		 
		HTTP http = targetMethod.getAnnotation(HTTP.class);

		SecurityObject securityObject = null;

		if(http.isRequireAuth()) {
			
			securityObject = getCallableSecurity().checkCallableSecurity(print, targetMethod, request, response);;
		}
		
		Object[] targetMethodArguments = beforeInvoke(print, targetMethod, securityObject, request, response);
		
		Object returnValue = invoke(targetMethod, targetMethodArguments, request, response);

		String returnString = afterInvoke(print, returnValue).toString();

		return returnString;
	}

	
	public boolean isNotSupportHttpMethod(String httpMethod, Method method) {
		HTTP http = method.getAnnotation(HTTP.class);
		
		HttpMethod[] supportMethod = http.supportMethod();
		for(HttpMethod m : supportMethod) {
			if(m.is(httpMethod)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected Object[] beforeInvoke(AbstractProtocolPrint print,
			Method targetMethod, Object securityObject, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		Class<?>[] argumentClasses = targetMethod.getParameterTypes();
		
		Object[] arguments = new Object[argumentClasses.length];
		
		for(int i = 0; i < argumentClasses.length; i ++) {
			
			Class<?> argumentClass = argumentClasses[i];
			
			if(BaseRequest.class.isAssignableFrom(argumentClass)) {
				
				String value = (String) GlobalInstance.getTransportSecurity().getBody(request);
				
				if(value == null || value.trim().length() == 0) {
					throw new CallableException("missing arguments body");
				}
				
				value = URLDecoder.decode(value, "utf-8");
				
				Object in = print.in(value, argumentClass);
				
				SmartValidate.validate(in);
				
				arguments[i] = in;
			}
			else if(HttpServletRequest.class.isAssignableFrom(argumentClass)) {
				arguments[i] = request;
			}
			else if(HttpServletResponse.class.isAssignableFrom(argumentClass)) {
				arguments[i] = response;
			}
			else if(SecurityObject.class.isAssignableFrom(argumentClass)) {
				arguments[i] = securityObject;
			}
		}
		
		return arguments;
	}

	@Override
	protected Object invoke(Method targetMethod, Object[] arguments,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		Object returnValue = targetMethod.invoke(this, arguments);
		
		return returnValue;
	}

	@Override
	protected Object afterInvoke(AbstractProtocolPrint print, Object value) {
		if(value == null) {
			return "";
		}
		
		if(NOSERIALIZABLE.contains(value.getClass())) {
			
			return value;
		}
		
		return print.out(value);
	}
	
	private static final Set<Class<?>> NOSERIALIZABLE = new HashSet<>();
	
	static {
		
		NOSERIALIZABLE.add(int.class);
		NOSERIALIZABLE.add(long.class);
		NOSERIALIZABLE.add(float.class);
		NOSERIALIZABLE.add(double.class);
		NOSERIALIZABLE.add(char.class);
		NOSERIALIZABLE.add(boolean.class);
		NOSERIALIZABLE.add(byte.class);
		NOSERIALIZABLE.add(short.class);
		NOSERIALIZABLE.add(Short.class);
		NOSERIALIZABLE.add(Byte.class);
		NOSERIALIZABLE.add(Boolean.class);
		NOSERIALIZABLE.add(Character.class);
		NOSERIALIZABLE.add(Double.class);
		NOSERIALIZABLE.add(Float.class);
		NOSERIALIZABLE.add(Long.class);
		NOSERIALIZABLE.add(Integer.class);
		NOSERIALIZABLE.add(String.class);
		
	}
    private static final Logger logger = Logger.getLogger("callable");

	protected void checkAndContinue (BaseResponse response) throws ServiceException {
		
		ReturnInfo returnInfo = response.getReturnInfo();
		
		int code = returnInfo.getCode();
		
		if(HttpStatus.OK.getStatusCode() != code) {
			
			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			
			if(stackTrace.length > 1) {
				StringBuffer log = new StringBuffer();
				log.append(" RPC Exception : class --->")
				.append(response.getClass())
				.append(" response --->")
				.append(ToStringBuilder.reflectionToString(response))
				.append(" methodName --->")
				.append(stackTrace[1].getMethodName())
				.append(" line --->")
				.append(stackTrace[1].getLineNumber());
				
				logger.info(log.toString());
			}
			
			
			throw new ServiceException(code, returnInfo.getMessage());
		}
		
	}
	
	private ReturnInfo toReturnInfo(Exception e) {
		ReturnInfo returnInfo = null;
		if(e instanceof ServiceException) {
			ServiceException se = (ServiceException) e;
			returnInfo = se.getReturnInfo();
		}
		else {
			e.printStackTrace();
			returnInfo = new ReturnInfo(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return returnInfo;
	}

	public Response<ResponseData> handleException(Exception e) {
		
		return handleException(ResponseData.class, e);
		
	}
	
	public <T extends ResponseData> Response<T> handleException(Class<T> clazz, Exception e) {
		T data = null;
		try {
			data = clazz.newInstance();
		} catch (InstantiationException e1) {
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
		}
		return handleException(data, e);
	}
	
	public <T extends ResponseData> Response<T> handleException(T data, Exception e) {
		ReturnInfo returnInfo = toReturnInfo(e);
		Response<T> response = new Response<T>();
		data.setMessage(returnInfo.getMessage());
		response.setCode(returnInfo.getCode());
		response.setData(data);
		return response;
	}


    public <T> List<T> transforList(Class<T> clazz, List<?> sources) {
    	List<T> list = new ArrayList<>();
    	if(sources == null) {
    		return list;
    	}
    	for(Object o : sources) {
    		T t = transfor(clazz, o);
    		list.add(t);
    	}
    	return list;
    }
    
    public void transfor(Object target, Object source) {
    	if(source == null || target == null) {
    		return;
    	}
		mapper.map(source, target);
	}  
    
    public <T> T transfor(Class<T> target, Object source) {
    	if(source == null) {
    		return null;
    	}
		return mapper.map(source, target);
	}
}
