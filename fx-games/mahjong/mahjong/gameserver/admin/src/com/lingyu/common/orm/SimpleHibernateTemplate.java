package com.lingyu.common.orm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.transform.ResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.lingyu.common.util.BeanUtils;

/**
 * 
 * @author Allen Jiang
 * 
 * @version 1.0, 2014-5-01
 */
public class SimpleHibernateTemplate<T, PK extends Serializable> {

	protected static final Logger logger = LoggerFactory.getLogger(SimpleHibernateTemplate.class);

	private final Class<T> entityClass;
	protected SessionFactory sessionFactory;

	public SimpleHibernateTemplate(SessionFactory sessionFactory, Class<T> entityClass) {
		// this.entityClass = (Class<T>) ((ParameterizedType)
		// getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		this.entityClass = entityClass;
		this.sessionFactory = sessionFactory;
	}

	// @Autowired
	// @Qualifier("sessionFactory")
	// private SessionFactory sessionFactory;

	public Session getSession() {
		// 事务必须是开启的(Required)，否则获取不到
		return sessionFactory.getCurrentSession();
	}

	@SuppressWarnings("unchecked")
	public PK save(T entity) {
		Assert.notNull(entity);
		return (PK) getSession().save(entity);
	}

	public void saveOrUpdate(T entity) {
		Assert.notNull(entity);
		getSession().saveOrUpdate(entity);
	}

	public void update(T entity) {
		Assert.notNull(entity);
		getSession().update(entity);

	}

	public void delete(PK id) {
		Assert.notNull(id);
		getSession().delete(this.get(id));

	}

	public void delete(T entity) {
		Assert.notNull(entity);
		getSession().delete(entity);

	}

	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<T> findAll() {
		Criteria criteria = getSession().createCriteria(entityClass);
		// criteria.setProjection(Projections.rowCount());
		// criteria.setFirstResult(PageUtil.getPageStart(pn, pageSize));
		return criteria.list();

	}

	public int getSize() {
		Criteria criteria = getSession().createCriteria(entityClass);
		criteria.setProjection(Projections.rowCount());
		return ((Long) criteria.uniqueResult()).intValue();
	}

	/**
	 * 
	 * @param page
	 * @return
	 */
	public Page<T> findAll(Page<T> page) {
		return findByCriteria(page);
	}

	/** 有记录则更新，无记录则插入 */
	public void merge(T entity) {
		getSession().merge(entity);
	}

	public boolean exists(PK id) {
		return get(id) != null;
	}

	@SuppressWarnings("unchecked")
	public T get(PK id) {
		return (T) getSession().get(this.entityClass, id);
	}

	/**
	 * 按HQL查询对象列表.强制结果集类型
	 * 
	 * @param hql
	 * @param values
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<T> findT(String hql, Object... values) {
		Query query = this.createQuery(this.getSession(), hql, values);
		return query.list();

	}

	//
	// public List<T> listAll() {
	// return listAll(-1, -1);
	//
	// }

	/**
	 * 按HQL分页查询. 暂不支持自动获取总结果数,需用户另行执行查询.
	 * 
	 * @param page 分页参数.包括pageSize 和firstResult.
	 * @param hql hql语句.
	 * @param values 数量可变的参数.
	 * 
	 * @return 分页查询结果,附带结果列表及所有查询时的参数.
	 */
	@SuppressWarnings("unchecked")
	public Page<T> find(final Page<T> page, final String hql, final Object... values) {
		Assert.notNull(page);
		if (page.isAutoCount()) {
			int totalCount = countHqlResult(hql, values);
			page.setTotalCount(totalCount);
		}
		Query query = createQuery(getSession(), hql, values);
		if (page.isFirstSetted()) {
			query.setFirstResult(page.getFirst());
		}
		if (page.isPageSizeSetted()) {
			query.setMaxResults(page.getPageSize());
		}
		page.setResult(query.list());
		return page;
	}

	/**
	 * 按Criterion分页查询.
	 * 
	 * @param page 分页参数.包括pageSize、firstResult、orderBy、asc、autoCount.
	 *            其中firstResult可直接指定,也可以指定pageNo. autoCount指定是否动态获取总结果数.
	 * 
	 * @param criterion 数量可变的Criterion.
	 * @return 分页查询结果.附带结果列表及所有查询时的参数.
	 */
	@SuppressWarnings("unchecked")
	public Page<T> findByCriteria(final Page<T> page, final Criterion... criterions) {
		Assert.notNull(page);

		Criteria criteria = getSession().createCriteria(entityClass);
		for (Criterion c : criterions) {
			criteria.add(c);
		}
		if (page.isAutoCount()) {
			page.setTotalCount(countQueryResult(page, criteria));
		}
		if (page.isFirstSetted()) {
			criteria.setFirstResult(page.getFirst());
		}
		if (page.isPageSizeSetted()) {
			criteria.setMaxResults(page.getPageSize());
		}
		if (page.isOrderBySetted()) {
			if (page.getOrder().endsWith(QueryParameter.ASC)) {
				criteria.addOrder(Order.asc(page.getOrderBy()));
			} else {
				criteria.addOrder(Order.desc(page.getOrderBy()));
			}
		}
		page.setResult(criteria.list());
		return page;
	}

