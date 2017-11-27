/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.exceptions;

import java.util.Collection;
import java.util.LinkedList;

import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.logging.ErrorObject;
import org.onap.aai.logging.ErrorObjectNotFoundException;

public class AAIException extends Exception {

	public static final String DEFAULT_EXCEPTION_CODE = "AAI_4000";
	private static final long serialVersionUID = 1L;

	private final String code;
	private final ErrorObject errorObject;
	private final Collection<String> templateVars;

	/**
	 * Instantiates a new AAI exception.
	 */
	public AAIException() {
		super();
		this.code = DEFAULT_EXCEPTION_CODE;
		this.templateVars = new LinkedList<String> ();

		try {
			this.errorObject = ErrorLogHelper.getErrorObject(getCode());
		} catch (ErrorObjectNotFoundException e) {
			throw new RuntimeException("Failed to instantiate AAIException with code=" + getCode()
										 + " - update error.properties before using this exception code");
		}
	}

	/**
	 * Instantiates a new AAI exception.
	 *
	 * @param code the code
	 */
	public AAIException(String code) {
		super();

		this.code = code;
		this.templateVars = new LinkedList<String> ();

		try {
			this.errorObject = ErrorLogHelper.getErrorObject(getCode());
		} catch (ErrorObjectNotFoundException e) {
			throw new RuntimeException("Failed to instantiate AAIException with code=" + getCode()
										 + " - update error.properties before using this exception code");
		}
	}
	
	/**
	 * Instantiates a new AAI exception.
	 *
	 * @param code the code
	 * @param details the details
	 */
	public AAIException(String code, String details) {
		super(details);

		this.code = code;
		this.templateVars = new LinkedList<String> ();

		try {
			this.errorObject = ErrorLogHelper.getErrorObject(getCode());
		} catch (ErrorObjectNotFoundException e) {
			throw new RuntimeException("Failed to instantiate AAIException with code=" + getCode()
										 + " - update error.properties before using this exception code");
		}
	}

	/**
	 * Instantiates a new AAI exception.
	 *
	 * @param code the code
	 * @param cause the cause
	 */
	public AAIException(String code, Throwable cause) {
		super(cause);

		this.code = code;
		this.templateVars = new LinkedList<String> ();

		try {
			this.errorObject = ErrorLogHelper.getErrorObject(getCode());
		} catch (ErrorObjectNotFoundException e) {
			throw new RuntimeException("Failed to instantiate AAIException with code=" + getCode()
										 + " - update error.properties before using this exception code");
		}
	}
	
	/**
	 * Instantiates a new AAI exception.
	 *
	 * @param code the code
	 * @param cause the cause
	 * @param details the details
	 */
	public AAIException(String code, Throwable cause, String details) {
		super(details, cause);

		this.code = code;
		this.templateVars = new LinkedList<String> ();

		try {
			this.errorObject = ErrorLogHelper.getErrorObject(getCode());
		} catch (ErrorObjectNotFoundException e) {
			throw new RuntimeException("Failed to instantiate AAIException with code=" + getCode()
										 + " - update error.properties before using this exception code");
		}
	}
	
	public String getCode() {
		return code;
	}

	public ErrorObject getErrorObject() {
		return errorObject;
	}

	public Collection<String> getTemplateVars() {
		return templateVars;
	}
}