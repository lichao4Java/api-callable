package com.qding.callable.spring.controllers;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import com.qding.callable.call.Callable;
import com.qding.callable.exception.CallableException;
import com.qding.callable.ip.IPUtil;
import com.qding.callable.process.GlobalInstance;
import com.qding.callable.process.pool.CallablePool;
import com.qding.callable.process.pool.ExecutorPool;
import com.qding.callable.process.pool.ProtocolPool;
import com.qding.callable.process.print.AbstractProtocolPrint;
import com.qding.callable.process.print.JsonProtocolPrint;
import com.qding.callable.process.security.TransportSecurity;
import com.qding.callable.spring.util.ApplicationContextUtil;
import com.qding.framework.common.api.struct.request.BaseRequest;
import com.qding.framework.common.constants.HttpStatus;
import com.qding.framework.common.exception.ServiceException;
import com.qding.framework.common.log.APILogger;
import com.smart.validate.exception.SmartValidateException;


/**
 * 
 * @author lichao
 *
 */
@Controller
@RequestMapping("/api")
public class APIController extends MultiActionController{

	private CallablePool callablePool = GlobalInstance.getCallablePool();
	
	private ExecutorPool executorPool = GlobalInstance.getExecutorPool();
	
	private ProtocolPool protocolPool = GlobalInstance.getProtocolPool();
	
	@RequestMapping(value="{protocolAlias}/{serviceAlias}/{methodAlias}")
	public void handler(HttpServletRequest request, HttpServletResponse response,
			
			@PathVariable(value="protocolAlias") String protocolAlias, 
			@PathVariable(value="serviceAlias") String serviceAlias, 
			@PathVariable(value="methodAlias") String methodAlias) throws IOException {
		
		long startTime = System.currentTimeMillis();
		int code = 200;
		
		TransportSecurity transportSecurity = GlobalInstance.getTransportSecurity();
        String responseAsString = null;
        AbstractProtocolPrint print = ApplicationContextUtil.getBeansOfType(JsonProtocolPrint.class);
		
        try {
        	
        	APILogger.getCurrRecord().setRequestFileds(new Date(), request.getRequestURI(), methodAlias, null, null, IPUtil.getIpAddress(request));
        	
        	transportSecurity.request(request);
        	
        	print = protocolPool.getProtocolPrint(serviceAlias);
        	
	        BaseRequest baseRequest = callablePool.getBaseRequest(print, request);
	        
	        Callable callable = callablePool.getCallable(serviceAlias, baseRequest);
	        
	        Method executorMethod = executorPool.getExecutor(callable.getClass(), methodAlias);
	        
	        responseAsString = callable.call(print, executorMethod, request, response);
	        
		} 
       
        catch(ServiceException e) {
        	code = e.getReturnInfo().getCode();
        	responseAsString = print.error(code, e.getReturnInfo().getMessage());
        }
        catch(CallableException e) {
        	code = HttpStatus.BAD_REQUEST.getStatusCode();
        	responseAsString = print.error(code, HttpStatus.BAD_REQUEST.toString());
        }
        catch(SmartValidateException e) {
        	code = HttpStatus.BAD_REQUEST.getStatusCode();
        	responseAsString = print.error(code, HttpStatus.BAD_REQUEST.toString());
        }
        catch (Exception e) {
			e.printStackTrace();
			code = HttpStatus.INTERNAL_SERVER_ERROR.getStatusCode();
			responseAsString = print.error(code, HttpStatus.INTERNAL_SERVER_ERROR.toString());
		}

        try {
			responseAsString = transportSecurity.response(request, response, responseAsString);
		} catch (CallableException e) {
			e.printStackTrace();
			code = HttpStatus.BAD_REQUEST.getStatusCode();
        	responseAsString = print.error(code, HttpStatus.BAD_REQUEST.toString());
		}
        
	    response.setCharacterEncoding("utf-8");
        response.setContentType(print.getContentType());
        response.getWriter().print(responseAsString);
        
        long endTime = System.currentTimeMillis();
        
        APILogger.getCurrRecord().setResponseFileds(code, endTime - startTime);
        try {
			APILogger.doLog();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
