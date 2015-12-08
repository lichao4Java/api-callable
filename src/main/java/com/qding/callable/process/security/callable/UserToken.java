package com.qding.callable.process.security.callable;

import java.io.Serializable;

public class UserToken extends SecurityObject implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 495004200586315217L;

	private String accountId;
	
	private String memberId;
	
	private String name;
	
	private Integer sourceType;
	
	public UserToken() {

	}


	public UserToken(String accountId, String memberId, String name,
			Integer sourceType) {
		super();
		this.accountId = accountId;
		this.memberId = memberId;
		this.name = name;
		this.sourceType = sourceType;
	}



	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public Integer getSourceType() {
		return sourceType;
	}


	public void setSourceType(Integer sourceType) {
		this.sourceType = sourceType;
	}


	@Override
	public String toString() {
		return "UserToken [accountId=" + accountId + ", memberId=" + memberId
				+ ", name=" + name + ", sourceType=" + sourceType + "]";
	}
}
