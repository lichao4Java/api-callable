package com.qding.callable.application;

import java.lang.annotation.Annotation;

import javax.servlet.ServletContextEvent;

import org.springframework.web.context.ContextLoaderListener;

import com.qding.callable.call.Callable;
import com.qding.callable.process.GlobalInstance;
import com.qding.callable.process.pool.CallablePool;
import com.qding.callable.process.pool.ExecutorPool;
import com.qding.callable.process.pool.ProtocolPool;
import com.qding.callable.process.print.JsonProtocolPrint;
import com.qding.callable.process.print.XmlProtocolPrint;
import com.smart.validate.ValidateRulePool;
import com.smart.validate.match.AbstractMatchValidate;

/**
 * 
 * @author lichao
 *
 */
public abstract class CallableApplication extends ContextLoaderListener{

	
	private CallablePool callablePool = GlobalInstance.getCallablePool();
	
	private ExecutorPool executorPool = GlobalInstance.getExecutorPool();
	
	private ProtocolPool protocolPool = GlobalInstance.getProtocolPool();
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		
		this.init();
		
		this.registerSupportProtocol();
		
		executorPool.mount();
	}

	protected void mount(String alias, String version, Class<? extends Callable> handler) {
		
		callablePool.mount(alias, version, handler);
		
	}
	
	protected void mount(String alias, Class<? extends Callable> handler) {
		
		callablePool.mount(alias, "1.0.0", handler);
		
	}
	
	protected void mountValidate(Class<? extends Annotation> alias, AbstractMatchValidate<? extends Annotation> handler) {
		
		ValidateRulePool.mount(alias, handler);
	
	}

	protected abstract void init();
	
	protected void registerSupportProtocol() {
		
		protocolPool.register("json", JsonProtocolPrint.class);
		protocolPool.register("xml", XmlProtocolPrint.class);
	
	}

	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		
	}

}
