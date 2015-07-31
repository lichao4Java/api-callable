package com.qding.callable.process.security;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.qding.callable.annotation.HTTP;
import com.qding.callable.process.GlobalInstance;
import com.qding.callable.process.print.AbstractProtocolPrint;
import com.qding.framework.common.constants.HttpStatus;
import com.qding.framework.common.exception.ServiceException;
import com.qding.framework.common.util.QDStringUtil;
import com.qdingnet.auth.service.AuthService;
import com.qdingnet.auth.struct.AuthException;
import com.qdingnet.auth.struct.AuthRequest;
import com.qdingnet.auth.struct.AuthResponse;
import com.qdingnet.auth.struct.TokenRequest;

public class UserTokenCallableSecurity extends CallableSecurity {

	
	public String generatorToken(UserToken tb) {
		
		Map<String, String> map = new HashMap<>();
		
		map.put("data", JSON.toJSONString(tb));
		
		TokenRequest tokenRequest = new TokenRequest(
				tb.getAccountId() == null ? "" : tb.getAccountId(), 
				tb.getName() == null ? "" : tb.getName(), 
				tb.getSourceType() == null ? "" : tb.getSourceType().toString(), 
				String.valueOf(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000), 
				map
		);
		
		try {
			return AuthService.generateToken(tokenRequest);
		} catch (AuthException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public UserToken checkCallableSecurity(AbstractProtocolPrint print,
			Method targetMethod, HttpServletRequest request,
			HttpServletResponse response) throws ServiceException {
		
		String token = (String) GlobalInstance.getTransportSecurity().getParameter(request, "userToken");
		
		if(QDStringUtil.isEmpty(token)) {
			throw new ServiceException(HttpStatus.UNAUTHORIZED);
		}
		
		AuthRequest authRequest = new AuthRequest(token);
		
		AuthResponse authResponse;
		
		try {
			authResponse = AuthService.auth(authRequest);
		} catch (AuthException e) {
			throw new ServiceException(HttpStatus.UNAUTHORIZED);
		}
		
		if(authResponse.getIs_expire()) {
			
			HTTP http = targetMethod.getAnnotation(HTTP.class);

			if(http.isNeedReLoginWhenExpire()) {
				//重新登录
				throw new ServiceException(HttpStatus.NOT_ACCEPTABLE);
			}
		}
		
		Object data = authResponse.getAttribute("data");
		
		UserToken tokenBean = JSON.parseObject(data.toString(), UserToken.class);
		
		return tokenBean;
	}

}
