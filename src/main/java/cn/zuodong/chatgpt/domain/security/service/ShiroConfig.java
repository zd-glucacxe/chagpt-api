package cn.zuodong.chatgpt.domain.security.service;


import cn.zuodong.chatgpt.domain.security.service.realm.JwtRealm;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SubjectFactory;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.web.filter.authc.AnonymousFilter;
import org.apache.shiro.web.filter.authc.LogoutFilter;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.DefaultWebSubjectFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * @author zuodong
 * @create 2023-12-05 18:18
 */

@Configuration
public class ShiroConfig {

    /**
     *  创建一个自定义的 JwtDefaultSubjectFactory
     * @return JwtDefaultSubjectFactory
     */
    @Bean
    public SubjectFactory subjectFactory() {
        class JwtDefaultSubjectFactory extends DefaultWebSubjectFactory {
            /**
             *  subjectFactory 负责创建 Subject对象
             * @param context
             * @return Subject 对象
             */
            @Override
            public Subject createSubject(SubjectContext context) {
                context.setSessionCreationEnabled(false); // 关闭Shiro中的Session创建功能。因为JWT 通常是无状态的。
                return super.createSubject(context);
            }
        }
        return new JwtDefaultSubjectFactory();
    }

    /**
     * Realm 定义了Shiro如何获取用户身份验证和授权信息，这里创建了一个自定义的 JwtRealm
     * @return JwtRealm
     */
    @Bean
    public Realm realm() {
        return new JwtRealm();
    }

    /**
     *  DefaultWebSecurityManager 是Shiro的核心组件，用于配置各种安全组件
     * @return DefaultWebSecurityManager对象，是Apache Shiro框架中用于管理安全性的核心类
     */
    @Bean
    public DefaultWebSecurityManager securityManager() {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        //设置安全管理器的认证（Realm）部分
        securityManager.setRealm(realm());  // realm()返回一个Realm对象，该对象负责执行实际的身份验证和授权

        // 创建一个DefaultSubjectDAO实例，该实例负责管理Subject实例的状态。
        DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();

        // 创建一个DefaultSessionStorageEvaluator实例，该实例用于决定Subject是否应该在会话中保留状态。
        DefaultSessionStorageEvaluator defaultSessionStorageEvaluator = new DefaultSessionStorageEvaluator();

        //  禁用会话存储，不在会话中保留Subject的状态。
        defaultSessionStorageEvaluator.setSessionStorageEnabled(false);

        //将上述创建的defaultSessionStorageEvaluator设置到subjectDAO中
        subjectDAO.setSessionStorageEvaluator(defaultSessionStorageEvaluator);

        //将subjectDAO设置到securityManager中，以便管理Subject的状态。
        securityManager.setSubjectDAO(subjectDAO);

        //设置Subject的工厂。 subjectFactory()方法返回一个SubjectFactory对象
        securityManager.setSubjectFactory(subjectFactory());
        return securityManager;
    }

    /**
     * 配置了Shiro的过滤器链，定义了不同路径使用不同过滤器的规则
     * @return shiroFilter  回配置好的 ShiroFilterFactoryBean 实例
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean() {
        //创建一个 ShiroFilterFactoryBean 实例，该实例用于配置Shiro的过滤器链。
        ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
        shiroFilter.setSecurityManager(securityManager()); //设置安全管理器 securityManager()。
        shiroFilter.setLoginUrl("/unauthenticated"); //设置登录URL为 "/unauthenticated"，表示未认证用户访问需要认证的资源时会被重定向到该URL。
        shiroFilter.setUnauthorizedUrl("/unauthorized"); //设置未授权URL为 "/unauthorized"，表示认证用户但没有访问权限时会被重定向到该URL。
        // 添加jwt过滤器
        Map<String, Filter> filterMap = new HashMap<>();
        // 设置过滤器【anon\logout可以不设置】
        filterMap.put("anon", new AnonymousFilter()); // "anon" 使用 AnonymousFilter，表示匿名访问，无需认证即可访问。
        filterMap.put("jwt", new JwtFilter());  // "jwt" 使用 JwtFilter，表示JWT验证。
        filterMap.put("logout", new LogoutFilter());  // "logout" 使用 LogoutFilter，表示登出。
        shiroFilter.setFilters(filterMap);

        // 拦截器，指定方法走哪个拦截器 【login->anon】【logout->logout】【verify->jwt】
        // 创建一个有序的 LinkedHashMap 用于存储路径和过滤器的映射规则。
        Map<String, String> filterRuleMap = new LinkedHashMap<>();
        filterRuleMap.put("/login", "anon"); // 对于路径 /login，配置为匿名访问
        filterRuleMap.put("/logout", "logout"); // 对于路径 /logout，配置为登出操作  使用 LogoutFilter 过滤器。
        filterRuleMap.put("/verify", "jwt"); // 对于路径 /verify，配置为JWT验证   使用 LogoutFilter 过滤器。
        shiroFilter.setFilterChainDefinitionMap(filterRuleMap);

        return shiroFilter;
    }

}
