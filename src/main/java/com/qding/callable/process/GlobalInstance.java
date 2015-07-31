package com.qding.callable.process;

import com.qding.callable.process.pool.CallablePool;
import com.qding.callable.process.pool.ExecutorPool;
import com.qding.callable.process.pool.ProtocolPool;
import com.qding.callable.process.security.TransportSecurity;
import com.qding.callable.process.security.UserTokenCallableSecurity;

public class GlobalInstance {

	private static final CallablePool callablePool = new CallablePool();
	
	private static final ExecutorPool executorPool = new ExecutorPool();
	
	private static final ProtocolPool protocolPool = new ProtocolPool();
	
	private static final UserTokenCallableSecurity userTokenCallableSecurity = new UserTokenCallableSecurity();
	
	private static final TransportSecurity transportSecurity = new TransportSecurity();
	
	public static TransportSecurity getTransportSecurity() {
		
		return transportSecurity;
	}
	
	public static UserTokenCallableSecurity getUserTokenCallableSecurity() {
		
		return userTokenCallableSecurity;
	}
	
	public static CallablePool getCallablePool() {
		
		return callablePool;
	}
	
	public static ExecutorPool getExecutorPool() {
		
		return executorPool;
	}
	
	public static ProtocolPool getProtocolPool() {
		
		return protocolPool;
	}
}
