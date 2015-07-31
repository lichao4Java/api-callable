package com.qding.callable.spring.controllers;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qding.callable.annotation.DependencyHttp;
import com.qding.callable.annotation.DependencyRpc;
import com.qding.callable.annotation.HTTP;
import com.qding.callable.call.Callable;
import com.qding.callable.process.GlobalInstance;
import com.qding.callable.struct.Response;
import com.qding.framework.common.api.struct.request.BaseRequest;


/**
 * 
 * @author lichao
 *
 */
@Controller
@RequestMapping("/tools")
public class APIToolsController {

	private static Map<String, Map<String, Class<? extends Callable>>> servicePool;
	
	private void buildDoc(JSONArray doc, String id, String parent, String text, Object[] keys, Object[] values) {
		
		JSONObject child = tree( id, parent, text, keys, values);
		
		doc.add(child);
	}
	
	private void buildParam(JSONArray tree, String id, String parent, String text, Object[] keys, Object[] values) {
		
		JSONObject child = tree( id, parent, text, keys, values);
		
		tree.add(child);
	}

	private JSONObject tree(String id, String parent, String text, Object[] keys, Object[] values) {
		JSONObject child = new JSONObject();
		child.put("id", id);
		child.put("parent", parent);
		child.put("text", text);
		JSONArray doc = new JSONArray();
		for(int i = 0; i < keys.length; i ++) {
			JSONObject kv = new JSONObject();
			kv.put("key", keys[i].toString());
			kv.put("value", values[i].toString());
			doc.add(kv);
		}
		child.put("data", doc);
		return child;
	}
	
	private static JSONArray root;

	private JSONArray init() {
		
		synchronized (APIToolsController.class) {
			
			if(root == null) {
				root = new JSONArray();
			}
			else {
				return root;
			}
			
		}
		
		servicePool = GlobalInstance.getCallablePool().get();
		
		buildServices();
		
		return root;
	}

	public void buildServices() {
		
		Set<String> aliass = servicePool.keySet();
		
		for(String alias : aliass) {
			
			buildDoc(root, alias, 
					"#", 
					alias, 
					new String[0], 
					new Object[0]
			);
			
			
			buildVersions(alias);
			
		}
		
	}
	
	private void buildVersions(String alias) {
		Map<String, Class<? extends Callable>> serviceVersionPool = servicePool.get(alias);
		Set<String> versions = serviceVersionPool.keySet();
		for(String version : versions) {
			buildDoc(root, alias + ":" + version, 
					alias, 
					version, 
					new String[0], 
					new Object[0]
			);
			
			buildMethods(alias, version);
		}
	}

	private void buildMethods(String alias,
			String version) {
		
		Map<String, Class<? extends Callable>> serviceVersionPool = servicePool.get(alias);
		Class<? extends Callable> clazz = serviceVersionPool.get(version);
		
		Method[] methods = clazz.getDeclaredMethods();
		
		for(Method method : methods) {
			
			HTTP http = method.getAnnotation(HTTP.class);
			if(http == null) { continue; }
			
			String id = alias + ":" + version + ":" + http.alias();

			buildDoc(root, id, 
					alias + ":" + version, 
					http.alias(),
					new String[0], new String[0]);
			
		}
	}

	private JSONObject buildMethod(String serviceAlias, String methodAlias, String version) {
		Map<String, Class<? extends Callable>> serviceVersionPool = servicePool.get(serviceAlias);
		Class<? extends Callable> clazz = serviceVersionPool.get(version);
		Method[] methods = clazz.getDeclaredMethods();
		
		for(Method method : methods) {
			HTTP http = method.getAnnotation(HTTP.class);
			if(http == null || !http.alias().equals(methodAlias)) { continue; }

			String id = serviceAlias + ":" + version + ":" + http.alias();
			
			try {
				
				JSONArray responseTree = buildResponse(method);
				
				JSONArray requestTree = buildRequest(method);
				
				String[] dependencyRpces = bulidDependencyRpc(method);
				
				String[] bulidDependencyHttp = bulidDependencyHttp(method);
				
				return tree( id, 
						"#", 
						http.alias(),
						new String[]{"服务", "版本号", "调用方法", "支持HTTP方法", "是否需要接口认证", "RPC依赖", "HTTP依赖", "TREE:REQUEST", "TREE:RESPONSE"}, 
						new Object[]{serviceAlias, version, http.alias(), Arrays.toString(http.supportMethod()), http.isRequireAuth(), Arrays.toString(dependencyRpces), Arrays.toString(bulidDependencyHttp), requestTree.toJSONString(), responseTree.toJSONString()}
						);
				
				
			} catch (Exception e) {
				e.printStackTrace();
			} 
		
		}
		return new JSONObject();
	}

