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
	
}