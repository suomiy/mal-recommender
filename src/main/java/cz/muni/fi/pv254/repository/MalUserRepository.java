package cz.muni.fi.pv254.repository;

/**
 * Created by suomiy on 11/7/15.
 */

import cz.muni.fi.pv254.entity.User;
import cz.muni.fi.pv254.entity.QMalUser;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@ApplicationScoped
@Transactional
public class MalUserRepository extends Repository<User> {
    public MalUserRepository() {
        super();
    }

    @Inject
    public MalUserRepository(EntityManager em) {
        super(em, User.class, QMalUser.malUser);
    }
}
