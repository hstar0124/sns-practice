package com.example.sns.service;

import com.example.sns.exception.ErrorCode;
import com.example.sns.exception.SnsApplicationException;
import com.example.sns.model.Alarm;
import com.example.sns.model.User;
import com.example.sns.model.entity.UserEntity;
import com.example.sns.repository.AlarmEntityRepository;
import com.example.sns.repository.UserCacheRepository;
import com.example.sns.repository.UserEntityRepository;
import com.example.sns.util.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserEntityRepository userEntityRepository;
    private final AlarmEntityRepository alarmEntityRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserCacheRepository userCacheRepository;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.token.expired-time-ms}")
    private Long expiredTimeMs;

    public User loadUserByUsername(String userName) {
        return userCacheRepository.getUser(userName).orElseGet(() ->
            userEntityRepository.findByUserName(userName).map(User::fromEntity).orElseThrow(() ->
                new SnsApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", userName)))
        );

    }

    @Transactional
    public User join(String userName, String password) {

        // 회원가입하려는 userName으로 회원가입된 user가 있는지
        userEntityRepository.findByUserName(userName).ifPresent(it -> {
            throw new SnsApplicationException(ErrorCode.DUPLICATED_USER_NAME, String.format("%s is duplicated", userName));
        });

        // 회원가입 진행 = user 등록
        UserEntity userEntity = userEntityRepository.save(UserEntity.of(userName, passwordEncoder.encode(password)));

        return User.fromEntity(userEntity);
    }


    public String login(String userName, String password) {

        // 회원가입 여부 체크
        //UserEntity userEntity = userEntityRepository.findByUserName(userName).orElseThrow(() -> new SnsApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", userName)));
        User user = loadUserByUsername(userName);

        userCacheRepository.setUser(user);

        // 비밀번호 체크
        //if(!userEntity.getPassword().equals(password)){
        if(!passwordEncoder.matches(password, user.getPassword())) {
            throw new SnsApplicationException(ErrorCode.INVALID_PASSWORD);
        }

        // 토큰 생성
        String token = JwtTokenUtils.generateToken(userName, secretKey, expiredTimeMs);

        return token;
    }

    public Page<Alarm> alarmList(Integer userId, Pageable pageable) {

        //UserEntity userEntity = userEntityRepository.findByUserName(userName).orElseThrow(() -> new SnsApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", userName)));

        return alarmEntityRepository.findAllByUserId(userId, pageable).map(Alarm::fromEntity);
    }
}