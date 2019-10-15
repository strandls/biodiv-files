package com.strandls.file;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.strandls.file.api.APIModule;
import com.strandls.file.dao.DaoModule;
import com.strandls.file.service.ServiceModule;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class FileServeletContextListener extends GuiceServletContextListener {


	@Override
	protected Injector getInjector() {

		Injector injector = Guice.createInjector(new JerseyServletModule() {
			@Override
			protected void configureServlets() {

				Configuration configuration = new Configuration();

				try {
					for (Class<?> cls : getEntityClassesFromPackage("com")) {
						configuration.addAnnotatedClass(cls);
					}
				} catch (ClassNotFoundException | IOException | URISyntaxException e) {
					e.printStackTrace();
				}

				configuration = configuration.configure();
				SessionFactory sessionFactory = configuration.buildSessionFactory();
				
				Map<String, String> props = new HashMap<String, String>();
				props.put("javax.ws.rs.Application", ApplicationConfig.class.getName());
				props.put("jersey.config.server.wadl.disableWadl", "true");

				bind(SessionFactory.class).toInstance(sessionFactory);
				serve("/api/*").with(GuiceContainer.class,props);
			}
		}, new APIModule(), new DaoModule(), new ServiceModule());

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

		Files.find(Paths.get(folder.getAbsolutePath()), 999, (p, bfa) -> bfa.isRegularFile()).forEach(file -> {
			String name = file.toFile().getAbsolutePath().replaceAll(folder.getAbsolutePath() + File.separatorChar, "")
					.replace(File.separatorChar, '.');
			if (name.indexOf('.') != -1) {
				name = packageName + '.' + name.substring(0, name.lastIndexOf('.'));
				names.add(name);
			}
		});

		return names;
	}
}