package com.qding.callable.process.pool;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.qding.callable.call.Callable;
import com.qding.callable.exception.CallableException;
import com.qding.callable.process.GlobalInstance;
import com.qding.callable.process.print.AbstractProtocolPrint;
import com.qding.callable.spring.util.ApplicationContextUtil;
import com.qding.framework.common.api.struct.AppDevice;
import com.qding.framework.common.api.struct.request.BaseRequest;

/**
 * callable
 * @author lichao
 *
 */
public class CallablePool {

	private static Map<String, Map<String, Class<? extends Callable>>> callablePool = new HashMap<>();

	private static final Logger logger = Logger.getLogger("callable pool");

	public Map<String, Map<String, Class<? extends Callable>>> get() {
		
		return Collections.unmodifiableMap(callablePool);
		
	}

	public void mount(String alias, String version, Class<? extends Callable> handler) {
		
		synchronized (callablePool) {
			
			if(callablePool.containsKey(alias)) {
			
				Map<String, Class<? extends Callable>> versionPool = callablePool.get(alias);
				
				versionPool.put(version, handler);
			}
			
			else {
				
				Map<String, Class<? extends Callable>> versionPool = new TreeMap<>(
						GlobalInstance.getVersioncomparestrategy()
				);
				
				versionPool.put(version, handler);
				callablePool.put(alias, versionPool);
			}
			
		}
	}
	
	public BaseRequest getBaseRequest(AbstractProtocolPrint print, HttpServletRequest request) {
		
		Object body = GlobalInstance.getTransportSecurity().getBody(request);
		
		if(body != null) {
			return print.in(body.toString(), BaseRequest.class);
		}
		
		return null;
	}

	
	public Callable getCallable(String serviceAlias, BaseRequest request) throws Exception {
		
		AppDevice appDevice = null;
		
		if(request != null) {
			
			appDevice = request.getAppDevice();
					
		}
		
		if(appDevice == null || appDevice.getQdVersion() == null) {
			
			logger.info("appDevice.version not found. instead of lastVersion");
			
			return getLastVersionCallable(serviceAlias);
			
		}
		
		return getCallable(serviceAlias, appDevice.getQdVersion());
	}
	
	public Callable getCallable(String serviceAlias, String version) throws Exception {
		
		if(version == null) {
			
			throw new CallableException(
					"version not found");
		}
		
		Map<String, Class<? extends Callable>> versionPool = callablePool.get(serviceAlias);
		
		if(versionPool == null) {
			throw new CallableException(
					serviceAlias + " not found");
		}
		
		Class<? extends Callable> clazz = versionPool.get(version);
		
		if(clazz != null) {
			return ApplicationContextUtil.getBeansOfType(clazz);
		}
		
		logger.info("alias : " + serviceAlias + ", version : " + version + " not register. instead of close version");
		
		return getCloseVersionCallable(serviceAlias, version);
	}
	
	public Callable getCloseVersionCallable(String alias, String targetVersion) throws CallableException {
		Map<String, Class<? extends Callable>> versionPool = callablePool.get(alias);
		
		if(versionPool == null) {
			throw new CallableException(alias + " not found");
		}
		
		String[] versions = versionPool.keySet().toArray(new String[]{});
		for(int i = 0; i < versions.length; i ++) {
			
			if(GlobalInstance.getVersioncomparestrategy()
					.compare(versions[i], targetVersion) > 0) {
				
				if(i - 1 < 0) {
					//not find
					return getLastVersionCallable(alias);
				}
				
				String closeVersion = versions[i - 1];
				
				logger.info("targetVersion : " + targetVersion + ",closeVersion : " + closeVersion);
				//find
				return ApplicationContextUtil.getBeansOfType(versionPool.get(closeVersion));
			}
		}
		//not find
		return getLastVersionCallable(alias);
	}
	
	public Callable getLastVersionCallable(String alias) throws CallableException {
		Map<String, Class<? extends Callable>> versionPool = callablePool.get(alias);
		
		if(versionPool == null) {
			throw new CallableException(
					alias + " not found");
		}
		
		int size = versionPool.values().size();
		if(size > 0) {
			Class<? extends Callable>[] array = (Class<? extends Callable>[]) versionPool.values().toArray(new Class<?>[]{});
			
			return ApplicationContextUtil.getBeansOfType(array[size - 1]);
		}
		return null;
	}
	
}
