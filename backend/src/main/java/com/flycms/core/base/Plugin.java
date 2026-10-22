
package com.flycms.core.base;

import jakarta.annotation.PostConstruct;
/*
 *  Open source house, All rights reserved
 *  开发公司：mxxsaas.com<br/>
 *  版权：开源中国<br/>
 *	http://www.mxxsaas.com
 *
 *
 */
public interface Plugin {

	@PostConstruct
	public void init() throws Exception;
}
