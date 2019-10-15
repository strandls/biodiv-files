package com.strandls.file.dao;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import javax.persistence.NoResultException;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public abstract class AbstractDao<T, K extends Serializable> {


	protected SessionFactory sessionFactory;
	
	protected Class<? extends T> daoType;

	protected AbstractDao(SessionFactory sessionFactory) {
		daoType = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		this.sessionFactory = sessionFactory;
	}

	public T save(T entity) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.save(entity);
			tx.commit();			
		} catch (Exception e) {
			if(tx!=null)
				tx.rollback();
			throw e;
		} finally {
			session.close();
		}
		return entity;
	}

	public T update(T entity) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.update(entity);
			tx.commit();			
		} catch (Exception e) {
			if(tx!=null)
				tx.rollback();
			throw e;
		} finally {
			session.close();
		}
		return entity;
	}

	public T delete(T entity) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.delete(entity);
			tx.commit();			
		} catch (Exception e) {
			if(tx!=null)
				tx.rollback();
			throw e;
		} finally {
			session.close();
		}
		return entity;
	}

	public abstract T findById(K id);

	@SuppressWarnings("unchecked")
	public List<T> findAll() {
		Session session = sessionFactory.openSession();
		Criteria criteria = session.createCriteria(daoType);
		List<T> entities = criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
		return entities;
	}
	
	//TODO:improve this to do dynamic finder on any property
	public T findByPropertyWithCondition(String property, String value, String condition) {
		String queryStr = "" +
			    "from "+daoType.getSimpleName()+" t " +
			    "where t."+property+" "+condition+" :value" ;
		Session session = sessionFactory.openSession();
		org.hibernate.query.Query query = session.createQuery(queryStr);
		query.setParameter("value", value);
		
		T entity = null;
		try {
			entity = (T) query.getSingleResult();
		} catch(NoResultException e) {
			throw e;
		}
		session.close();
		return entity;

	}
}