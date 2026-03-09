package br.com.evolui.portalevolui.web.service;

import br.com.evolui.portalevolui.web.beans.UserBean;
import br.com.evolui.portalevolui.web.repository.user.UserRepository;
import br.com.evolui.portalevolui.web.security.UserDetailsSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAuthenticationService implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetailsSecurity loadUserByUsername(String login) throws UsernameNotFoundException {
        UserBean user = userRepository.findByLoginOrEmail(login, login)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with login: " + login));
        if (!user.getEnabled()) {
            throw new UsernameNotFoundException("Usuário não está ativo");
        }

        return UserDetailsSecurity.build(user);
    }
}
