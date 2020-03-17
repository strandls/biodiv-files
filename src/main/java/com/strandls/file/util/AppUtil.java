package com.strandls.file.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.ws.rs.core.CacheControl;

public class AppUtil {

	public static CacheControl getCacheControl() {
		CacheControl cache = new CacheControl();
		cache.setMaxAge(365 * 24 * 60 * 60);
		return cache;
	}

	public static String getDatePrefix() {
		StringBuilder prefix = new StringBuilder();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM");
		return prefix.append(sdf.format(new Date())).append("_").append(new GregorianCalendar().get(Calendar.YEAR))
				.append("_").toString();
	}

}
