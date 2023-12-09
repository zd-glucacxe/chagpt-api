package cn.zuodong.chatgpt.domain.security.service.realm;

import cn.zuodong.chatgpt.domain.security.model.vo.JwtToken;
import cn.zuodong.chatgpt.domain.security.service.JwtUtil;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 负责处理JWT的身份验证逻辑，验证通过后可以获取到用户的身份信息（在这里是用户名），并将JWT本身作为凭证信息返回。
 *     而授权信息暂时未实现。授权信息通常需要根据实际需求从其他地方获取，例如数据库或其他存储。
 * @author zuodong
 * @create 2023-12-05 18:19
 */

// 实现了 Shiro 的 AuthorizingRealm 抽象类的类
public class JwtRealm extends AuthorizingRealm {

    private Logger logger = LoggerFactory.getLogger(JwtRealm.class);

    //创建了一个 JwtUtil 的静态实例，用于处理 JWT 相关的工具方法。
    private static JwtUtil jwtUtil = new JwtUtil();

    /**
     * 判断是否支持指定类型的 AuthenticationToken。
     * 在这里，判断传入的 token 是否是 JwtToken 类型。
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    /**
     * 用于获取授权信息
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        // 暂时不需要实现   因为JWT通常包含了足够的身份信息，而授权信息可能需要从其他地方获取。
        return null;
    }

    /**
     * 进行身份验证
     * @param token  JWT
     * @return  SimpleAuthenticationInfo 包含了JWT本身作为凭证。
     * @throws AuthenticationException
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String jwt = (String) token.getPrincipal();
        if (jwt == null) {
            throw new NullPointerException("jwtToken 不允许为空");
        }
        // 判断  使用 JwtUtil 中的 isVerify 方法验证JWT的有效性。
        if (!jwtUtil.isVerify(jwt)) {
            throw new UnknownAccountException();
        }
        // 验证通过   从JWT中解析出用户名信息，记录日志
        String username = (String) jwtUtil.decode(jwt).get("username");
        logger.info("鉴权用户 username：{}", username);
        return new SimpleAuthenticationInfo(jwt, jwt, "JwtRealm");
    }

}
