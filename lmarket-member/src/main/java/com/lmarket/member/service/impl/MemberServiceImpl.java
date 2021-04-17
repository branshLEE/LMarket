package com.lmarket.member.service.impl;

import com.lmarket.member.dao.MemberLevelDao;
import com.lmarket.member.entity.MemberLevelEntity;
import com.lmarket.member.exception.PhoneExistException;
import com.lmarket.member.exception.UsernameExistException;
import com.lmarket.member.service.MemberLevelService;
import com.lmarket.member.vo.MemberLoginVo;
import com.lmarket.member.vo.MemberRegistVo;
import com.lmarket.member.vo.Oauth2UserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.PageUtils;
import com.common.utils.Query;

import com.lmarket.member.dao.MemberDao;
import com.lmarket.member.entity.MemberEntity;
import com.lmarket.member.service.MemberService;
import org.springframework.web.bind.annotation.RequestParam;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) {
        MemberDao baseMapper = this.baseMapper;
        MemberEntity entity = new MemberEntity();

        //设置默认会员等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        entity.setLevelId(levelEntity.getId());

        //检查用户名喝手机号是否唯一
        checkPhoneUnique(vo.getPhone());
        checkUsernameUnique(vo.getUserName());

        entity.setMobile(vo.getPhone());
        entity.setUsername(vo.getUserName());

        //昵称设置为用户名
        entity.setNickname(vo.getUserName());

        //密码要进行加密存储
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        entity.setPassword(encode);

        //其他的默认信息

        //保存
        baseMapper.insert(entity);

    }

    @Override
    public void checkPhoneUnique(String phone)  throws PhoneExistException{
        MemberDao memberDao = this.baseMapper;
        Integer mobile = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(mobile>0){
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUsernameUnique(String username) throws UsernameExistException{
        MemberDao memberDao = this.baseMapper;
        Integer name = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if(name>0){
            throw new UsernameExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {

        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();

        //1、去数据库查询 select * from `ums_member` where username = ? or mobile = ?
        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct).or().eq("mobile", loginacct));

        if(entity == null){
            //登录失败
            return null;
        }else{
            //1、获取到数据库的password
            String entityPassword = entity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            //2、进行密码匹配
            boolean matches = passwordEncoder.matches(password, entityPassword);
            if(matches){
                return entity;
            }else{
                return null;
            }
        }
    }

    @Override
    public MemberEntity login(Oauth2UserVo vo) {

        //登录和注册合并逻辑
        String user_id = vo.getUser_id();
        //1、判断当前社交用户是否已经登录过系统
        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("oauth2_userId", user_id));
        if(entity != null){
            //用户已经注册
            MemberEntity update = new MemberEntity();
            update.setId(entity.getId());
            update.setAccessToken(vo.getAccess_token());

            memberDao.updateById(update);

            entity.setAccessToken(vo.getAccess_token());
            return entity;
        }else {
            //2、没有查到当前社交用户对应的记录，则注册
            MemberEntity register = new MemberEntity();
            register.setOauth2Userid(vo.getUser_id());
            register.setAccessToken(vo.getAccess_token());
            register.setNickname(vo.getNick_name());

            memberDao.insert(register);

            return register;

        }
    }

}