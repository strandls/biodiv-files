package com.strandls.file.scheduler;

import java.util.Date;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class QuartzJob implements Job {

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// Do something
		System.out.println("\n\n***** Date: " + new Date() + " *****\n\n");
	}

}
