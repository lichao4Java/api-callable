package com.qding.callable.process.security.callable;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.qding.callable.process.print.AbstractProtocolPrint;
import com.qding.framework.common.exception.ServiceException;

public abstract class CallableSecurity {

	public abstract SecurityObject checkCallableSecurity(AbstractProtocolPrint print,
			Method targetMethod, HttpServletRequest request,
			HttpServletResponse response) throws ServiceException;
	
}
