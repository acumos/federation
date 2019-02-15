/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
 * ===================================================================================
 * This Acumos software file is distributed by AT&T and Tech Mahindra
 * under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ===============LICENSE_END=========================================================
 */
package org.acumos.federation.gateway.cds;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.acumos.cds.CodeNameType;
import org.acumos.cds.domain.MLPCodeNamePair;
import org.acumos.federation.gateway.service.CodeNamesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The code is written to tolerate early/static references to particular codes by resolving such references only
 * when method invocations occur upon the corresponding CodeName instance.
 * @param <T> Type
 */
public abstract class CodeNames<T extends CodeName> {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static CodeNamesService codes = null;

	protected static Map<String, MLPCodeNamePair> codeNamePairs;
	protected static Map<String, CodeName> codeNames = new HashMap();

	@Autowired
	public void setCodeNamesService(CodeNamesService theService) {
		if (codes != null && codes != theService) {
			log.warn("Mismatched auto (RE-)wiring. Has " + codes + ". Got " + theService);
		}
		if (codes == null)
			codes = theService;
	}	
//	@PostConstruct
//	private void loadCodeNames() {

	protected static void loadCodeNamePairs(CodeNameType theType) {

		List<MLPCodeNamePair> pairs = Collections.EMPTY_LIST;

		if (codes == null)
			throw new IllegalStateException("Service not available");

		try {
			pairs = codes.getCodeNames(theType);
		}
		catch(Exception x) {
			log.warn("Failed to load codes for " + theType, x); 
			//throw new BeanInitializationException("Failed to load codes for " + getType() , x);
			return;
		}

		log.info("Loaded codes for {}: {}", theType, pairs);
		codeNamePairs = new HashMap();
		for (MLPCodeNamePair pair: pairs) {
			codeNamePairs.put(pair.getCode(), pair);
		}
	}

	//public abstract CodeNameType getType();

	public static Iterable<String> codes() {
		return Collections.unmodifiableSet(codeNamePairs.keySet());
	}

	/**
	 * There is weakness in here as it can be called with different code types within the scope of the same container ..
	 * @param <T> Type
	 * @param theCode Code
	 * @param theCodeType Code type
	 * @return Code
	 */	
	protected static <T extends CodeName> T forCode(final String theCode, Class<T> theCodeType) {
		log.info("forCode {}: {}", theCodeType, theCode); 
		synchronized (codeNames) {
			if (!codeNames.containsKey(theCode)) {
				codeNames.put(theCode, (CodeName)Proxy.newProxyInstance(CodeNames.class.getClassLoader(), new Class[] { theCodeType }, new CodeNameHandler(theCode)));
			}
			return (T)codeNames.get(theCode);
		}
	}

	protected static MLPCodeNamePair getCodeNamePair(CodeNameType theType, String theCode) {
		if (codeNamePairs == null) {
			loadCodeNamePairs(theType);
		}

		return (codeNamePairs != null) ? codeNamePairs.get(theCode) : null;
	}


	static class CodeNameHandler implements InvocationHandler {

		private String code;
		private MLPCodeNamePair pair;

		private CodeNameHandler(String theCode) {
			this.code = theCode;
		}

		public Object invoke(Object theProxy, Method theMethod, Object[] theArgs) throws Throwable {
		
			if (this.pair == null) {
				this.pair = getCodeNamePair(invokeGetType(theProxy), this.code);
			}
			if (this.pair == null)
				throw new RuntimeException("A non-existent code has been referenced: " + this.code + ". Known codes: " + codeNamePairs.keySet());

			if (theMethod.getName().equals("getType")) {
				return invokeGetType(theProxy);
			}
			if (theMethod.getName().equals("getCode")) {
				return this.pair.getCode();
			}
			if (theMethod.getName().equals("getName")) {
				return this.pair.getName();
			}
			if (theMethod.getName().equals("equals")) {
				CodeName other = (CodeName)theArgs[0];
				return this.pair.getCode().equals(other.getCode()) &&
							 this.pair.getName().equals(other.getName()); //we should also test the type ..
			}
			if (theMethod.getName().equals("toString")) {
				return this.pair.getCode().toString();
			}
			throw new RuntimeException("Unexpected CodeName call: " + theMethod);
		}

		private CodeNameType invokeGetType(Object theProxy) throws Throwable {
			Class tgt = theProxy.getClass().getInterfaces()[0];
			Method tgtMethod = tgt.getDeclaredMethod("getType");

			if (tgtMethod == null || !tgtMethod.isDefault())
				throw new RuntimeException("Give a default definition to getType in an actual CodeName");

			Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
                .getDeclaredConstructor(Class.class, int.class);
			constructor.setAccessible(true);
 
			CodeNameType type = (CodeNameType)
				constructor.newInstance(tgt, MethodHandles.Lookup.PRIVATE)
                .unreflectSpecial(tgtMethod, tgt)
											 .bindTo(theProxy)
											 .invokeWithArguments();

//			CodeNameType type = (CodeNameType)
//					MethodHandles.lookup()
//											 .in(tgt/*theMethod.getDeclaringClass()*/)
//											 .unreflectSpecial(tgtMethod, tgt/*theMethod.getDeclaringClass()*/)
//											 //.unreflect(getType)
//											 .bindTo(theProxy)
//											 .invokeWithArguments();
			System.out.println("Found type " + type + " for code " + this.code);
			return type;	
		}
	}

}


