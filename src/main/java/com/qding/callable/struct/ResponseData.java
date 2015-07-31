package com.qding.callable.struct;

import java.io.Serializable;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias(value="data")
public class ResponseData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2767300673516866302L;
	
	private String message;
	
	public ResponseData() {

	}
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "ResponseData [message=" + message + "]";
	}
	
}
