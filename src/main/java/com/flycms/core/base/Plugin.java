
package com.flycms.core.base;

import javax.annotation.PostConstruct;
/*
 *  Open source house, All rights reserved
 *  开发公司：28844.com<br/>
 *  版权：开源中国<br/>
 *	http://www.28844.com
 *
 *
 */
public interface Plugin {

	@PostConstruct
	public void init() throws Exception;
}
