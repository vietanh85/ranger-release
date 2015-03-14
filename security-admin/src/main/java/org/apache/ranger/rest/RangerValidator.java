/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.rest;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.apache.ranger.plugin.store.ServiceStore;
import org.apache.ranger.plugin.util.SearchFilter;

public abstract class RangerValidator {
	
	private static final Log LOG = LogFactory.getLog(RangerValidator.class);

	ServiceStore _store;

	public enum Action {
		CREATE, UPDATE, DELETE;
	};
	
	protected RangerValidator(ServiceStore store) {
		if (store == null) {
			throw new IllegalArgumentException("ServiceValidator(): store is null!");
		}
		_store = store;
	}

	public void validate(Long id, Action action) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerValidator.validate(" + id + ")");
		}

		List<ValidationFailureDetails> failures = new ArrayList<ValidationFailureDetails>();
		if (isValid(id, action, failures)) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("<== RangerValidator.validate(" + id + "): valid");
			}
		} else {
			String message = serializeFailures(failures);
			LOG.debug("<== RangerValidator.validate(" + id + "): invalid, reason[" + message + "]");
			throw new Exception(message);
		}
	}
	
	/**
	 * This method is expected to be overridden by sub-classes.  Default implementation provided to not burden implementers from having to implement methods that they know would never be called. 
	 * @param id
	 * @param action
	 * @param failures
	 * @return
	 */
	boolean isValid(Long id, Action action, List<ValidationFailureDetails> failures) {
		failures.add(new ValidationFailureDetailsBuilder()
				.isAnInternalError()
				.becauseOf("unimplemented method called")
				.build());
		return false;
	}

	String serializeFailures(List<ValidationFailureDetails> failures) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerValidator.getFailureMessage()");
		}

		String message = null;
		if (CollectionUtils.isEmpty(failures)) {
			LOG.warn("serializeFailures: called while list of failures is null/empty!");
		} else {
			StringBuilder builder = new StringBuilder();
			for (ValidationFailureDetails aFailure : failures) {
				builder.append(aFailure.toString());
				builder.append(";");
			}
			message = builder.toString();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerValidator.serializeFailures(): " + message);
		}
		return message;
	}

	Set<String> getServiceConfigParameters(RangerService service) {
		if (service == null || service.getConfigs() == null) {
			return new HashSet<String>();
		} else {
			return service.getConfigs().keySet();
		}
	}

	Set<String> getRequiredParameters(RangerServiceDef serviceDef) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerValidator.getRequiredParameters(" + serviceDef + ")");
		}

		Set<String> result;
		if (serviceDef == null) {
			result = Collections.emptySet();
		} else {
			List<RangerServiceConfigDef> configs = serviceDef.getConfigs();
			if (CollectionUtils.isEmpty(configs)) {
				result = Collections.emptySet();
			} else {
				result = new HashSet<String>(configs.size()); // at worse all of the config items are required!
				for (RangerServiceConfigDef configDef : configs) {
					if (configDef.getMandatory()) {
						result.add(configDef.getName());
					}
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerValidator.getRequiredParameters(" + serviceDef + "): " + result);
		}
		return result;
	}

	RangerServiceDef getServiceDef(Long id) {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerValidator.getServiceDef(" + id + ")");
		}
		RangerServiceDef result = null;
		try {
			result = _store.getServiceDef(id);
		} catch (Exception e) {
			LOG.debug("Encountred exception while retrieving service def from service store!", e);
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerValidator.getServiceDef(" + id + "): " + result);
		}
		return result;
	}

	RangerServiceDef getServiceDef(String type) {
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerValidator.getServiceDef(" + type + ")");
		}
		RangerServiceDef result = null;
		try {
			result = _store.getServiceDefByName(type);
		} catch (Exception e) {
			LOG.debug("Encountred exception while retrieving service definition from service store!", e);
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerValidator.getServiceDef(" + type + "): " + result);
		}
		return result;
	}

	RangerService getService(Long id) {
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerValidator.getService(" + id + ")");
		}
		RangerService result = null;
		try {
			result = _store.getService(id);
		} catch (Exception e) {
			LOG.debug("Encountred exception while retrieving service from service store!", e);
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerValidator.getService(" + id + "): " + result);
		}
		return result;
	}

	RangerService getService(String name) {
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerValidator.getService(" + name + ")");
		}
		RangerService result = null;
		try {
			result = _store.getServiceByName(name);
		} catch (Exception e) {
			LOG.debug("Encountred exception while retrieving service from service store!", e);
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerValidator.getService(" + name + "): " + result);
		}
		return result;
	}

	RangerPolicy getPolicy(Long id) {
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerValidator.getPolicy(" + id + ")");
		}
		RangerPolicy result = null;
		try {
			result = _store.getPolicy(id);
		} catch (Exception e) {
			LOG.debug("Encountred exception while retrieving policy from service store!", e);
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerValidator.getPolicy(" + id + "): " + result);
		}
		return result;
	}

	List<RangerPolicy> getPolicies(final String policyName, final String serviceName) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerValidator.getPolicies(" + policyName + ", " + serviceName + ")");
		}

		List<RangerPolicy> policies = null;
		try {
			SearchFilter filter = new SearchFilter();
			filter.setParam(SearchFilter.POLICY_NAME, policyName);
			filter.setParam(SearchFilter.SERVICE_NAME, serviceName);
			
			policies = _store.getPolicies(filter);
		} catch (Exception e) {
			LOG.debug("Encountred exception while retrieving service from service store!", e);
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerValidator.getPolicies(" + policyName + ", " + serviceName + "): " + policies);
		}
		return policies;
	}

	Set<String> getAccessTypes(RangerServiceDef serviceDef) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerValidator.getAccessTypes(" + serviceDef + ")");
		}

		Set<String> accessTypes = new HashSet<String>();
		if (serviceDef == null) {
			LOG.warn("serviceDef passed in was null!");
		} else if (CollectionUtils.isEmpty(serviceDef.getAccessTypes())) {
			LOG.warn("AccessTypeDef collection on serviceDef was null!");
		} else {
			for (RangerAccessTypeDef accessTypeDef : serviceDef.getAccessTypes()) {
				if (accessTypeDef == null) {
					LOG.warn("Access type def was null!");
				} else {
					String accessType = accessTypeDef.getName();
					if (StringUtils.isBlank(accessType)) {
						LOG.warn("Access type def name was null/empty/blank!");
					} else {
						accessTypes.add(accessType.toLowerCase());
					}
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerValidator.getAccessTypes(" + serviceDef + "): " + accessTypes);
		}
		return accessTypes;
	}
	
	/**
	 * This function exists to encapsulates the current behavior of code which treats and unspecified audit preference to mean audit is enabled.
	 * @param policy
	 * @return
	 */
	boolean getIsAuditEnabled(RangerPolicy policy) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerValidator.getIsAuditEnabled(" + policy + ")");
		}

		boolean isEnabled = false;
		if (policy == null) {
			LOG.warn("policy was null!");
		} else if (policy.getIsAuditEnabled() == null) {
			isEnabled = true;
		} else {
			isEnabled = policy.getIsAuditEnabled();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerValidator.getIsAuditEnabled(" + policy + "): " + isEnabled);
		}
		return isEnabled;
	}
	
	/**
	 * Returns names of resource types set to lower-case to allow for case-insensitive comparison. 
	 * @param serviceDef
	 * @return
	 */
	Set<String> getMandatoryResourceNames(RangerServiceDef serviceDef) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerValidator.getMandatoryResourceNames(" + serviceDef + ")");
		}

		Set<String> resourceNames = new HashSet<String>();
		if (serviceDef == null) {
			LOG.warn("serviceDef passed in was null!");
		} else if (CollectionUtils.isEmpty(serviceDef.getResources())) {
			LOG.warn("ResourceDef collection on serviceDef was null!");
		} else {
			for (RangerResourceDef resourceTypeDef : serviceDef.getResources()) {
				if (resourceTypeDef == null) {
					LOG.warn("resource type def was null!");
				} else {
					Boolean mandatory = resourceTypeDef.getMandatory();
					if (mandatory != null && mandatory == true) {
						String resourceName = resourceTypeDef.getName();
						if (StringUtils.isBlank(resourceName)) {
							LOG.warn("Resource def name was null/empty/blank!");
						} else {
							resourceNames.add(resourceName.toLowerCase());
						}
					}
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerValidator.getMandatoryResourceNames(" + serviceDef + "): " + resourceNames);
		}
		return resourceNames;
	}

	Set<String> getAllResourceNames(RangerServiceDef serviceDef) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerValidator.getAllResourceNames(" + serviceDef + ")");
		}

		Set<String> resourceNames = new HashSet<String>();
		if (serviceDef == null) {
			LOG.warn("serviceDef passed in was null!");
		} else if (CollectionUtils.isEmpty(serviceDef.getResources())) {
			LOG.warn("ResourceDef collection on serviceDef was null!");
		} else {
			for (RangerResourceDef resourceTypeDef : serviceDef.getResources()) {
				if (resourceTypeDef == null) {
					LOG.warn("resource type def was null!");
				} else {
					String resourceName = resourceTypeDef.getName();
					if (StringUtils.isBlank(resourceName)) {
						LOG.warn("Resource def name was null/empty/blank!");
					} else {
						resourceNames.add(resourceName.toLowerCase());
					}
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerValidator.getAllResourceNames(" + serviceDef + "): " + resourceNames);
		}
		return resourceNames;
	}
	
	/**
	 * Returns the resource-types defined on the policy converted to lowe-case
	 * @param policy
	 * @return
	 */
	Set<String> getPolicyResources(RangerPolicy policy) {
		if (policy == null || policy.getResources() == null || policy.getResources().isEmpty()) {
			return new HashSet<String>();
		} else {
			Set<String> result = new HashSet<String>();
			for (String name : policy.getResources().keySet()) {
				result.add(name.toLowerCase());
			}
			return result;
		}
	}

	Map<String, String> getValidationRegExes(RangerServiceDef serviceDef) {
		if (serviceDef == null || CollectionUtils.isEmpty(serviceDef.getResources())) {
			return new HashMap<String, String>();
		} else {
			Map<String, String> result = new HashMap<String, String>();
			for (RangerResourceDef resourceDef : serviceDef.getResources()) {
				if (resourceDef == null) {
					LOG.warn("A resource def in resource def collection is null");
				} else {
					String name = resourceDef.getName();
					String regEx = resourceDef.getValidationRegEx();
					if (StringUtils.isBlank(name)) {
						LOG.warn("resource name is null/empty/blank");
					} else if (StringUtils.isBlank(regEx)) {
						LOG.debug("validation regex is null/empty/blank");
					} else {
						result.put(name, regEx);
					}
				}
			}
			return result;
		}
	}
	
	int getEnumDefaultIndex(RangerEnumDef enumDef) {
		int index;
		if (enumDef == null) {
			index = -1;
		} else if (enumDef.getDefaultIndex() == null) {
			index = 0;
		} else {
			index = enumDef.getDefaultIndex();
		}
		return index;
	}

	Collection<String> getImpliedGrants(RangerAccessTypeDef def) {
		if (def == null) {
			return null;
		} else if (CollectionUtils.isEmpty(def.getImpliedGrants())) {
			return new ArrayList<String>();
		} else {
			List<String> result = new ArrayList<String>(def.getImpliedGrants().size());
			for (String name : def.getImpliedGrants()) {
				if (StringUtils.isBlank(name)) {
					result.add(name); // could be null!
				} else {
					result.add(name.toLowerCase());
				}
			}
			return result;
		}
	}

	/**
	 * Returns a copy of the policy resource map where all keys (resource-names) are lowercase
	 * @param input
	 * @return
	 */
	Map<String, RangerPolicyResource> getPolicyResourceWithLowerCaseKeys(Map<String, RangerPolicyResource> input) {
		if (input == null) {
			return null;
		}
		Map<String, RangerPolicyResource> output = new HashMap<String, RangerPolicyResource>(input.size());
		for (Map.Entry<String, RangerPolicyResource> entry : input.entrySet()) {
			output.put(entry.getKey().toLowerCase(), entry.getValue());
		}
		return output;
	}

}
