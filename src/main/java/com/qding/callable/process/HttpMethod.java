package com.qding.callable.process;

public enum HttpMethod {

	POST("post"),
	GET("get");
	
	private String method;
	
	HttpMethod(String method) {
		this.method = method;
	}
	
	public boolean is(String method) {
		return this.method.equalsIgnoreCase(method);
	}
}
