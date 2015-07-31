package com.qding.callable.process.pool;

import java.util.HashMap;
import java.util.Map;

import com.qding.callable.exception.CallableException;
import com.qding.callable.process.print.AbstractProtocolPrint;
import com.qding.callable.spring.util.ApplicationContextUtil;
import com.qding.framework.common.constants.HttpStatus;

/**
 * 
 * @author lichao
 *
 */
public class ProtocolPool extends AliasPool<AbstractProtocolPrint>{

	private static Map<String, Class<? extends AbstractProtocolPrint>> protocolPool = new HashMap<>();

	public ProtocolPool() {
		super(protocolPool);
	}
	
	public AbstractProtocolPrint getProtocolPrint(String alias) throws CallableException {
		
		Class<? extends AbstractProtocolPrint> printClass = getExecutor(alias);
    	
    	if(printClass == null) {
			
    		throw new CallableException("only support [json,xml] ");
    	}
    	else {
    		return ApplicationContextUtil.getBeansOfType(printClass);
    	}
    	
	}
	
}
