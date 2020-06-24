package com.strandls.file.scheduler;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

public class QuartzScheduler {

	public void scheduleJob(Scheduler schedule) throws SchedulerException {
		JobDetail jobDetail = JobBuilder.newJob(QuartzJob.class).build();
		Trigger jobTrigger = TriggerBuilder.newTrigger().withSchedule(
				CronScheduleBuilder.cronSchedule("0 0/10 1/1 1/1 * ?").withMisfireHandlingInstructionIgnoreMisfires())
				.build();
		schedule.scheduleJob(jobDetail, jobTrigger);
	}

}