	/**
	 * 执行count查询获取本次Hql查询所能获得的对象总数。 本函数只能自动处理简单的hql语句，复杂的hql查询请另行编写count语句查询
	 * 
	 * @param hql
	 * @param values
	 * @return
	 */
	protected int countHqlResult(String hql, final Object... values) {
		String fromHql = hql;
		// select子句与order by子句会影响count查询，进行简单的排除
		fromHql = "from " + StringUtils.substringAfter(fromHql, "from");
		fromHql = StringUtils.substringBefore(fromHql, "order by");
		String countHql = "select count(id) " + fromHql;
		try {
			Integer count = findInt(countHql, values);
			return count;
		} catch (Exception e) {
			throw new RuntimeException("hql can't be auto count, hql is:" + countHql, e);
		}
	}

	/**
	 * 按HQL查询Intger类形结果.
	 */
	public Integer findInt(String hql, Object... values) {
		return Integer.parseInt(findUnique(hql, values).toString());
	}

	/**
	 * 按HQL查询Long类型结果.
	 */
	public Long findLong(String hql, Object... values) {
		return (Long) findUnique(hql, values);
	}

	public void flush() {

		getSession().flush();
	}

	public void clear() {
		getSession().clear();
	}

	/**
	 * 按HQL查询唯一对象.
	 */
	public Object findUnique(final String hql, final Object... values) {
		Query query = createQuery(getSession(), hql, values);
		return query.uniqueResult();

	}

	/**
	 * 执行批处理语句.如 之间insert, update, delete 等.
	 */
	public int bulkUpdate(final String queryString, final Object... values) {
		Query query = this.createQuery(this.getSession(), queryString, values);
		Object result = query.executeUpdate();
		return result == null ? 0 : ((Integer) result).intValue();
	}

	@SuppressWarnings("unchecked")
	public T unique(Criteria criteria) {
		return (T) criteria.uniqueResult();
	}

	// protected void setParameters(Query query, Object[] paramlist) {
	// if (paramlist != null) {
	// for (int i = 0; i < paramlist.length; i++) {
	// if (paramlist[i] instanceof Date) {
	// // TODO 难道这是bug 使用setParameter不行？？
	// query.setTimestamp(i, (Date) paramlist[i]);
	// } else {
	// query.setParameter(i, paramlist[i]);
	// }
	// }
	// }
	// }

	/**
	 * 
	 * @param session
	 * @param queryString
	 * @param values
	 * @return
	 */
	private Query createQuery(Session session, String queryString, Object... values) {
		Assert.hasText(queryString);
		Query queryObject = session.createQuery(queryString);
		try {
			if (values != null) {
				for (int i = 0; i < values.length; i++) {
					{
						queryObject.setParameter(String.valueOf(i), values[i]);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return queryObject;
	}

	/**
	 * 通过count查询获得本次查询所能获得的对象总数.
	 * 
	 * @return page对象中的totalCount属性将赋值.
	 */
	@SuppressWarnings("unchecked")
	protected int countQueryResult(Page<T> page, Criteria c) {
		CriteriaImpl impl = (CriteriaImpl) c;
		// 先把Projection、ResultTransformer、OrderBy取出来,清空三者后再执行Count操作
		Projection projection = impl.getProjection();
		ResultTransformer transformer = impl.getResultTransformer();
		List<CriteriaImpl.OrderEntry> orderEntries = null;
		try {
			orderEntries = (List<CriteriaImpl.OrderEntry>) BeanUtils.getFieldValue(impl, "orderEntries");
			BeanUtils.setFieldValue(impl, "orderEntries", new ArrayList<CriteriaImpl.OrderEntry>());
		} catch (Exception e) {
			logger.error("不可能抛出的异常:{}", e.getMessage());
		}
		// 执行Count查询
		int totalCount = (Integer) c.setProjection(Projections.rowCount()).uniqueResult();
		if (totalCount < 1)
			return -1;
		// 将之前的Projection和OrderBy条件重新设回去
		c.setProjection(projection);
		if (projection == null) {
			c.setResultTransformer(CriteriaSpecification.ROOT_ENTITY);
		}
		if (transformer != null) {
			c.setResultTransformer(transformer);
		}
		try {
			BeanUtils.setFieldValue(impl, "orderEntries", orderEntries);
		} catch (Exception e) {
			logger.error("不可能抛出的异常:{}", e.getMessage());
		}
		return totalCount;
	}

	/**
	 * 按Criterion查询对象列表.
	 * 
	 * @param criterion 数量可变的Criterion.
	 */
	@SuppressWarnings("unchecked")
	public List<T> findByCriteria(final Criterion... criterions) {
		Criteria criteria = this.getSession().createCriteria(entityClass);
		for (Criterion c : criterions) {
			criteria.add(c);
		}
		return criteria.list();
	}

	/**
	 * 按属性查找对象列表.
	 */
	public List<T> findByProperty(String propertyName, Object value) {
		Assert.hasText(propertyName);
		return findByCriteria(Restrictions.eq(propertyName, value));
	}

	/**
	 * 按属性查找唯一对象.
	 */
	@SuppressWarnings("unchecked")
	public T findUniqueByProperty(final String propertyName, final Object value) {
		Assert.hasText(propertyName);
		Criteria criteria = this.getSession().createCriteria(entityClass);
		criteria.add(Restrictions.eq(propertyName, value));
		return (T) criteria.uniqueResult();

	}
}
