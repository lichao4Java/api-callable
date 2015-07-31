package com.qding.callable.process;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.qding.callable.process.print.AbstractProtocolPrint;

/**
 * 
 * @author lichao
 *
 */
public abstract class InvokeCallable {
	
	protected abstract Object[] beforeInvoke(AbstractProtocolPrint print, Method targetMethod, Object securityObject, HttpServletRequest request, HttpServletResponse response) throws Exception;
	
	protected abstract Object invoke(Method targetMethod, Object[] targetMethodArguments, HttpServletRequest request, HttpServletResponse response) throws Exception;
	
	protected abstract Object afterInvoke(AbstractProtocolPrint print, Object value);
}
