package com.strandls.file;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.servlet.ServletContextEvent;

import org.glassfish.jersey.servlet.ServletContainer;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.rabbitmq.client.Channel;
import com.strandls.file.api.APIModule;
import com.strandls.file.dao.DaoModule;
import com.strandls.file.scheduler.QuartzJob;
import com.strandls.file.scheduler.QuartzJobFactory;
import com.strandls.file.scheduler.QuartzScheduler;
import com.strandls.file.service.ServiceModule;

public class FileServeletContextListener extends GuiceServletContextListener {

private static final Logger logger = LoggerFactory.getLogger(FileServeletContextListener.class);
	private Scheduler scheduler;

	@Override
	protected Injector getInjector() {

		Injector injector = Guice.createInjector(new ServletModule() {
			@Override
			protected void configureServlets() {

				Configuration configuration = new Configuration();

				try {
					for (Class<?> cls : getEntityClassesFromPackage("com")) {
						configuration.addAnnotatedClass(cls);
					}
				} catch (ClassNotFoundException | IOException | URISyntaxException e) {
				 logger.error(e.getMessage());
				}

				configuration = configuration.configure();
				SessionFactory sessionFactory = configuration.buildSessionFactory();

				Map<String, String> props = new HashMap<String, String>();
				props.put("javax.ws.rs.Application", ApplicationConfig.class.getName());
				props.put("jersey.config.server.provider.packages", "com");
				props.put("jersey.config.server.wadl.disableWadl", "true");

				bind(SessionFactory.class).toInstance(sessionFactory);
				bind(QuartzJob.class).in(Scopes.SINGLETON);

				RabbitMqConnection connection = new RabbitMqConnection();
				Channel channel = null;
				try {
					channel = connection.setRabbitMQConnetion();
					bind(Channel.class).toInstance(channel);
				} catch (Exception ex) {
					logger.error(ex.getMessage());
				}
				bind(ServletContainer.class).in(Scopes.SINGLETON);
				serve("/api/*").with(ServletContainer.class, props);
			}
		}, new APIModule(), new DaoModule(), new ServiceModule());
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.setJobFactory(injector.getInstance(QuartzJobFactory.class));
			scheduler.start();
			QuartzScheduler quScheduler = new QuartzScheduler();
			quScheduler.scheduleJob(scheduler);

		} catch (Exception ex) {
			logger.error(ex.getMessage());
		}

		return injector;

	}

	protected List<Class<?>> getEntityClassesFromPackage(String packageName)
			throws URISyntaxException, IOException, ClassNotFoundException {

		List<String> classNames = getClassNamesFromPackage(packageName);
		List<Class<?>> classes = new ArrayList<Class<?>>();
		for (String className : classNames) {
			Class<?> cls = Class.forName(className);
			Annotation[] annotations = cls.getAnnotations();

			for (Annotation annotation : annotations) {
				if (annotation instanceof javax.persistence.Entity) {
					System.out.println("Mapping entity :" + cls.getCanonicalName());
					classes.add(cls);
				}
			}
		}

		return classes;
	}

	private static ArrayList<String> getClassNamesFromPackage(final String packageName)
			throws URISyntaxException, IOException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		ArrayList<String> names = new ArrayList<String>();
		URL packageURL = classLoader.getResource(packageName);

		URI uri = new URI(packageURL.toString());
		File folder = new File(uri.getPath());

		try (Stream<Path> files = Files.find(Paths.get(folder.getAbsolutePath()), 999,
				(p, bfa) -> bfa.isRegularFile())) {
			files.forEach(file -> {
				String name = file.toFile().getAbsolutePath()
						.replaceAll(folder.getAbsolutePath() + File.separatorChar, "").replace(File.separatorChar, '.');
				if (name.indexOf('.') != -1) {
					name = packageName + '.' + name.substring(0, name.lastIndexOf('.'));
					names.add(name);
				}
			});
		}
		return names;

	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		super.contextInitialized(servletContextEvent);
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		Injector injector = (Injector) servletContextEvent.getServletContext().getAttribute(Injector.class.getName());
		Channel channel = injector.getInstance(Channel.class);
		try {
			if (scheduler != null && !scheduler.isShutdown()) {
				scheduler.shutdown(true);

				System.out.println("\n\n***** Shutdown? " + scheduler.isShutdown() + " *****\n\n");
			}
			channel.getConnection().close();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		super.contextDestroyed(servletContextEvent);
	}
}