	private String[] bulidDependencyRpc(Method method) {
		String[] dependencyRpces = new String[0];
		DependencyRpc dependencyRpc = method.getAnnotation(DependencyRpc.class);
		if(dependencyRpc != null) {
			Class<?>[] cs = dependencyRpc.clazz();
			String[] ms = dependencyRpc.method();
			if(cs != null && ms != null && cs.length == ms.length) {
				dependencyRpces = new String[cs.length];
				for(int i = 0; i < cs.length; i ++) {
					Class<?> c = cs[i];
					String m = ms[i];
					String file = c.getProtectionDomain().getCodeSource().getLocation().getFile();
					dependencyRpces[i] = file.substring(file.lastIndexOf("/") + 1) + "-->" + c.getName() + "-->" + m;
				}
			}
		}
		return dependencyRpces;
	}
	
	private String[] bulidDependencyHttp(Method method) {
		String[] dependencyHttpes = new String[0];
		DependencyHttp dependencyHttp = method.getAnnotation(DependencyHttp.class);
		if(dependencyHttp != null) {
			return dependencyHttp.url();
		}
		return dependencyHttpes;
	}

	private JSONArray buildRequest(Method method) {
		JSONArray requestTree = new JSONArray();
		
		Class<?>[] parameterTypes = method.getParameterTypes();
		if(parameterTypes.length > 0) {
			Class<?>[] classes = method.getParameterTypes();
			for(Class<?> request : classes) {
				if(BaseRequest.class.isAssignableFrom(request)) {
					buildParam(requestTree, "request", "#", "request", new String[]{}, new String[]{});
					buildJavaBean(requestTree, "request", request);
				}
			}
		}
		return requestTree;
	}

	private JSONArray buildResponse(Method method) {
		
		JSONArray responseTree = new JSONArray();
		buildParam(responseTree, "response", "#", "response", new String[]{}, new String[]{});
		
		if(Response.class.isAssignableFrom(method.getReturnType())) {
			
			Type pt = method.getGenericReturnType();
			
			buildParam(responseTree, "data", "response", "data", new String[]{}, new String[]{});
			buildParam(responseTree, "code", "response", "code", new String[]{}, new String[]{});
			
			ParameterizedType type = (ParameterizedType) pt;
			buildJavaBean(responseTree, "data", (Class<?>)type.getActualTypeArguments()[0]);
		}
		
		return responseTree;
	}
	
	private void buildJavaBean(JSONArray tree, String parent, Class<?> clazz) {
    	
		if(!isJavaBean(clazz)) {
			return;
		}
		
		Object target = null;
		try {
			target = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e1) {
			e1.printStackTrace();
		}
		
        for (Field field : clazz.getDeclaredFields()) {
        	
        	if(Modifier.isStatic(field.getModifiers())
        			|| !Modifier.isPrivate(field.getModifiers())) {
        		continue;
        	}
        	
        	String id = parent + ":" + field.getName();
        	
        	try {
        		
				Class<?> type = field.getType();

				PropertyDescriptor pd = new PropertyDescriptor(field.getName(), clazz);
				
				Object value = pd.getReadMethod().invoke(target);
				
				buildParam(tree, id, 
						parent, 
						field.getName() + ":" + field.getType().getSimpleName() + ":" + Arrays.toString(field.getAnnotations()),
						new String[]{field.getName()}, 
						new Object[]{value == null ? "" : value}
						);
				
				if(Collection.class.isAssignableFrom(type)
						|| Map.class.isAssignableFrom(type)
						|| type.isArray()) {
					Type genericType = field.getGenericType();
					if(genericType instanceof ParameterizedType) {
						ParameterizedType pt = (ParameterizedType) field.getGenericType();
						Class<?> actualType = (Class<?>) pt.getActualTypeArguments()[0];
						buildJavaBean(tree, id, actualType);
					}
				}
				else {
					buildJavaBean(tree, id, field.getType());
				}
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | IntrospectionException e) {
//				e.printStackTrace();
			} 
        }
        
        if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
        	buildJavaBean(tree, parent,clazz.getSuperclass());
        }
    }
	
	private boolean isJavaBean(Class<?> clazz) {
		
		return clazz.getName().startsWith("com.qding");
		
	}
	
	@RequestMapping("console")
	public String console(HttpServletRequest request) {
		
		JSONArray root = init();
		
		request.setAttribute("data", root.toJSONString());
		
		return "/api-tools/console/console.jsp";
	}
	
	@RequestMapping("method")
	public void method(HttpServletResponse response ,
			@RequestParam("serviceAlias") String serviceAlias, 
			@RequestParam("methodAlias") String methodAlias, 
			@RequestParam("version")  String version) throws IOException {
		
		JSONObject doc = buildMethod(serviceAlias, methodAlias, version);
		
		response.setContentType("text/json;charset=utf-8");
		response.getWriter().write(doc.toJSONString());
	}
}
