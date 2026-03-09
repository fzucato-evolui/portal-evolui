package br.com.evolui.portalevolui.web.repository.user;

import br.com.evolui.portalevolui.web.beans.UserBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserBean, Long>, UserCustomRepository {
    Optional<UserBean> findByLogin(String login);
    Optional<UserBean> findByLoginOrEmail(String login, String email);
}
