package com.aston.userservice.dao;

import com.aston.userservice.entity.User;
import com.aston.userservice.exception.DataAccessException;
import com.aston.userservice.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class UserDaoImpl implements UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);
    private final SessionFactory sessionFactory;

    public UserDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public UserDaoImpl() {
        this(HibernateUtil.getSessionFactory());
    }

    @Override
    public User create(User user) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(user);
            transaction.commit();
            return user;
        } catch (Exception e) {
            logger.error("Не удалось создать пользователя ", e);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new DataAccessException("Не удалось создать пользователя", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            User user = session.find(User.class, id);
            transaction.commit();
            return Optional.ofNullable(user);

        } catch (Exception e) {
            logger.error("Ошибка в поиске пользователя ", e);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new DataAccessException("Не удалось получить пользователя", e);
        }
    }

    @Override
    public List<User> findAll() {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            List<User> users = session.createQuery("from User", User.class).list();
            transaction.commit();
            return users;
        } catch (Exception e) {
            logger.error("Ошибка при получении списка пользователей ", e);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new DataAccessException("Не удалось получить список пользователей", e);
        }
    }

    @Override
    public User update(User user) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            User updatedUser = session.merge(user);
            transaction.commit();
            return updatedUser;
        } catch (Exception e) {
            logger.error("Ошибка при обновлении пользователя", e);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new DataAccessException("Не удалось обновить пользователя", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            User deletedUser = session.find(User.class, id);
            if (deletedUser != null) {
                session.remove(deletedUser);
            }
            transaction.commit();
        } catch (Exception e) {
            logger.error("Ошибка при удалении пользователя ", e);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new DataAccessException("Не удалось удалить пользователя", e);
        }
    }
}
