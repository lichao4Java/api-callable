package com.qding.callable.application;

import javax.servlet.ServletContextEvent;

import org.springframework.web.context.ContextLoaderListener;

import com.qding.callable.call.Callable;
import com.qding.callable.process.GlobalInstance;
import com.qding.callable.process.print.JsonProtocolPrint;
import com.qding.callable.process.print.XmlProtocolPrint;

/**
 * 
 * @author lichao
 *
 */
public abstract class CallableApplication extends ContextLoaderListener{

	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		
		this.init();
		
		this.registerSupportProtocol();
		
		GlobalInstance.getExecutorPool().mount();
	}

	protected void mount(String alias, String version, Class<? extends Callable> handler) {
		
		GlobalInstance.getCallablePool().mount(alias, version, handler);
		
	}
	
	protected abstract void init();
	
	protected void registerSupportProtocol() {
		
		GlobalInstance.getProtocolPool().register("json", JsonProtocolPrint.class);
		GlobalInstance.getProtocolPool().register("xml", XmlProtocolPrint.class);
	
	}

	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		
	}

}
