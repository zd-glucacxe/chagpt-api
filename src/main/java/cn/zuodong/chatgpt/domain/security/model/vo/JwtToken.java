package cn.zuodong.chatgpt.domain.security.model.vo;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * 将 JWT 封装成 Shiro 可以理解的身份验证令牌，以便在 Shiro 中进行身份验证。
 * @author zuodong
 * @create 2023-12-05 18:16
 */

//实现了 Shiro 的 AuthenticationToken 接口的类
public class JwtToken implements AuthenticationToken {

    private String jwt;

    /**
     * 带参数的构造函数，用于传递 JWT
     * @param jwt
     */
    public JwtToken(String jwt) {
        this.jwt = jwt;
    }

    /**
     * 等同于账户, 获取主体信息，一般对应用户的身份（例如用户名、用户ID等）。 JWT被用作主体信息。
     */
    @Override
    public Object getPrincipal() {
        return jwt;
    }

    /**
     * 等同于密码，  用于获取凭证信息，一般对应用户的密码或者其他凭证。在这里，同样将 JWT 作为凭证信息。
     */
    @Override
    public Object getCredentials() {
        return jwt;
    }

}
