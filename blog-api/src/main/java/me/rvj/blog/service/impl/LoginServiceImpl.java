package me.rvj.blog.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import me.rvj.blog.entity.SysUser;
import me.rvj.blog.mapper.SysUserMapper;
import me.rvj.blog.service.LoginService;
import me.rvj.blog.util.JWTUtils;
import me.rvj.blog.vo.ErrorCode;
import me.rvj.blog.vo.Result;
import me.rvj.blog.vo.params.LoginParams;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @program: rv-blog
 * @description: LoginServiceImpl
 * @author: Rv_Jiang
 * @date: 2022/5/26 12:23
 */
@Service
public class LoginServiceImpl implements LoginService {

    private static final String DEFAULT_SALT = "@Rv_Jiang#";

    @Autowired
    SysUserMapper userMapper;

    @Override
    public Result login(LoginParams loginParams) {
//        参数初始化
        String account = loginParams.getAccount();
        String password = loginParams.getPassword();

//        边界判断
        /* 账号密码不为空 */
        if (StringUtils.isBlank(account) || StringUtils.isBlank(password)) {
            return Result.fail(ErrorCode.PARAMS_ERROR);
        }

//        账号密码验证
        LambdaQueryWrapper<SysUser> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SysUser::getAccount, account);
        SysUser user = userMapper.selectOne(lqw);
        /* 账号或密码错误 */
        if (user == null || !user.getPassword().equals(DigestUtils.sha512Hex(
                password + (user.getSalt() != null ? user.getSalt() : DEFAULT_SALT))
        )) {
            return Result.fail(ErrorCode.ACCOUNT_PWD_ERROR);
        }

//        token生成
        String token = JWTUtils.createToken(user.getId());

        return Result.success(token);
    }

    @Override
    public Result register(LoginParams loginParams) {
//         参数初始化
        String account = loginParams.getAccount();
        String password = loginParams.getPassword();
        String nickname = loginParams.getNickname();
//        System.out.println(account+"\t"+password+"\t"+nickname);

//        边界处理
        if (StringUtils.isBlank(account)
                || StringUtils.isBlank(password)
                || StringUtils.isBlank(nickname)
        ) {
            return Result.fail(ErrorCode.PARAMS_ERROR);
        }

//        账号存在验证
        LambdaQueryWrapper<SysUser> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SysUser::getAccount, account);
        SysUser user = userMapper.selectOne(lqw);
        if (user != null) {
            return Result.fail(ErrorCode.ACCOUNT_EXIST);
        }

//        新建用户
        user = new SysUser();
        user.setNickname(nickname);
        user.setAccount(account);
        user.setPassword(DigestUtils.sha512Hex(password + DEFAULT_SALT));
        user.setCreateDate(System.currentTimeMillis());
        user.setLastLogin(System.currentTimeMillis());
        user.setAvatar("/static/img/avatar/user.png");
        user.setAdmin(0); //1 为true
        user.setDeleted(0); // 0 为false
        user.setSalt(DEFAULT_SALT);
        userMapper.insert(user);

//        生成token
        String token = JWTUtils.createToken(user.getId());

        return Result.success(token);
    }

    @Override
    public Long checkToken(String token) {
        /* token为空 */
        if (StringUtils.isBlank(token)) {
            return null;
        }

        /* token验证 */
        Map<String, Object> stringObjectMap = JWTUtils.checkToken(token);
        if (stringObjectMap == null) {
            return null;
        }

        /* 获取userId */
        Long userId = (Long) stringObjectMap.get(JWTUtils.MAP_Key);

        return userId;

    }
}
