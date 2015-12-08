package com.qding.callable.process;

import com.qding.callable.process.pool.CallablePool;
import com.qding.callable.process.pool.ExecutorPool;
import com.qding.callable.process.pool.ProtocolPool;
import com.qding.callable.process.security.callable.CallableSecurity;
import com.qding.callable.process.security.callable.UserTokenCallableSecurity;
import com.qding.callable.process.security.transport.EncryptionTransportSecurity;
import com.qding.callable.process.security.transport.TransportSecurity;
import com.qding.callable.process.version.strategy.NumberVersionCompareStrategy;
import com.qding.callable.process.version.strategy.VersionCompareStrategy;

public class GlobalInstance {

	private static final CallablePool callablePool = new CallablePool();
	
	private static final ExecutorPool executorPool = new ExecutorPool();
	
	private static final ProtocolPool protocolPool = new ProtocolPool();
	
	private static CallableSecurity callableSecurity = new UserTokenCallableSecurity();
	
	private static TransportSecurity transportSecurity = new EncryptionTransportSecurity();
	
	private static VersionCompareStrategy versionCompareStrategy = new NumberVersionCompareStrategy();
	
	public static class GlobalSetting {
		
		public static void setVersionCompareStrategy(VersionCompareStrategy versionCompareStrategy) {
			GlobalInstance.versionCompareStrategy = versionCompareStrategy;
		}
		
		public static void setCallableSecurity(CallableSecurity callableSecurity) {
			GlobalInstance.callableSecurity = callableSecurity;
		}
		
		public static void setTransportSecurity(TransportSecurity transportSecurity) {
			GlobalInstance.transportSecurity = transportSecurity;
		}
	}
	
	public static TransportSecurity getTransportSecurity() {
		
		return transportSecurity;
	}
	
	public static CallableSecurity getCallableSecurity() {
		
		return callableSecurity;
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
	
	public static VersionCompareStrategy getVersioncomparestrategy() {
		return versionCompareStrategy;
	}
	
}
