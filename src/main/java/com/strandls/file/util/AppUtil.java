package com.strandls.file.util;

import javax.ws.rs.core.CacheControl;

public class AppUtil {
	
	public static CacheControl getCacheControl() {
		CacheControl cache = new CacheControl();
		cache.setMaxAge(30 *24 * 60 * 60);
		return cache;
	}

}
