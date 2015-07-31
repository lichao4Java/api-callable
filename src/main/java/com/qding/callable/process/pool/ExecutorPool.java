package com.qding.callable.process.pool;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.qding.callable.annotation.HTTP;
import com.qding.callable.call.Callable;
import com.qding.callable.exception.CallableException;
import com.qding.callable.process.GlobalInstance;

/**
 * executor
 * @author lichao
 *
 */
public class ExecutorPool{

	private static Map<Class<? extends Callable>, Map<String, Method>> executorPool = new HashMap<>();
	
	private static final Logger logger = Logger.getLogger("executor pool");
	
	public static Map<Class<? extends Callable>, Map<String, Method>> get() {
		
		return Collections.unmodifiableMap(executorPool);
		
	}

	public void mount() {
		
		Map<String, Map<String, Class<? extends Callable>>> callablePool = GlobalInstance.getCallablePool().get();
		
		Set<String> serviceAliases = callablePool.keySet();
		
		for(String serviceAlias : serviceAliases) {
			
			Map<String, Class<? extends Callable>> serviceVersion = callablePool.get(serviceAlias);
			
			Set<String> serviceVersions = serviceVersion.keySet();
			
			for(String serviceVerison : serviceVersions) {
				

				logger.info("mount callable : " + serviceAlias);
				logger.info("\t version : " + serviceVerison);
				
				versionExecutor.mount(serviceVersion.get(serviceVerison));
			}
		}
	}
	
	public Method getExecutor(Class<? extends Callable> callable, String alias) throws CallableException {
		
		Map<String, Method> ems = executorPool.get(callable);
		
		if(ems == null) {
			
			throw new CallableException(
					callable + "is not register");
		}
		
		Method executorMethod = ems.get(alias);
		
		if(executorMethod == null) {
			throw new CallableException(
					"can not find method " + alias + " in " + callable);
		}
		
		return executorMethod;
	}
	
	private static VersionExecutor versionExecutor = new VersionExecutor();

	private static class VersionExecutor {
		
		private Class<? extends Callable> targetCallable;
		
		public void mount(Class<? extends Callable> callable) {
			
			this.targetCallable = callable;
			
			mountAllVersion(callable);

		}
		
		/**
		 * 
		 * @param callable 当前要挂载的服务
		 */
		private void mountAllVersion(Class<? extends Callable> callable) {
			
			synchronized (executorPool) {
			
				Method[] methods = callable.getDeclaredMethods();
				
				for(Method method : methods) {
	
					if(!Modifier.isPublic(method.getModifiers())) {
						continue;
					}
					
					HTTP http = method.getAnnotation(HTTP.class);
					
					if(http == null) {
						continue;
					}
					
					String alias = http.alias();
					
					if(executorPool.containsKey(targetCallable)) {
						
						Map<String, Method> ems = executorPool.get(targetCallable);
						
						if(ems.containsKey(alias)) {
							
							logger.info("\tOverride executor: "+ http.alias() + " has override from " + callable);
							
							continue;
							
						}
							
						ems.put(alias, method);
							
						logger.info("\t\t executor: "+ http.alias());
						
					}
					else {
						
						Map<String, Method> ems = new HashMap<>();
						
						ems.put(alias, method);
						
						executorPool.put(targetCallable, ems);
						
						logger.info("\t\t executor: "+ http.alias());
					}
					
				}
				
				/**
				 * 如果有上一个版本 就挂载上一个版本的方法
				 */
				if(callable.getSuperclass() != null && Callable.class.isAssignableFrom(callable.getSuperclass())) {
					
					mountAllVersion((Class<? extends Callable>) callable.getSuperclass());
				}
			}
		}
	}
	
}
