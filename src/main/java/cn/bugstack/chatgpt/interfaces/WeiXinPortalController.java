package cn.bugstack.chatgpt.interfaces;

import cn.bugstack.chatglm.model.ChatCompletionRequest;
import cn.bugstack.chatglm.model.Model;
import cn.bugstack.chatglm.model.Role;
import cn.bugstack.chatglm.session.Configuration;
import cn.bugstack.chatglm.session.OpenAiSession;
import cn.bugstack.chatglm.session.OpenAiSessionFactory;
import cn.bugstack.chatglm.session.defaults.DefaultOpenAiSessionFactory;
import cn.bugstack.chatgpt.application.IWeiXinValidateService;

import cn.bugstack.chatgpt.domain.receive.model.MessageTextEntity;
import cn.bugstack.chatgpt.infrastructure.util.XmlUtil;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * @author 小傅哥，微信：fustack
 * @description 微信公众号，请求处理服务
 * @github https://github.com/fuzhengwei
 * @Copyright 公众号：bugstack虫洞栈 | 博客：https://bugstack.cn - 沉淀、分享、成长，让自己和他人都能有所收获！
 */
@RestController
@RequestMapping("/wx/portal/{appid}")
public class WeiXinPortalController {

    private Logger logger = LoggerFactory.getLogger(WeiXinPortalController.class);

    @Value("${wx.config.originalid:gh_95b2229b90fb}")
    private String originalId;

    @Resource
    private IWeiXinValidateService weiXinValidateService;

    private OpenAiSession openAiSession;

    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    private Map<String, String> chatGPTMap = new ConcurrentHashMap<>();

    public WeiXinPortalController() {
        // 1. 配置文件；智谱Ai申请你的 ApiSecretKey 教程；https://bugstack.cn/md/project/chatgpt/sdk/chatglm-sdk-java.html
        Configuration configuration = new Configuration();
        configuration.setApiHost("https://open.bigmodel.cn/");
        configuration.setApiSecretKey("d570f7c5d289cdac2abdfdc562e39f3f.trqz1dH8ZK6ED7Pg");
        // 2. 会话工厂
        OpenAiSessionFactory factory = new DefaultOpenAiSessionFactory(configuration);
        // 3. 开启会话
        this.openAiSession = factory.openSession();
        logger.info("开始 openAiSession");
    }

    /**
     * 处理微信服务器发来的get请求，进行签名的验证
     * http://xfg-studio.natapp1.cc/wx/portal/wx4bd388e42758df34
     * <p>
     * appid     微信端AppID
     * signature 微信端发来的签名
     * timestamp 微信端发来的时间戳
     * nonce     微信端发来的随机字符串
     * echostr   微信端发来的验证字符串
     */
    @GetMapping(produces = "text/plain;charset=utf-8")
    public String validate(@PathVariable String appid,
                           @RequestParam(value = "signature", required = false) String signature,
                           @RequestParam(value = "timestamp", required = false) String timestamp,
                           @RequestParam(value = "nonce", required = false) String nonce,
                           @RequestParam(value = "echostr", required = false) String echostr) {
        try {
            logger.info("微信公众号验签信息{}开始 [{}, {}, {}, {}]", appid, signature, timestamp, nonce, echostr);
            if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {
                throw new IllegalArgumentException("请求参数非法，请核实!");
            }
            boolean check = weiXinValidateService.checkSign(signature, timestamp, nonce);
            logger.info("微信公众号验签信息{}完成 check：{}", appid, check);
            if (!check) {
                return null;
            }
            return echostr;
        } catch (Exception e) {
            logger.error("微信公众号验签信息{}失败 [{}, {}, {}, {}]", appid, signature, timestamp, nonce, echostr, e);
            return null;
        }
    }

    /**
     * 此处是处理微信服务器的消息转发的
     */
    @PostMapping(produces = "application/xml; charset=UTF-8")
    public String post(@PathVariable String appid,
                       @RequestBody String requestBody,
                       @RequestParam("signature") String signature,
                       @RequestParam("timestamp") String timestamp,
                       @RequestParam("nonce") String nonce,
                       @RequestParam("openid") String openid,
                       @RequestParam(name = "encrypt_type", required = false) String encType,
                       @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        try {
            logger.info("接收微信公众号信息请求{}开始 {}", openid, requestBody);
            MessageTextEntity message = XmlUtil.xmlToBean(requestBody, MessageTextEntity.class);
            // 异步任务
            if (chatGPTMap.get(message.getContent().trim()) == null || "NULL".equals(chatGPTMap.get(message.getContent().trim()))) {
                // 反馈信息[文本]
                MessageTextEntity res = new MessageTextEntity();
                res.setToUserName(openid);
                res.setFromUserName(originalId);
                res.setCreateTime(String.valueOf(System.currentTimeMillis() / 1000L));
                res.setMsgType("text");
                res.setContent("消息处理中，请再回复我一句【" + message.getContent().trim() + "】");
                if (chatGPTMap.get(message.getContent().trim()) == null) {
                    doChatGPTTask(message.getContent().trim());
                }

                return XmlUtil.beanToXml(res);
            }

            // 反馈信息[文本]
            MessageTextEntity res = new MessageTextEntity();
            res.setToUserName(openid);
            res.setFromUserName(originalId);
            res.setCreateTime(String.valueOf(System.currentTimeMillis() / 1000L));
            res.setMsgType("text");
            res.setContent(chatGPTMap.get(message.getContent().trim()));
            String result = XmlUtil.beanToXml(res);
            logger.info("接收微信公众号信息请求{}完成 {}", openid, result);
            chatGPTMap.remove(message.getContent().trim());
            return result;
        } catch (Exception e) {
            logger.error("接收微信公众号信息请求{}失败 {}", openid, requestBody, e);
            return "";
        }
    }

    public void doChatGPTTask(String content) {
        chatGPTMap.put(content, "NULL");
        taskExecutor.execute(() -> {
            // 入参；模型、请求信息；记得更新最新版 ChatGLM-SDK-Java
            ChatCompletionRequest request = new ChatCompletionRequest();
            request.setModel(Model.CHATGLM_TURBO); // chatGLM_6b_SSE、chatglm_lite、chatglm_lite_32k、chatglm_std、chatglm_pro
            request.setPrompt(new ArrayList<ChatCompletionRequest.Prompt>() {
                private static final long serialVersionUID = -7988151926241837899L;

                {
                    add(ChatCompletionRequest.Prompt.builder()
                            .role(Role.user.getCode())
                            .content(content)
                            .build());
                }
            });
            // 同步获取结果
            try {
                CompletableFuture<String> future = openAiSession.completions(request);
                chatGPTMap.put(content, future.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

        });
    }

}
