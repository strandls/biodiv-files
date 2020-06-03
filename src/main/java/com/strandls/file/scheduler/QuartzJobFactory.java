package com.strandls.file.scheduler;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class QuartzJobFactory implements JobFactory {
	
	private final Injector inject;
	
	@Inject
	public QuartzJobFactory(Injector inject) {
		this.inject = inject;
	}

	@Override
	public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
		JobDetail jobDetail = bundle.getJobDetail();
		Class jobClass = jobDetail.getJobClass();
		try {
			return (Job) inject.getInstance(jobClass);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new UnsupportedOperationException(ex);
		}
	}	
	
}
